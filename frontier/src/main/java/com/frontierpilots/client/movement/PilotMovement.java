package com.frontierpilots.client.movement;

import com.frontierpilots.client.PilotKeys;
import com.frontierpilots.config.PilotConfig;
import java.util.Locale;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Movimento de piloto (estilo Titanfall 2), executado no cliente uma vez por tick,
 * logo depois do teclado ser lido e ANTES da fisica vanilla (travel/jump).
 *
 * Ideia central: o jogador local e autoritativo sobre a propria posicao no Minecraft, entao
 * escrevemos direto no deltaMovement e deixamos a colisao vanilla fazer o resto.
 *
 * Conservacao de impulso:
 *  - no ar o arrasto vanilla (0.91/tick) e desfeito e trocado por PilotConfig.AIR_RETENTION;
 *  - a aceleracao aerea vanilla (0.02) e subtraida antes e substituida pelo nosso controle aereo;
 *  - o atrito de chao perdido no tick do pulo e devolvido (bhop/slide-hop mantem velocidade).
 */
public final class PilotMovement {
    private static final double VANILLA_AIR_FRICTION = 0.91;
    private static final double EPS = 1.0E-4;

    public static final PilotMovementState STATE = new PilotMovementState();

    private PilotMovement() {}

    /** Dados do tick atual, para nao passar 10 parametros entre os metodos. */
    private static final class Ctx {
        final LocalPlayer p;
        final Input in;
        final PilotMovementState s;
        final Vec3 wish;          // direcao desejada no mundo (|wish| <= 1)
        final float fwd;
        final boolean jumpPressed;
        final boolean slideKey;

        Ctx(LocalPlayer p, Input in, PilotMovementState s, Vec3 wish, float fwd, boolean jumpPressed, boolean slideKey) {
            this.p = p;
            this.in = in;
            this.s = s;
            this.wish = wish;
            this.fwd = fwd;
            this.jumpPressed = jumpPressed;
            this.slideKey = slideKey;
        }

        boolean hasWish() {
            return wish.lengthSqr() > EPS;
        }
    }

    private record Wall(Vec3 normal, double into) {}

    // ------------------------------------------------------------------------------------------
    // Ponto de entrada
    // ------------------------------------------------------------------------------------------

    public static void onInput(LocalPlayer p, Input in) {
        PilotMovementState s = STATE;
        if (s.owner != p) {
            s.reset();
            s.owner = p;
        }

        if (!PilotConfig.ENABLED.get() || !isEligible(p)) {
            boolean jumping = in.jumping;
            s.reset();
            s.jumpHeldLast = jumping;
            return;
        }

        final float fwd = in.forwardImpulse;
        final Vec3 wish = wishVector(p, in.leftImpulse, fwd);
        final boolean jumpPressed = in.jumping && !s.jumpHeldLast;
        final boolean slideKey = PilotKeys.SLIDE.isDown();
        final boolean slidePressed = slideKey && !s.slideHeldLast;
        s.jumpHeldLast = in.jumping;
        s.slideHeldLast = slideKey;

        if (s.slideCooldown > 0) s.slideCooldown--;
        if (s.wallCooldown > 0) s.wallCooldown--;
        if (s.slideBuffer > 0) s.slideBuffer--;
        if (slidePressed) s.slideBuffer = 6; // aceita apertar C um pouco antes de pousar

        Ctx c = new Ctx(p, in, s, wish, fwd, jumpPressed, slideKey);

        if (p.onGround()) {
            s.airTicks = 0;
            s.airJumpsUsed = 0;
            if (s.wallRunning) endWallRun(s, 0);
            groundTick(c);
        } else {
            s.airTicks++;
            airTick(c);
        }

        updateCamera(c);
        if (PilotConfig.DEBUG.get()) debugOverlay(c);
    }

    private static boolean isEligible(LocalPlayer p) {
        return p.isAlive()
                && !p.isSpectator()
                && !p.isSleeping()
                && !p.isPassenger()
                && !p.isFallFlying()
                && !p.isInWater()
                && !p.isUnderWater()
                && !p.isInLava()
                && !p.isSwimming()
                && !p.onClimbable()
                && !p.getAbilities().flying;
    }

    // ------------------------------------------------------------------------------------------
    // Chao: corrida + slide
    // ------------------------------------------------------------------------------------------

    private static void groundTick(Ctx c) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        Vec3 v = p.getDeltaMovement();
        double hSpeed = Math.hypot(v.x, v.z);

        if (!s.sliding && PilotConfig.SLIDE_ENABLED.get() && c.slideKey && s.slideBuffer > 0
                && s.slideCooldown == 0 && hSpeed >= PilotConfig.SLIDE_START_MIN_SPEED.get()) {
            startSlide(c, v, hSpeed);
        }
        if (s.sliding) {
            slideTick(c);
            return;
        }

