package com.carpinchoia;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue STEP_VOLUME;
    public static final ForgeConfigSpec.DoubleValue ALERT_VOLUME;

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> AI_URL;
    public static final ForgeConfigSpec.IntValue HEAR_RANGE;

    static {
        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        STEP_VOLUME = c.defineInRange("stepVolume", 1.0, 0.0, 1.0);
        ALERT_VOLUME = c.defineInRange("alertVolume", 1.0, 0.0, 1.0);
        CLIENT_SPEC = c.build();

        ForgeConfigSpec.Builder s = new ForgeConfigSpec.Builder();
        AI_URL = s.comment("URL de tu servidor de IA (STT + LLM + TTS). Ver README.")
                  .define("aiUrl", "http://127.0.0.1:8765/carpincho");
        HEAR_RANGE = s.comment("Distancia (bloques) a la que el carpincho escucha a sus dueños")
                      .defineInRange("hearRange", 8, 2, 32);
        COMMON_SPEC = s.build();
    }
}
