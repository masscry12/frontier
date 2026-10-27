package com.frontierpilots.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Todos os numeros de "game feel" ficam aqui, em unidades de blocos por tick (1 b/tick = 20 b/s).
 * Referencia: andar = 0.216, correr = 0.28, pulo correndo ~ 0.35.
 * Arquivo gerado em run/config/frontier-common.toml.
 */
public final class PilotConfig {
    public static final ForgeConfigSpec SPEC;

    // geral
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue NO_FALL_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.DoubleValue MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL;
    public static final ForgeConfigSpec.DoubleValue FOV_BOOST;

    // chao
    public static final ForgeConfigSpec.DoubleValue GROUND_SPRINT_BOOST;

    // ar
    public static final ForgeConfigSpec.DoubleValue AIR_RETENTION;
    public static final ForgeConfigSpec.DoubleValue AIR_ACCEL;
    public static final ForgeConfigSpec.DoubleValue AIR_SPEED_CAP;

    // pulo duplo
    public static final ForgeConfigSpec.BooleanValue DJ_ENABLED;
    public static final ForgeConfigSpec.IntValue DJ_COUNT;
    public static final ForgeConfigSpec.DoubleValue DJ_POWER;
    public static final ForgeConfigSpec.DoubleValue DJ_REDIRECT;

    // wallrun
    public static final ForgeConfigSpec.BooleanValue WALL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WALL_SPEED;
    public static final ForgeConfigSpec.IntValue WALL_MAX_TICKS;
    public static final ForgeConfigSpec.DoubleValue WALL_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue WALL_RISE;
    public static final ForgeConfigSpec.DoubleValue WALL_SINK;
    public static final ForgeConfigSpec.DoubleValue WALL_STICK;
    public static final ForgeConfigSpec.DoubleValue WALL_PROBE;
    public static final ForgeConfigSpec.IntValue WALL_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WALL_JUMP_UP;
    public static final ForgeConfigSpec.DoubleValue WALL_JUMP_PUSH;
    public static final ForgeConfigSpec.BooleanValue WALL_JUMP_REFRESH;

    // slide
    public static final ForgeConfigSpec.BooleanValue SLIDE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SLIDE_BOOST;
    public static final ForgeConfigSpec.DoubleValue SLIDE_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SLIDE_DECAY;
    public static final ForgeConfigSpec.DoubleValue SLIDE_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue SLIDE_START_MIN_SPEED;
    public static final ForgeConfigSpec.IntValue SLIDE_MAX_TICKS;
    public static final ForgeConfigSpec.IntValue SLIDE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue SLIDE_STEER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ENABLED = b.comment("Liga/desliga todo o sistema de movimentacao de piloto.")
                .define("enabled", true);
        NO_FALL_DAMAGE = b.comment("Remove o dano de queda (pilotos nao sofrem queda em Titanfall 2).")
                .define("noFallDamage", true);
        DEBUG = b.comment("Mostra velocidade e estado do movimento na action bar (util para testes).")
                .define("debug", true);
        MAX_SPEED = b.comment("Velocidade horizontal maxima absoluta (blocos/tick).")
                .defineInRange("maxHorizontalSpeed", 0.85, 0.3, 3.0);
        CAMERA_ROLL = b.comment("Inclinacao da camera no wallrun, em graus. Se inclinar PARA a parede, use valor negativo.")
                .defineInRange("cameraRollDegrees", 7.0, -20.0, 20.0);
        FOV_BOOST = b.comment("Aumento maximo do FOV em alta velocidade (0.2 = +20%).")
                .defineInRange("fovBoost", 0.18, 0.0, 0.6);
        b.pop();

        b.push("ground");
        GROUND_SPRINT_BOOST = b.comment("Aceleracao extra ao correr no chao (blocos/tick por tick). 0 = velocidade vanilla.")
                .defineInRange("sprintBoost", 0.035, 0.0, 0.2);
        b.pop();

