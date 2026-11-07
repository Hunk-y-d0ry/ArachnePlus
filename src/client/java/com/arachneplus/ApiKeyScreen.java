package com.arachneplus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ApiKeyScreen extends ModScreen {
	private final Screen parent;

	public ApiKeyScreen(Screen parent) {
		super(Component.literal("API key"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreenAndShow(parent))
				.bounds(this.width / 2 - 40, this.height / 2 + 116, 80, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		String[] lines = {
				"You do NOT need an API key. Arachne Plus reads your kills,",
				"your profile and other players through the Arachne Plus website,",
				"which has its own Hypixel key (kills re-check every 10 seconds after a boss).",
				"",
				"A personal key is optional and gives you FASTER kill updates:",
				"it talks to Hypixel directly (re-checks every 5 seconds after a boss,",
				"every 15 seconds in the sanctuary) and skips the website.",
				"If it ever fails, the website is used as the backup.",
				"",
				"1. Get a key at developer.hypixel.net",
				"   (stuck? type /apkeyhelp in chat for something to copy into Google)",
				"2. In chat, type:  /apapikey <your key>",
				"",
				"Or you can type your kills yourself:",
				"/apbestiary <number of Arachne kills>"
		};
		int y = this.height / 2 - 84;
		Logo.draw(g, this.font, this.width / 2f, y - 36, Logo.SMALL);
		g.text(this.font, "Hypixel API key", this.width / 2 - this.font.width("Hypixel API key") / 2, y - 18, Theme.gold(), true);
		for (String l : lines) {
			g.text(this.font, l, this.width / 2 - this.font.width(l) / 2, y, Theme.text(), true);
			y += 12;
		}
	}
}
