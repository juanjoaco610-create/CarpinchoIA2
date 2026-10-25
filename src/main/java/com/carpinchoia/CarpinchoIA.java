package com.carpinchoia;

import com.carpinchoia.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(CarpinchoIA.ID)
public class CarpinchoIA {
    public static final String ID = "carpinchoia";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<EntityType<CarpinchoEntity>> CARPINCHO = ENTITIES.register("carpincho",
        () -> EntityType.Builder.<CarpinchoEntity>of(CarpinchoEntity::new, MobCategory.CREATURE)
            .sized(0.8F, 2.0F).clientTrackingRange(10).build(ID + ":carpincho"));
    public static final RegistryObject<EntityType<CarpinchaEntity>> CARPINCHA = ENTITIES.register("carpincha",
        () -> EntityType.Builder.<CarpinchaEntity>of(CarpinchaEntity::new, MobCategory.CREATURE)
            .sized(0.8F, 2.0F).clientTrackingRange(10).build(ID + ":carpincha"));

    public static final RegistryObject<Item> CARPINCHO_EGG = ITEMS.register("carpincho_spawn_egg",
        () -> new ForgeSpawnEggItem(CARPINCHO, 0x8B5A2B, 0x3E2A14, new Item.Properties()));
    public static final RegistryObject<Item> CARPINCHA_EGG = ITEMS.register("carpincha_spawn_egg",
        () -> new ForgeSpawnEggItem(CARPINCHA, 0xC08A5A, 0xE58FAE, new Item.Properties()));
    public static final RegistryObject<Item> CONTROL = ITEMS.register("carpincho_control",
        () -> new ControlItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TOTEM = ITEMS.register("carpincho_totem",
        () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<SoundEvent> STEP = SOUNDS.register("carpincho_step",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ID, "carpincho_step")));
    public static final RegistryObject<SoundEvent> ALERT = SOUNDS.register("carpincho_alert",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ID, "carpincho_alert")));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("carpinchoia",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.carpinchoia"))
            .icon(() -> new ItemStack(CARPINCHO_EGG.get()))
            .displayItems((p, out) -> {
                out.accept(CARPINCHO_EGG.get());
                out.accept(CARPINCHA_EGG.get());
                out.accept(CONTROL.get());
            }).build());

    public CarpinchoIA() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
        bus.addListener(this::attributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        Net.init();
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(CARPINCHO.get(), CarpinchoEntity.attrs().build());
        e.put(CARPINCHA.get(), CarpinchoEntity.attrs().build());
    }
}
