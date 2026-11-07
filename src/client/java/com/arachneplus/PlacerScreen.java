package com.arachneplus;

import net.minecraft.client.gui.screens.Screen;

public class PlacerScreen extends PageScreen {
	public PlacerScreen(Screen parent) {
		super("Placer", parent);
	}

	@Override
	protected void build() {
		toggle("Placer", () -> Config.placer, v -> Config.placer = v);
		opens("PLACE alert style...", () -> new PlaceScreen(this));
	}
}
