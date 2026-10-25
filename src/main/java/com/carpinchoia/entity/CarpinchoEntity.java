package com.carpinchoia.entity;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.Config;
import com.carpinchoia.Net;
import com.carpinchoia.voice.CarpinchoVoicePlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.function.Consumer;

/** Carpincho bípedo. Inmortal (solo se elimina con /carpinchoeliminar). */
public class CarpinchoEntity extends PathfinderMob {
    public static final int MOMENTO_TICKS = 72000; // 1 hora

    private final Set<UUID> owners = new HashSet<>();
    private BlockPos zoneCenter = null;
    private int zoneRadius = 16;
    private boolean alerts = true;
    private int momentoLeft = 0;
    private UUID partner = null;
    private int alertCooldown = 0;

    public CarpinchoEntity(EntityType<? extends CarpinchoEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 8)
            .add(Attributes.FOLLOW_RANGE, 24)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public boolean isFemale() { return false; }

    // ---------- IA ----------
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Corre de jugadores que no son dueños (salvo en Momento Carpincho)
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8F, 1.0, 1.5,
            e -> e instanceof Player p && !isMomento() && !isOwner(p) && !p.isCreative() && !p.isSpectator()));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override public boolean canUse() { return isMomento() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isMomento() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Defensa: solo durante Momento Carpincho, y nunca contra dueños
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            e -> isMomento() && e instanceof Player p && !isOwner(p) && !p.isCreative() && isInZone(p)));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            e -> isMomento() && isInZone(e)));
    }

    // ---------- Inmortal ----------
    @Override public boolean hurt(DamageSource s, float amount) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(CarpinchoIA.STEP.get(), 0.6F, 1.0F);
    }

    @Override
    public void remove(RemovalReason r) {
        if (!level().isClientSide && r.shouldDestroy() && partner != null && level() instanceof ServerLevel sl) {
            Entity m = sl.getEntity(partner);
            if (m instanceof CarpinchoEntity c) c.partner = null;
        }
        super.remove(r);
    }

    // ---------- Tick ----------
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) serverTick();
    }

    private void serverTick() {
        ServerLevel sl = (ServerLevel) level();
        if (momentoLeft > 0) {
            momentoLeft--;
            if (momentoLeft == 0) {
                tellOwners(Component.literal("§6§l¡Momento Carpincho terminado!"));
            } else if (tickCount % 20 == 0) {
                int s = momentoLeft / 20;
                Component t = Component.literal(String.format("§6Momento Carpincho: §f%02d:%02d", s / 60, s % 60));
                forEachOwner(o -> o.displayClientMessage(t, true));
            }
        }
        if (alertCooldown > 0) alertCooldown--;
        if (alerts && tickCount % 20 == 0 && alertCooldown == 0) checkIntruders();
        if (tickCount % 10 == 0) {
            double r = Config.HEAR_RANGE.get();
            forEachOwner(o -> { if (o.distanceToSqr(this) <= r * r) CarpinchoVoicePlugin.listen(o.getUUID(), getUUID()); });
        }
        if (tickCount % 40 == 0) love(sl);
        if (getY() < sl.getMinBuildHeight() - 8) { // cayó al vacío: rescate
            BlockPos t = zoneCenter != null ? zoneCenter : sl.getSharedSpawnPos();
            teleportTo(t.getX() + 0.5, t.getY() + 1, t.getZ() + 0.5);
            fallDistance = 0;
        }
    }

    private void checkIntruders() {
        BlockPos c = zoneCenter != null ? zoneCenter : blockPosition();
        AABB box = new AABB(c).inflate(zoneRadius);
        for (Player p : level().getEntitiesOfClass(Player.class, box,
                pl -> !isOwner(pl) && !pl.isSpectator() && pl.isAlive())) {
            alertCooldown = 200;
            Component msg = Component.literal("§c[Alerta de base] §f" + getName().getString()
                + " detectó a §e" + p.getName().getString() + " §fen la base (" + p.blockPosition().toShortString() + ")");
            forEachOwner(o -> {
                o.sendSystemMessage(msg);
                o.playNotifySound(CarpinchoIA.ALERT.get(), SoundSource.NEUTRAL, 1F, 1F);
            });
            break;
        }
    }

    private void love(ServerLevel sl) {
        if (partner == null) {
            if (tickCount < 100) return;
            List<CarpinchoEntity> l = level().getEntitiesOfClass(CarpinchoEntity.class, getBoundingBox().inflate(10),
                e -> e != this && e.isAlive() && e.isFemale() != isFemale() && e.partner == null);
            if (!l.isEmpty()) {
                CarpinchoEntity m = l.get(0);
                partner = m.getUUID();
                m.partner = getUUID();
                hearts(sl, 12); m.hearts(sl, 12);
                Component msg = Component.literal("§d♥ " + getName().getString() + " y " + m.getName().getString() + " ahora son novios ♥");
                tellOwners(msg); m.tellOwners(msg);
            }
        } else {
            Entity m = sl.getEntity(partner);
            if (m != null && m.isAlive()) {
                if (distanceToSqr(m) > 36 && !isMomento()) getNavigation().moveTo(m, 1.0);
                else if (distanceToSqr(m) < 9 && random.nextInt(4) == 0) hearts(sl, 2);
            }
        }
    }

    private void hearts(ServerLevel sl, int n) {
        sl.sendParticles(ParticleTypes.HEART, getX(), getEyeY() + 0.3, getZ(), n, 0.3, 0.3, 0.3, 0);
    }

    // ---------- Momento Carpincho ----------
    public boolean isMomento() { return momentoLeft > 0; }

    public void startMomento() {
        momentoLeft = MOMENTO_TICKS;
        tellOwners(Component.literal("§6§l¡MOMENTO CARPINCHO! §fDura 1 hora."));
        forEachOwner(Net::totem);
        level().playSound(null, blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 1F, 1F);
    }

    // ---------- Dueños / zona ----------
    public boolean isOwner(Player p) { return owners.contains(p.getUUID()); }
    public Set<UUID> getOwners() { return owners; }
    public void addOwner(UUID id) { owners.add(id); }
    public void removeOwner(UUID id) { owners.remove(id); }
    public BlockPos getZoneCenter() { return zoneCenter; }
    public int getZoneRadius() { return zoneRadius; }
    public boolean isAlerts() { return alerts; }
    public void setAlerts(boolean a) { alerts = a; }

    public void setZone(BlockPos c, int radius) {
        zoneCenter = c;
        zoneRadius = Math.max(1, Math.min(64, radius));
        restrictTo(zoneCenter, zoneRadius);
    }

    public boolean isInZone(Entity e) {
        if (zoneCenter == null) return e.distanceToSqr(this) <= 24 * 24;
        return e.blockPosition().distSqr(zoneCenter) <= (double) zoneRadius * zoneRadius;
    }

    public void forEachOwner(Consumer<ServerPlayer> c) {
        if (level().getServer() == null) return;
        for (UUID id : owners) {
            ServerPlayer sp = level().getServer().getPlayerList().getPlayer(id);
            if (sp != null) c.accept(sp);
        }
    }

    public void tellOwners(Component m) { forEachOwner(o -> o.sendSystemMessage(m)); }

    // ---------- NBT ----------
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        ListTag l = new ListTag();
        for (UUID id : owners) l.add(NbtUtils.createUUID(id));
        t.put("Owners", l);
        if (zoneCenter != null) {
            t.putInt("ZX", zoneCenter.getX()); t.putInt("ZY", zoneCenter.getY()); t.putInt("ZZ", zoneCenter.getZ());
        }
        t.putInt("ZoneRadius", zoneRadius);
        t.putBoolean("Alerts", alerts);
        t.putInt("MomentoLeft", momentoLeft);
        if (partner != null) t.putUUID("Partner", partner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        owners.clear();
        for (Tag x : t.getList("Owners", Tag.TAG_INT_ARRAY)) owners.add(NbtUtils.loadUUID(x));
        zoneRadius = t.contains("ZoneRadius") ? t.getInt("ZoneRadius") : 16;
        if (t.contains("ZX")) setZone(new BlockPos(t.getInt("ZX"), t.getInt("ZY"), t.getInt("ZZ")), zoneRadius);
        alerts = !t.contains("Alerts") || t.getBoolean("Alerts");
        momentoLeft = t.getInt("MomentoLeft");
        partner = t.hasUUID("Partner") ? t.getUUID("Partner") : null;
    }
}
