package com.arachneplus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpawnScreen extends PageScreen {
	public SpawnScreen(Screen parent) {
		super("Spawn timer", parent);
	}

	private static String modeName(int mode) {
		return switch (mode) {
			case Config.SPAWN_AUTO -> "Automatic";
			case Config.SPAWN_QUICK -> "Quick place (22s)";
			default -> "No quick placing (40s)";
		};
	}

	@Override
	protected void build() {
		full(cycleButton(x -> Component.literal("Timer type: " + modeName(Config.spawnMode)), () -> {
			Config.spawnMode = (Config.spawnMode + 1) % 3;
			if (Config.spawnMode == Config.SPAWN_AUTO) {
				ArachnePlusClient.say("Automatic spawn timer: turn OFF all other particle-hiding mods and settings"
						+ " (keep Minecraft's Particles on All). This mod's own 'Hide all particles' is fine to use.");
			}
		}));
		full(cycleButton(x -> Component.literal("Show timer as: " + Config.SPAWN_DISPLAY_NAMES[Config.spawnDisplay]), () -> {
			Config.spawnDisplay = (Config.spawnDisplay + 1) % 4;
			Config.showSpawnTimer = Config.spawnDisplay != 0;
		}));
		opens("Hologram...", () -> new HologramScreen(this));
		half(net.minecraft.client.gui.components.Button.builder(Component.literal("Start spawn timer"), b -> {
			var mc = this.minecraft;
			mc.setScreenAndShow(null);
			ArachnePlusClient.manualSpawnStart(mc);
		}).bounds(0, 0, W, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		if (Config.spawnMode == Config.SPAWN_AUTO) {
			String a = "Automatic: turn off other particle-hiding mods/settings (this mod's own is fine).";
			g.text(this.font, a, this.width / 2 - this.font.width(a) / 2, this.height - 22, Theme.gold(), true);
		}
	}
}
