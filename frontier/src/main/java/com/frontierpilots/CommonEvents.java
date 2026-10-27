package com.frontierpilots;

import com.frontierpilots.config.PilotConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos que rodam nos dois lados (cliente e servidor). */
@Mod.EventBusSubscriber(modid = FrontierMod.MOD_ID)
public final class CommonEvents {
    private CommonEvents() {}

    /** Pilotos nao tomam dano de queda (como em Titanfall 2). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player && PilotConfig.NO_FALL_DAMAGE.get()) {
            event.setDamageMultiplier(0.0F);
        }
    }
}
