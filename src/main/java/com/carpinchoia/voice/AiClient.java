package com.carpinchoia.voice;

import com.carpinchoia.Config;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Cliente HTTP hacia TU servidor de IA (voz -> texto -> LLM -> texto -> voz).
 * Request : POST aiUrl, body = WAV 16-bit mono 48kHz, headers X-Player y X-Carpincho.
 * Response: JSON {"text": "...", "audio": "<base64 PCM 16-bit LE mono 48kHz>"}  (audio opcional)
 * Si no se escuchó la palabra "carpincho" / no hay nada que responder, devolvé {"text": ""}.
 */
public class AiClient {
    public record Reply(String text, short[] pcm) {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public static CompletableFuture<Reply> ask(byte[] wav, String player, String carpincho) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(Config.AI_URL.get()))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "audio/wav")
            .header("X-Player", player)
            .header("X-Carpincho", carpincho)
            .POST(HttpRequest.BodyPublishers.ofByteArray(wav)).build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            String text = o.has("text") ? o.get("text").getAsString() : "";
            short[] pcm = null;
            if (o.has("audio") && !o.get("audio").getAsString().isEmpty()) {
                byte[] b = Base64.getDecoder().decode(o.get("audio").getAsString());
                pcm = new short[b.length / 2];
                for (int i = 0; i < pcm.length; i++) pcm[i] = (short) ((b[2 * i] & 0xFF) | (b[2 * i + 1] << 8));
            }
            return new Reply(text, pcm);
        });
    }
}
