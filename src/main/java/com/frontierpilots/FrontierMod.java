package com.frontierpilots;

import com.frontierpilots.config.PilotConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(FrontierMod.MOD_ID)
public class FrontierMod {
    public static final String MOD_ID = "frontier";

    public FrontierMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PilotConfig.SPEC);
    }
}
