package com.frontierpilots.client;

import com.frontierpilots.FrontierMod;
import com.frontierpilots.client.movement.PilotMovement;
import com.frontierpilots.client.movement.PilotMovementState;
import com.frontierpilots.config.PilotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos do barramento do Forge, so no cliente. */
@Mod.EventBusSubscriber(modid = FrontierMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    /** Dispara uma vez por tick, depois de ler o teclado e antes da fisica do jogador. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            PilotMovement.onInput(player, event.getInput());
        }
    }

    /** Inclinacao (roll) da camera durante o wallrun. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        PilotMovementState s = PilotMovement.STATE;
        if (!PilotConfig.ENABLED.get() || s.owner == null) return;
        float roll = Mth.lerp((float) event.getPartialTick(), s.prevRoll, s.roll);
        event.setRoll(event.getRoll() + roll);
    }

    /** Abre um pouco o FOV conforme a velocidade horizontal. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || event.getPlayer() != self || !PilotConfig.ENABLED.get()) return;
        Vec3 v = self.getDeltaMovement();
        double speed = Math.hypot(v.x, v.z);
        double t = Mth.clamp((speed - 0.30) / 0.45, 0.0, 1.0);
        event.setNewFovModifier(event.getNewFovModifier() * (float) (1.0 + t * PilotConfig.FOV_BOOST.get()));
    }
}
