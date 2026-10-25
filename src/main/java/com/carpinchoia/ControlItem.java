package com.carpinchoia;

import com.carpinchoia.entity.CarpinchoEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Control remoto: clic al aire = cambia el radio; shift+clic al carpincho = zona centrada donde estás parado. */
public class ControlItem extends Item {
    static final int[] RADII = {8, 16, 24, 32, 48, 64};

    public ControlItem(Properties p) { super(p); }

    public static int getRadius(ItemStack s) {
        return s.hasTag() && s.getTag().contains("radius") ? s.getTag().getInt("radius") : 16;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack s = p.getItemInHand(h);
        if (!l.isClientSide) {
            int idx = 0;
            for (int i = 0; i < RADII.length; i++) if (RADII[i] == getRadius(s)) idx = i;
            int r = RADII[(idx + 1) % RADII.length];
            s.getOrCreateTag().putInt("radius", r);
            p.displayClientMessage(Component.literal("§6Radio del control: §f" + r + " bloques"), true);
        }
        return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack s, Player p, LivingEntity t, InteractionHand h) {
        if (!(t instanceof CarpinchoEntity c)) return InteractionResult.PASS;
        if (p.level().isClientSide) return InteractionResult.SUCCESS;
        if (c.getOwners().isEmpty()) {
            c.addOwner(p.getUUID());
            p.sendSystemMessage(Component.literal("§aAhora sos dueño de " + c.getName().getString()));
        } else if (!c.isOwner(p) && !p.hasPermissions(2)) {
            p.sendSystemMessage(Component.literal("§cNo sos dueño de este carpincho."));
        } else if (p.isShiftKeyDown()) {
            c.setZone(p.blockPosition(), getRadius(s));
            p.sendSystemMessage(Component.literal("§aZona fijada en " + p.blockPosition().toShortString()
                + " radio " + getRadius(s)));
        } else {
            p.sendSystemMessage(Component.literal("§6" + c.getName().getString() + " §f| zona: "
                + (c.getZoneCenter() == null ? "libre" : c.getZoneCenter().toShortString() + " r=" + c.getZoneRadius())
                + " | alerta: " + (c.isAlerts() ? "ON" : "OFF") + " | dueños: " + c.getOwners().size()));
        }
        return InteractionResult.SUCCESS;
    }
}
