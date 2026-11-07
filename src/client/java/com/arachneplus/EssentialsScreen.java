package com.arachneplus;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EssentialsScreen extends PageScreen {
	public EssentialsScreen(Screen parent) {
		super("Essentials", parent);
	}

	private static String phaseName() {
		if (!Config.showPhase) {
			return "Off";
		}
		return Config.minimalPhase ? "Minimal" : "Full";
	}

	@Override
	protected void build() {
		half(cycleButton(x -> Component.literal("Phase tracker: " + phaseName()), () -> {
			if (!Config.showPhase) {
				Config.showPhase = true;
				Config.minimalPhase = false;
			} else if (!Config.minimalPhase) {
				Config.minimalPhase = true;
			} else {
				Config.showPhase = false;
				Config.minimalPhase = false;
			}
		}));
		toggle("Brood counter", () -> Config.showBroods, v -> Config.showBroods = v);
		opens("Spawn timer...", () -> new SpawnScreen(this));
		opens("Placer...", () -> new PlacerScreen(this));
	}
}
