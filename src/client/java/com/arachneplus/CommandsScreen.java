package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CommandsScreen extends ModScreen {
	private final Screen parent;
	private final int cat;

	public CommandsScreen(Screen parent) {
		this(parent, 0);
	}

	public CommandsScreen(Screen parent, int cat) {
		super(Component.literal("Commands"));
		this.parent = parent;
		this.cat = cat;
	}

	@Override
	protected void init() {
		int n = CommandList.CATS.length;
		int w = 82;
		int gap = 3;
		int x0 = this.width / 2 - (n * w + (n - 1) * gap) / 2;
		for (int i = 0; i < n; i++) {
			final int idx = i;
			Component label = i == cat
					? Component.literal(CommandList.CATS[i]).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
					: Component.literal(CommandList.CATS[i]);
			this.addRenderableWidget(Button.builder(label, b -> this.minecraft.setScreenAndShow(new CommandsScreen(parent, idx)))
					.bounds(x0 + i * (w + gap), 44, w, 20).build());
		}
		int y = 74;
		for (String[] c : CommandList.ALL) {
			if (Integer.parseInt(c[2]) != cat) {
				continue;
			}
			final String first = c[0].split("\\s+")[0];
			this.addRenderableWidget(Button.builder(Component.literal(c[0]), b -> this.minecraft.keyboardHandler.setClipboard(first))
					.bounds(this.width / 2 - 150, y, 300, 20)
					.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(c[1] + "\n(click to copy " + first + ")")))
					.build());
			y += 24;
		}
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreenAndShow(parent))
				.bounds(this.width / 2 - 40, this.height - 30, 80, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		Logo.draw(g, this.font, cx, 4, Logo.SMALL);
		g.text(this.font, "Commands", cx - this.font.width("Commands") / 2, 19, Theme.gold(), true);
		String sub = "Hover a command to see what it does. Any also works starting with a+ instead of ap";
		g.text(this.font, sub, cx - this.font.width(sub) / 2, 31, Theme.sub(), true);
	}
}