        // Se o jogador esta segurando pulo, o vanilla vai pular neste tick e aplicar atrito de chao
        // (0.546) no fim dele. Guardamos isso para devolver no proximo tick (airTick).
        s.hopFlag = c.in.jumping;
        s.hopFriction = groundFriction(p);

        // Corrida mais rapida que a vanilla: empurrao extra na direcao desejada.
        if (p.isSprinting() && c.hasWish() && hSpeed < PilotConfig.MAX_SPEED.get()) {
            double boost = PilotConfig.GROUND_SPRINT_BOOST.get();
            p.setDeltaMovement(v.x + c.wish.x * boost, v.y, v.z + c.wish.z * boost);
        }
    }

    private static void startSlide(Ctx c, Vec3 v, double hSpeed) {
        PilotMovementState s = c.s;
        Vec3 dir = hSpeed > 1.0E-3 ? new Vec3(v.x / hSpeed, 0, v.z / hSpeed) : lookHorizontal(c.p);
        s.sliding = true;
        s.slideTicks = 0;
        s.slideDir = dir;
        s.slideSpeed = Math.min(hSpeed + PilotConfig.SLIDE_BOOST.get(), PilotConfig.SLIDE_MAX_SPEED.get());
        s.slideBuffer = 0;
        sound(c.p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 0.8F);
    }

    private static void slideTick(Ctx c) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        s.slideTicks++;
        Vec3 v = p.getDeltaMovement();

        // Slide-hop: pular durante o slide mantem toda a velocidade.
        if (c.in.jumping) {
            p.setDeltaMovement(s.slideDir.x * s.slideSpeed, v.y, s.slideDir.z * s.slideSpeed);
            s.hopFlag = true;
            s.hopFriction = groundFriction(p);
            endSlide(s, 4);
            return;
        }

        boolean end = !c.slideKey
                || s.slideTicks > PilotConfig.SLIDE_MAX_TICKS.get()
                || s.slideSpeed < PilotConfig.SLIDE_MIN_SPEED.get()
                || p.horizontalCollision;
        if (end) {
            endSlide(s, PilotConfig.SLIDE_COOLDOWN.get());
            return;
        }

        s.slideSpeed *= PilotConfig.SLIDE_DECAY.get();
        if (c.hasWish()) {
            Vec3 d = s.slideDir.add(c.wish.scale(PilotConfig.SLIDE_STEER.get()));
            double len = d.length();
            if (len > EPS) s.slideDir = d.scale(1.0 / len);
        }
        p.setDeltaMovement(s.slideDir.x * s.slideSpeed, v.y, s.slideDir.z * s.slideSpeed);

        // Sem WASD durante o slide: a velocidade e nossa, nao a vanilla.
        c.in.forwardImpulse = 0.0F;
        c.in.leftImpulse = 0.0F;
    }

    private static void endSlide(PilotMovementState s, int cooldown) {
        s.sliding = false;
        s.slideTicks = 0;
        s.slideCooldown = cooldown;
    }

    // ------------------------------------------------------------------------------------------
    // Ar: controle aereo, pulo duplo, wallrun
    // ------------------------------------------------------------------------------------------

    private static void airTick(Ctx c) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        if (s.sliding) endSlide(s, PilotConfig.SLIDE_COOLDOWN.get()); // saiu de uma borda: mantem o impulso

        Vec3 v = p.getDeltaMovement();

        // 1) troca o arrasto vanilla pelo nosso
        double friction = s.hopFlag ? s.hopFriction : VANILLA_AIR_FRICTION;
        s.hopFlag = false;
        Vec3 h = new Vec3(v.x, 0, v.z).scale(PilotConfig.AIR_RETENTION.get() / friction);

        // O vanilla ainda vai somar isto depois de nos; subtraimos para ter controle total.
        Vec3 vanillaAir = c.wish.scale(p.isSprinting() ? 0.026 : 0.02);

        // 2) wallrun / wall jump
        if (PilotConfig.WALL_ENABLED.get() && handleWall(c, h, vanillaAir)) return;

        // 3) pulo duplo
        if (PilotConfig.DJ_ENABLED.get() && c.jumpPressed && s.airTicks >= 2
                && s.airJumpsUsed < PilotConfig.DJ_COUNT.get()) {
            doubleJump(c, h);
            return;
        }

        // 4) controle aereo: curva e acelera ate o limite, sem nunca frear quem ja esta mais rapido
        Vec3 out = h;
        if (c.hasWish()) {
            Vec3 steered = h.add(c.wish.scale(PilotConfig.AIR_ACCEL.get()));
            double limit = Math.min(Math.max(h.length(), PilotConfig.AIR_SPEED_CAP.get()), PilotConfig.MAX_SPEED.get());
            double len = steered.length();
            if (len > limit) steered = steered.scale(limit / len);
            out = steered;
        }
        out = clampHorizontal(out, PilotConfig.MAX_SPEED.get());
        out = out.subtract(vanillaAir);
        p.setDeltaMovement(out.x, v.y, out.z);
        s.debugState = "AR";
    }

    private static void doubleJump(Ctx c, Vec3 h) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        s.airJumpsUsed++;

        Vec3 out = h;
        if (c.hasWish()) {
            double speed = Math.max(h.length(), 0.22);
            Vec3 dir = c.wish.normalize();
            if (h.length() > 1.0E-3) {
                double r = PilotConfig.DJ_REDIRECT.get();
                Vec3 mixed = h.normalize().scale(1.0 - r).add(dir.scale(r));
                if (mixed.lengthSqr() > 1.0E-6) dir = mixed.normalize();
            }
            out = dir.scale(speed);
        }
        out = clampHorizontal(out, PilotConfig.MAX_SPEED.get());
        p.setDeltaMovement(out.x, PilotConfig.DJ_POWER.get(), out.z);
        p.fallDistance = 0.0F;
        burst(p);
        sound(p, SoundEvents.PHANTOM_FLAP, 0.8F, 1.4F);
        s.debugState = "PULO DUPLO";
    }

    // ---- wallrun ----

    /** @return true se este tick foi tratado pelo sistema de parede (velocidade ja definida). */
    private static boolean handleWall(Ctx c, Vec3 h, Vec3 vanillaAir) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        Vec3 hDir = h.lengthSqr() > 1.0E-6 ? h.normalize() : Vec3.ZERO;
        Wall wall = findWall(p, hDir);

        if (s.wallRunning) {
            if (c.jumpPressed) {
                wallJump(c, h);
                return true;
            }
            if (wall != null && wall.into() <= 0.92) {
                s.wallLostTicks = 0;
                s.wallNormal = wall.normal();
            } else {
                s.wallLostTicks++;
            }
            boolean stop = c.fwd <= 0.1F
                    || s.wallLostTicks > 2
                    || s.wallTicks >= PilotConfig.WALL_MAX_TICKS.get();
            if (stop) {
                endWallRun(s, PilotConfig.WALL_COOLDOWN.get());
                return false;
            }
            wallRunTick(c, h, vanillaAir);
            return true;
        }

        if (wall != null
                && !c.jumpPressed
                && s.wallCooldown == 0
                && s.airTicks >= 2
                && c.fwd > 0.3F
                && h.length() >= PilotConfig.WALL_MIN_SPEED.get()
                && wall.into() <= 0.8) {
            s.wallRunning = true;
            s.wallTicks = 0;
            s.wallLostTicks = 0;
            s.wallNormal = wall.normal();
            s.wallTangent = tangent(h, wall.normal(), lookHorizontal(p));
            wallRunTick(c, h, vanillaAir);
            return true;
        }
        return false;
    }

    private static void wallRunTick(Ctx c, Vec3 h, Vec3 vanillaAir) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        Vec3 n = s.wallNormal;
        Vec3 t = tangent(h, n, s.wallTangent);
        s.wallTangent = t;

        double speed = Math.min(Math.max(h.length(), PilotConfig.WALL_SPEED.get()), PilotConfig.MAX_SPEED.get());
        double progress = Math.min(1.0, (s.wallTicks / (double) PilotConfig.WALL_MAX_TICKS.get()) * 1.5);
        double y = Mth.lerp(progress, PilotConfig.WALL_RISE.get(), -PilotConfig.WALL_SINK.get());
        double stick = PilotConfig.WALL_STICK.get();

        Vec3 out = new Vec3(t.x * speed - n.x * stick, 0, t.z * speed - n.z * stick).subtract(vanillaAir);
        p.setDeltaMovement(out.x, y, out.z);
        p.fallDistance = 0.0F;
        s.wallTicks++;
        s.debugState = "WALLRUN " + s.wallTicks;
    }

    private static void wallJump(Ctx c, Vec3 h) {
        PilotMovementState s = c.s;
        LocalPlayer p = c.p;
        Vec3 n = s.wallNormal;
        Vec3 t = s.wallTangent;
        double along = Math.min(Math.max(h.length(), PilotConfig.WALL_SPEED.get() * 0.85), PilotConfig.MAX_SPEED.get());
        double push = PilotConfig.WALL_JUMP_PUSH.get();
        p.setDeltaMovement(t.x * along + n.x * push, PilotConfig.WALL_JUMP_UP.get(), t.z * along + n.z * push);
        p.fallDistance = 0.0F;
        endWallRun(s, PilotConfig.WALL_COOLDOWN.get());
        if (PilotConfig.WALL_JUMP_REFRESH.get()) s.airJumpsUsed = 0;
        burst(p);
        sound(p, SoundEvents.PHANTOM_FLAP, 0.8F, 1.1F);
        s.debugState = "WALL JUMP";
    }

    private static void endWallRun(PilotMovementState s, int cooldown) {
        s.wallRunning = false;
        s.wallTicks = 0;
        s.wallLostTicks = 0;
        s.wallCooldown = Math.max(s.wallCooldown, cooldown);
    }

    /**
     * Procura uma parede nas 4 direcoes cardeais. Usa a caixa do jogador sem os 0.35 de cima e de
     * baixo (para ignorar o chao e degraus) deslocada por PilotConfig.WALL_PROBE.
     * Escolhe a parede mais paralela a velocidade atual; 'into' vai de 0 (paralelo) a 1 (de frente).
     */
    private static Wall findWall(LocalPlayer p, Vec3 hDir) {
        AABB core = p.getBoundingBox().deflate(0.0, 0.35, 0.0);
        double probe = PilotConfig.WALL_PROBE.get();
        Wall best = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            AABB shifted = core.move(d.getStepX() * probe, 0.0, d.getStepZ() * probe);
            if (p.level().noCollision(p, shifted)) continue;
            Vec3 normal = new Vec3(-d.getStepX(), 0.0, -d.getStepZ());
            double into = hDir.lengthSqr() > 0.0 ? Math.abs(hDir.dot(normal)) : 0.0;
            if (best == null || into < best.into()) best = new Wall(normal, into);
        }
        return best;
    }

    private static Vec3 tangent(Vec3 h, Vec3 n, Vec3 fallback) {
        Vec3 t = h.subtract(n.scale(h.dot(n)));
        if (t.lengthSqr() < EPS) return fallback;
        return t.normalize();
    }

    // ------------------------------------------------------------------------------------------
    // Camera e debug
    // ------------------------------------------------------------------------------------------

    private static void updateCamera(Ctx c) {
        PilotMovementState s = c.s;
        float target = 0.0F;
        if (s.wallRunning) {
            double yaw = Math.toRadians(c.p.getYRot());
            double leftX = Math.cos(yaw);
            double leftZ = Math.sin(yaw);
            // parede a esquerda => towardWall > 0 => roll positivo (cabeca inclina para longe da parede)
            double towardWall = -(s.wallNormal.x * leftX + s.wallNormal.z * leftZ);
            target = (float) (Math.signum(towardWall) * PilotConfig.CAMERA_ROLL.get());
        }
        s.prevRoll = s.roll;
        s.roll = Mth.lerp(0.25F, s.roll, target);
    }

    private static void debugOverlay(Ctx c) {
        PilotMovementState s = c.s;
        Vec3 v = c.p.getDeltaMovement();
        double speed = Math.hypot(v.x, v.z);
        String state = s.sliding ? "SLIDE " + s.slideTicks
                : s.wallRunning ? "WALLRUN " + s.wallTicks
                : c.p.onGround() ? "CHAO"
                : s.debugState.isEmpty() ? "AR" : s.debugState;
        String text = String.format(Locale.ROOT, "%.2f b/s | %s | pulos extras %d/%d",
                speed * 20.0, state, PilotConfig.DJ_COUNT.get() - s.airJumpsUsed, PilotConfig.DJ_COUNT.get());
        c.p.displayClientMessage(Component.literal(text), true);
    }

    // ------------------------------------------------------------------------------------------
    // Utilitarios
    // ------------------------------------------------------------------------------------------

    /** Mesma conversao do vanilla (Entity.getInputVector): teclado -> direcao no mundo. */
    private static Vec3 wishVector(LocalPlayer p, float left, float forward) {
        double lenSq = left * left + forward * forward;
        if (lenSq < EPS) return Vec3.ZERO;
        double scale = lenSq > 1.0 ? 1.0 / Math.sqrt(lenSq) : 1.0;
        double l = left * scale;
        double f = forward * scale;
        double yaw = Math.toRadians(p.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(l * cos - f * sin, 0.0, f * cos + l * sin);
    }

    private static Vec3 lookHorizontal(LocalPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private static Vec3 clampHorizontal(Vec3 h, double max) {
        double len = h.length();
        return len > max ? h.scale(max / len) : h;
    }

    private static double groundFriction(LocalPlayer p) {
        float f = p.level().getBlockState(p.getOnPos()).getBlock().getFriction();
        return f * 0.91;
    }

    private static void sound(LocalPlayer p, SoundEvent sound, float volume, float pitch) {
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static void burst(LocalPlayer p) {
        for (int i = 0; i < 8; i++) {
            p.level().addParticle(ParticleTypes.CLOUD,
                    p.getX() + (p.getRandom().nextDouble() - 0.5) * 0.5,
                    p.getY() + 0.1,
                    p.getZ() + (p.getRandom().nextDouble() - 0.5) * 0.5,
                    0.0, -0.02, 0.0);
        }
    }
}
