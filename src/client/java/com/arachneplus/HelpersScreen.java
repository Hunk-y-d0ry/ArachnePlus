package com.arachneplus;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HelpersScreen extends PageScreen {
	public HelpersScreen(Screen parent) {
		super("Other helpers", parent);
	}

	private static Component shrinkText() {
		return Component.literal("Other players' size: " + sizeName(Config.playerShrink));
	}

	@Override
	protected void build() {
		toggle("Line to boss", () -> Config.tracer, v -> Config.tracer = v);
		opens("Line settings...", () -> new LineScreen(this));
		toggle("Arachne glow", () -> Config.glow, v -> Config.glow = v);
		toggle("Purple runic glow", () -> Config.runicGlow, v -> Config.runicGlow = v);
		toggle("Silence Arachne chat", () -> Config.silenceArachne, v -> Config.silenceArachne = v);
		toggle("Hide damage numbers", () -> Config.hideDamageNumbers, v -> Config.hideDamageNumbers = v);
		toggle("Hide mob names", () -> Config.hideMobNames, v -> Config.hideMobNames = v);
		toggle("Minimal brood tags (2 / 1)", () -> Config.minimalBroodTags, v -> Config.minimalBroodTags = v);
		toggle("Hide player names", () -> Config.hidePlayerNames, v -> Config.hidePlayerNames = v);
		toggle("Hide all particles", () -> Config.hideParticles, v -> Config.hideParticles = v);

		fullWidget(new net.minecraft.client.gui.components.AbstractSliderButton(0, 0, W * 2 + GAP, H,
				shrinkText(), (Config.playerShrink - 1) / 9.0) {
			@Override
			protected void updateMessage() {
				this.setMessage(Component.literal("Other players' size: " + sizeName(1 + (int) Math.round(this.value * 9))));
			}

			@Override
			protected void applyValue() {
				Config.playerShrink = 1 + (int) Math.round(this.value * 9);
				Config.save();
			}
		});
	}

	private static String sizeName(int shrink) {
		return shrink <= 1 ? "normal" : "1/" + shrink + " (" + shrink + "x smaller)";
	}
}
