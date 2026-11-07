package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HologramScreen extends ModScreen {
	private static final int W = 220;
	private static final int H = 20;
	private static final int GAP = 4;
	private static final int[] SIZES = {50, 70, 100, 130, 160, 200, 300};
	private final Screen parent;

	public HologramScreen(Screen parent) {
		super(Component.literal("Hologram"));
		this.parent = parent;
	}

	private Component onOff(String label, boolean on) {
		return Component.literal(label + ": ").append(Component.literal(on ? "ON" : "OFF")
				.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
	}

	@Override
	protected void init() {
		int x = this.width / 2 - W / 2;
		int y = this.height / 2 - 70;
		this.addRenderableWidget(Button.builder(Component.literal("Spawn display: " + Config.SPAWN_DISPLAY_NAMES[Config.spawnDisplay]), b -> {
			Config.spawnDisplay = (Config.spawnDisplay + 1) % 4;
			Config.showSpawnTimer = Config.spawnDisplay != 0;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(
				Component.literal("Size: " + (Config.holoScaleWithDistance ? "changes with distance" : "locked (same size)")), b -> {
			Config.holoScaleWithDistance = !Config.holoScaleWithDistance;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Size: " + Config.holoSize + "%"), b -> {
			int next = SIZES[0];
			for (int s : SIZES) {
				if (s > Config.holoSize) {
					next = s;
					break;
				}
			}
			Config.holoSize = next;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(onOff("Background", Config.holoBackground), b -> {
			Config.holoBackground = !Config.holoBackground;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(onOff("Minimal (just the number)", Config.holoMinimal), b -> {
			Config.holoMinimal = !Config.holoMinimal;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP + 6;
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreenAndShow(parent))
				.bounds(x, y, W, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		String t = "Spawn timer hologram";
		Logo.draw(g, this.font, this.width / 2f, this.height / 2 - 114, Logo.SMALL);
		g.text(this.font, t, this.width / 2 - this.font.width(t) / 2, this.height / 2 - 96, Theme.gold(), true);
		String n = "It is only drawn while the altar is on your screen.";
		g.text(this.font, n, this.width / 2 - this.font.width(n) / 2, this.height / 2 - 84, Theme.sub(), true);
	}
}
