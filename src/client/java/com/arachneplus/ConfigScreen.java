package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends ModScreen {
	private static final int W = 150;
	private static final int H = 20;
	private static final int GAP = 4;
	private static final int CORNER_W = 120;

	private boolean creditsOpen = false;
	private int top;

	public ConfigScreen() {
		super(Component.literal("Arachne Plus Settings"));
	}

	private Button open(String label, java.util.function.Supplier<Screen> s, int x, int y, int w, int h) {
		return Button.builder(Component.literal(label), b -> this.minecraft.setScreenAndShow(s.get()))
				.bounds(x, y, w, h).build();
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		top = Math.max(26, this.height / 2 - 100);
		int y = top + 18;

		this.addRenderableWidget(Button.builder(Component.literal("Essentials").withStyle(ChatFormatting.BOLD),
				b -> this.minecraft.setScreenAndShow(new EssentialsScreen(this)))
				.bounds(cx - 110, y, 220, 28).build());
		y += 28 + GAP;

		int third = (W * 2 + GAP - 2 * GAP) / 3 + 14;
		int total = third * 3 + GAP * 2;
		int x0 = cx - total / 2;
		this.addRenderableWidget(open("Stats", () -> new StatsScreen(this), x0, y, third, H + 4));
		this.addRenderableWidget(open("Other helpers", () -> new HelpersScreen(this), x0 + third + GAP, y, third, H + 4));
		this.addRenderableWidget(open("Party Commands", () -> new PartyScreen(this), x0 + (third + GAP) * 2, y, third, H + 4));
		y += H + 4 + GAP + 6;

		this.addRenderableWidget(toggleButton("Mod enabled", () -> Config.enabled, v -> Config.enabled = v, cx - W - GAP / 2, y));
		this.addRenderableWidget(toggleButton("Only in the Sanctuary", () -> Config.sanctuaryOnly,
				v -> Config.sanctuaryOnly = v, cx + GAP / 2, y));
		y += H + GAP;
		this.addRenderableWidget(open("Edit HUD layout...", () -> new HudEditorScreen(this), cx - W - GAP / 2, y, W, H));
		this.addRenderableWidget(Button.builder(
				Component.literal("RESET SESSION").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), b -> {
					ArachneTracker.resetSession();
					ArachnePlusClient.say("Session reset (profit and kill timer)");
				}).bounds(cx + GAP / 2, y, W, H).build());
		y += H + GAP + 6;
		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(cx - 40, y, 80, H).build());
		addCornerButtons();
	}

	private Button toggleButton(String label, java.util.function.BooleanSupplier get,
			java.util.function.Consumer<Boolean> set, int x, int y) {
		java.util.function.Function<Boolean, Component> text = on -> Component.literal(label + ": ")
				.append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
		return Button.builder(text.apply(get.getAsBoolean()), b -> {
			boolean now = !get.getAsBoolean();
			set.accept(now);
			Config.save();
			b.setMessage(text.apply(now));
		}).bounds(x, y, W, H).build();
	}

	private void addCornerButtons() {
		int x = 4;
		int y = this.height - 24;
		this.addRenderableWidget(Button.builder(Component.literal(creditsOpen ? "Credits (hide)" : "Credits"), b -> {
			creditsOpen = !creditsOpen;
			this.rebuildWidgets();
		}).bounds(x, y, CORNER_W, H).build());
		y -= H + GAP;
		Button discord = Button.builder(Component.literal(Config.discordUrl.isEmpty() ? "Discord (coming soon)" : "Join our Discord"),
				b -> {
					if (!Config.discordUrl.isEmpty()) {
						net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(this, Config.discordUrl);
					}
				}).bounds(x, y, CORNER_W, H).build();
		discord.active = !Config.discordUrl.isEmpty();
		this.addRenderableWidget(discord);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		String title = this.title.getString();
		Logo.draw(g, this.font, this.width / 2f, top - 20, Logo.SMALL);
		g.text(this.font, title, this.width / 2 - this.font.width(title) / 2, top, Theme.gold(), true);
		if (creditsOpen) {
			int y = this.height - 24 - (H + GAP) - 38;
			g.fill(2, y - 4, 2 + 190, y + 32, Theme.box());
			g.text(this.font, "Lead Developer: Hunkydory_", 6, y, Theme.gold(), true);
			g.text(this.font, "Contributor: Ata2030", 6, y + 14, Theme.text(), true);
		}
	}
}
