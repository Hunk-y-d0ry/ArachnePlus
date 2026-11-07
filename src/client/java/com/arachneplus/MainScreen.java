package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MainScreen extends ModScreen {
	private static final int H = 20;
	private static final int GAP = 4;
	private static final int BIG_W = 220;
	private static final int SMALL_W = 108;

	private boolean creditsOpen = false;
	private int titleY, blurbY;

	public MainScreen() {
		super(Component.literal("Arachne Plus"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		titleY = Math.max(6, this.height / 2 - 138);
		blurbY = titleY + 40;
		int y = blurbY + 18;

		this.addRenderableWidget(Button.builder(
				Component.literal("Config").withStyle(ChatFormatting.BOLD),
				b -> this.minecraft.setScreenAndShow(new ConfigScreen()))
				.bounds(cx - BIG_W / 2, y, BIG_W, 28).build());
		y += 28 + GAP;
		this.addRenderableWidget(Button.builder(
				Component.literal("Achievements  " + Achievements.unlockedCount() + " / " + Achievements.total())
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				b -> this.minecraft.setScreenAndShow(new AchievementsScreen(this)))
				.bounds(cx - BIG_W / 2, y, BIG_W, 28).build());
		y += 28 + GAP;
		this.addRenderableWidget(Button.builder(
				Component.literal("GUI:  ").withStyle(ChatFormatting.BOLD)
						.append(Component.literal(Config.darkGui ? "DARK" : "LIGHT")
								.withStyle(Config.darkGui ? ChatFormatting.BLACK : ChatFormatting.WHITE, ChatFormatting.BOLD))
						.append(Component.literal("  (click to switch)").withStyle(ChatFormatting.GRAY)),
				b -> {
					Config.darkGui = !Config.darkGui;
					Config.save();
					this.rebuildWidgets();
				})
				.bounds(cx - BIG_W / 2, y, BIG_W, H).build());
		y += H + GAP;

		int lx = cx - SMALL_W - GAP / 2;
		int rx = cx + GAP / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Config GUI"),
				b -> this.minecraft.setScreenAndShow(new HudEditorScreen(this)))
				.bounds(lx, y, SMALL_W, H).build());
		this.addRenderableWidget(Button.builder(Component.literal("API key"),
				b -> this.minecraft.setScreenAndShow(new ApiKeyScreen(this)))
				.bounds(rx, y, SMALL_W, H).build());
		y += H + GAP;

		Button discord = Button.builder(Component.literal("Discord"), b ->
				ConfirmLinkScreen.confirmLinkNow(this, Config.discordUrl))
				.bounds(lx, y, SMALL_W, H).build();
		discord.active = !Config.discordUrl.isEmpty();
		this.addRenderableWidget(discord);
		this.addRenderableWidget(Button.builder(Component.literal(creditsOpen ? "Credits (hide)" : "Credits"), b -> {
			creditsOpen = !creditsOpen;
			this.rebuildWidgets();
		}).bounds(rx, y, SMALL_W, H).build());
		y += H + GAP;

		if (creditsOpen) {
			y += 34;
		}
		this.addRenderableWidget(Button.builder(Component.literal("Commands"),
				b -> this.minecraft.setScreenAndShow(new CommandsScreen(this)))
				.bounds(lx, y, SMALL_W, H).build());
		this.addRenderableWidget(Button.builder(Component.literal("Party Commands"),
				b -> this.minecraft.setScreenAndShow(new PartyScreen(this)))
				.bounds(rx, y, SMALL_W, H).build());
		y += H + GAP;
		boolean on = Config.enabled;
		this.addRenderableWidget(Button.builder(
				Component.literal(on ? "Disable" : "Enable")
						.withStyle(on ? ChatFormatting.RED : ChatFormatting.GREEN, ChatFormatting.BOLD),
				b -> {
					if (Config.enabled) {
						Config.disableEverything();
					} else {
						Config.enableEverything();
					}
					this.rebuildWidgets();
				}).bounds(cx - BIG_W / 2, y, BIG_W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
				.bounds(cx - 40, y, 80, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;

		float s = 4f;
		Component part1 = Component.literal("Arachne ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		Component part2 = Component.literal("Plus").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
		int w1 = this.font.width(part1) + 2;
		int w2 = this.font.width(part2);
		float total = (w1 + w2) * s;
		g.pose().pushMatrix();
		g.pose().translate(cx - total / 2f, (float) titleY);
		g.pose().scale(s, s);
		g.text(this.font, part1, 0, 0, 0xFFFF2A2A, true);
		g.text(this.font, part2, w1, 0, 0xFFFFD83A, true);
		g.pose().popMatrix();
		String blurb = "For Arachne Lovers, By Arachne Lovers";
		g.text(this.font, blurb, cx - this.font.width(blurb) / 2, blurbY, Theme.sub(), true);
		if (creditsOpen) {
			int y = blurbY + 18 + (28 + GAP) * 2 + (H + GAP) * 3 + 4;
			g.text(this.font, "Lead Developer: Hunkydory_", cx - this.font.width("Lead Developer: Hunkydory_") / 2, y, Theme.gold(), true);
			g.text(this.font, "Contributor: Ata2030", cx - this.font.width("Contributor: Ata2030") / 2, y + 14, Theme.text(), true);
		}
	}
}
