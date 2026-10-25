package com.carpinchoia.voice;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.entity.CarpinchoEntity;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Integración con Simple Voice Chat:
 *  - ESCUCHA: audio de dueños cercanos al carpincho -> servidor de IA.
 *  - HABLA:   canal de audio 3D anclado a la entidad (se oye desde donde está el carpincho).
 *  - Registra la categoría de volumen "Carpincho" (slider propio en los ajustes de voice chat).
 */
@ForgeVoicechatPlugin
public class CarpinchoVoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi api;
    /** jugador -> carpincho que lo está escuchando (lo actualiza el tick del carpincho). */
    private static final Map<UUID, UUID> LISTENING = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, EntityAudioChannel> CHANNELS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService POOL = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "carpincho-voice"); t.setDaemon(true); return t; });

    static {
        POOL.scheduleWithFixedDelay(CarpinchoVoicePlugin::flushIdle, 300, 200, TimeUnit.MILLISECONDS);
    }

    private static class Session {
        final UUID carpincho; final OpusDecoder dec; final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        volatile long last = System.currentTimeMillis();
        Session(UUID c, OpusDecoder d) { carpincho = c; dec = d; }
    }

    public static void listen(UUID player, UUID carpincho) { LISTENING.put(player, carpincho); }

    @Override public String getPluginId() { return CarpinchoIA.ID; }
    @Override public void initialize(VoicechatApi a) {}

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(VoicechatServerStartedEvent.class, e -> {
            api = e.getVoicechat();
            api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId("carpincho").setName("Carpincho")
                .setDescription("Volumen de la voz del Carpincho").build());
        });
        r.registerEvent(MicrophonePacketEvent.class, CarpinchoVoicePlugin::onMic);
    }

    // ---------- Escuchar ----------
    private static void onMic(MicrophonePacketEvent e) {
        VoicechatConnection con = e.getSenderConnection();
        if (con == null || api == null) return;
        Object o = con.getPlayer().getPlayer();
        if (!(o instanceof ServerPlayer sp)) return;
        UUID carp = LISTENING.get(sp.getUUID());
        if (carp == null) return;
        byte[] opus = e.getPacket().getOpusEncodedData();
        Session s = SESSIONS.computeIfAbsent(sp.getUUID(), k -> new Session(carp, api.createDecoder()));
        if (opus == null || opus.length == 0) { finish(sp.getUUID()); return; } // dejó de hablar
        s.last = System.currentTimeMillis();
        short[] pcm = s.dec.decode(opus);
        synchronized (s.pcm) {
            for (short v : pcm) { s.pcm.write(v & 0xFF); s.pcm.write((v >> 8) & 0xFF); }
        }
    }

    private static void flushIdle() {
        long now = System.currentTimeMillis();
        SESSIONS.forEach((id, s) -> { if (now - s.last > 700) finish(id); });
        // olvida jugadores que se alejaron (el tick del carpincho los refresca cada 0.5s)
    }

    private static void finish(UUID player) {
        Session s = SESSIONS.remove(player);
        if (s == null) return;
        byte[] raw;
        synchronized (s.pcm) { raw = s.pcm.toByteArray(); }
        s.dec.close();
        LISTENING.remove(player); // se vuelve a registrar en el próximo tick del carpincho
        if (raw.length < 48000 * 2 / 2) return; // menos de 0.5 s: ignorar
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer sp = server.getPlayerList().getPlayer(player);
        Entity ent = find(server, s.carpincho);
        if (sp == null || ent == null) return;
        String pName = sp.getName().getString(), cName = ent.getName().getString();
        AiClient.ask(wav(raw), pName, cName).thenAccept(rep -> {
            if (rep.text().isBlank() && rep.pcm() == null) return;
            server.execute(() -> {
                Entity c = find(server, s.carpincho);
                if (c == null) return;
                if (!rep.text().isBlank())
                    sp.sendSystemMessage(Component.literal("§6<" + cName + "> §f" + rep.text()));
                if (rep.pcm() != null) speak(c, rep.pcm());
            });
        }).exceptionally(ex -> { System.err.println("[CarpinchoIA] Error con el servidor de IA: " + ex); return null; });
    }

    private static Entity find(MinecraftServer s, UUID id) {
        for (ServerLevel l : s.getAllLevels()) { Entity e = l.getEntity(id); if (e instanceof CarpinchoEntity) return e; }
        return null;
    }

    // ---------- Hablar ----------
    private static void speak(Entity carp, short[] pcm) {
        if (api == null) return;
        EntityAudioChannel ch = CHANNELS.computeIfAbsent(carp.getUUID(), id -> {
            EntityAudioChannel c = api.createEntityAudioChannel(id, api.fromEntity(carp));
            if (c != null) { c.setCategory("carpincho"); c.setDistance(24F); }
            return c;
        });
        if (ch == null) return;
        OpusEncoder enc = api.createEncoder();
        int frames = pcm.length / 960;
        for (int i = 0; i < frames; i++) {
            short[] f = new short[960];
            System.arraycopy(pcm, i * 960, f, 0, 960);
            byte[] opus = enc.encode(f);
            POOL.schedule(() -> ch.send(opus), 20L * i, TimeUnit.MILLISECONDS);
        }
        POOL.schedule(() -> { ch.flush(); enc.close(); }, 20L * frames + 40, TimeUnit.MILLISECONDS);
    }

    private static byte[] wav(byte[] pcm) {
        int rate = 48000, n = pcm.length;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            o.write("RIFF".getBytes()); le(o, 36 + n, 4); o.write("WAVEfmt ".getBytes()); le(o, 16, 4);
            le(o, 1, 2); le(o, 1, 2); le(o, rate, 4); le(o, rate * 2, 4); le(o, 2, 2); le(o, 16, 2);
            o.write("data".getBytes()); le(o, n, 4); o.write(pcm);
        } catch (Exception ignored) {}
        return o.toByteArray();
    }
    private static void le(ByteArrayOutputStream o, int v, int bytes) { for (int i = 0; i < bytes; i++) o.write((v >> (8 * i)) & 0xFF); }
}
