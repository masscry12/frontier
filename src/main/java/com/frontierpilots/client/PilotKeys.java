package com.frontierpilots.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class PilotKeys {
    public static final KeyMapping SLIDE = new KeyMapping(
            "key.frontier.slide",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.frontier");

    private PilotKeys() {}
}
