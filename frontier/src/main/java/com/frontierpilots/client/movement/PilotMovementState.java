package com.frontierpilots.client.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Estado de movimento do jogador local (so existe no cliente). */
public final class PilotMovementState {
    public LocalPlayer owner;

    // entrada
    public boolean jumpHeldLast;
    public boolean slideHeldLast;

    // chao / ar
    public boolean hopFlag;            // houve tentativa de pulo no chao no tick anterior
    public double hopFriction = 0.91;  // atrito do chao naquele tick (para compensar)
    public int airTicks;
    public int airJumpsUsed;

    // slide
    public boolean sliding;
    public int slideTicks;
    public int slideBuffer;
    public int slideCooldown;
    public double slideSpeed;
    public Vec3 slideDir = Vec3.ZERO;

    // wallrun
    public boolean wallRunning;
    public int wallTicks;
    public int wallLostTicks;
    public int wallCooldown;
    public Vec3 wallNormal = Vec3.ZERO;   // aponta da parede para o jogador
    public Vec3 wallTangent = Vec3.ZERO;  // direcao da corrida ao longo da parede

    // camera
    public float roll;
    public float prevRoll;

    // debug
    public String debugState = "";

    public void reset() {
        jumpHeldLast = false;
        slideHeldLast = false;
        hopFlag = false;
        hopFriction = 0.91;
        airTicks = 0;
        airJumpsUsed = 0;
        sliding = false;
        slideTicks = 0;
        slideBuffer = 0;
        slideCooldown = 0;
        slideSpeed = 0;
        slideDir = Vec3.ZERO;
        wallRunning = false;
        wallTicks = 0;
        wallLostTicks = 0;
        wallCooldown = 0;
        wallNormal = Vec3.ZERO;
        wallTangent = Vec3.ZERO;
        roll = 0;
        prevRoll = 0;
        debugState = "";
    }
}
