package com.frontierpilots.client;

import com.frontierpilots.FrontierMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos do barramento do mod (registro de teclas). */
@Mod.EventBusSubscriber(modid = FrontierMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(PilotKeys.SLIDE);
    }
}