        b.push("air");
        AIR_RETENTION = b.comment("Quanto da velocidade horizontal se mantem por tick no ar. Vanilla = 0.91. 1.0 = sem arrasto.")
                .defineInRange("momentumRetention", 0.992, 0.8, 1.0);
        AIR_ACCEL = b.comment("Aceleracao de controle aereo (blocos/tick por tick). Vanilla = 0.02.")
                .defineInRange("airAcceleration", 0.04, 0.0, 0.3);
        AIR_SPEED_CAP = b.comment("Velocidade ate a qual o controle aereo consegue empurrar. Acima disso so redireciona, nao acelera.")
                .defineInRange("airSpeedCap", 0.40, 0.1, 2.0);
        b.pop();

        b.push("doubleJump");
        DJ_ENABLED = b.define("enabled", true);
        DJ_COUNT = b.comment("Pulos extras no ar.").defineInRange("airJumps", 1, 0, 5);
        DJ_POWER = b.comment("Impulso vertical do pulo duplo (pulo vanilla = 0.42).")
                .defineInRange("power", 0.52, 0.1, 1.5);
        DJ_REDIRECT = b.comment("Quanto o pulo duplo redireciona a velocidade para a direcao do teclado (0 a 1).")
                .defineInRange("redirect", 0.5, 0.0, 1.0);
        b.pop();

        b.push("wallrun");
        WALL_ENABLED = b.define("enabled", true);
        WALL_SPEED = b.comment("Velocidade minima mantida durante o wallrun.")
                .defineInRange("speed", 0.38, 0.1, 1.5);
        WALL_MAX_TICKS = b.comment("Duracao maxima do wallrun em ticks (20 = 1s).")
                .defineInRange("maxTicks", 45, 5, 400);
        WALL_MIN_SPEED = b.comment("Velocidade horizontal minima para engatar numa parede.")
                .defineInRange("minSpeed", 0.12, 0.0, 1.0);
        WALL_RISE = b.comment("Velocidade vertical no inicio do wallrun (sobe um pouco).")
                .defineInRange("rise", 0.10, 0.0, 0.5);
        WALL_SINK = b.comment("Velocidade de queda no fim do wallrun.")
                .defineInRange("sink", 0.06, 0.0, 0.5);
        WALL_STICK = b.comment("Forca que cola o jogador na parede.")
                .defineInRange("stick", 0.05, 0.0, 0.3);
        WALL_PROBE = b.comment("Distancia de deteccao da parede (blocos).")
                .defineInRange("probeDistance", 0.25, 0.05, 1.0);
        WALL_COOLDOWN = b.comment("Ticks antes de poder engatar em parede de novo apos sair/pular.")
                .defineInRange("cooldownTicks", 10, 0, 100);
        WALL_JUMP_UP = b.defineInRange("wallJumpUp", 0.50, 0.1, 1.5);
        WALL_JUMP_PUSH = b.comment("Empurrao para longe da parede no wall jump.")
                .defineInRange("wallJumpPush", 0.42, 0.0, 1.5);
        WALL_JUMP_REFRESH = b.comment("Wall jump devolve o pulo duplo.")
                .define("wallJumpRefreshesDoubleJump", true);
        b.pop();

        b.push("slide");
        SLIDE_ENABLED = b.define("enabled", true);
        SLIDE_BOOST = b.comment("Velocidade ganha ao iniciar o slide.")
                .defineInRange("startBoost", 0.10, 0.0, 0.6);
        SLIDE_MAX_SPEED = b.defineInRange("maxSpeed", 0.55, 0.1, 2.0);
        SLIDE_DECAY = b.comment("Multiplicador de velocidade por tick durante o slide (atrito).")
                .defineInRange("decay", 0.978, 0.8, 1.0);
        SLIDE_MIN_SPEED = b.comment("O slide termina abaixo desta velocidade.")
                .defineInRange("endSpeed", 0.15, 0.0, 1.0);
        SLIDE_START_MIN_SPEED = b.comment("Velocidade minima para iniciar um slide.")
                .defineInRange("startMinSpeed", 0.20, 0.0, 1.0);
        SLIDE_MAX_TICKS = b.defineInRange("maxTicks", 60, 5, 400);
        SLIDE_COOLDOWN = b.defineInRange("cooldownTicks", 8, 0, 100);
        SLIDE_STEER = b.comment("Quanto WASD curva o slide.")
                .defineInRange("steer", 0.06, 0.0, 0.5);
        b.pop();

        SPEC = b.build();
    }

    private PilotConfig() {}
}
