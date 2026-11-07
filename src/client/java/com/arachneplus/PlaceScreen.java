package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PlaceScreen extends ModScreen {
	private static final int W = 220;
	private static final int H = 20;
	private static final int GAP = 4;

	private final Screen parent;
	private int previewY;

	public PlaceScreen(Screen parent) {
		super(Component.literal("PLACE alert style"));
		this.parent = parent;
	}

	private Component colourText(String label, int argb) {
		return Component.literal(label + ": " + PlaceStyles.colorName(argb));
	}

	@Override
	protected void init() {
		int x = this.width / 2 - W / 2;
		previewY = Math.max(24, this.height / 2 - 120);
		int y = previewY + 70;

		this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			Config.placeVariant = Math.floorMod(Config.placeVariant - 1, PlaceStyles.COUNT);
			PlaceStyles.apply(PlaceStyles.variant(Config.placeVariant));
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, 30, H).build());
		this.addRenderableWidget(Button.builder(Component.literal("Variant " + (Config.placeVariant + 1) + " / " + PlaceStyles.COUNT), b -> {
			Config.placeVariant = new java.util.Random().nextInt(PlaceStyles.COUNT);
			PlaceStyles.apply(PlaceStyles.variant(Config.placeVariant));
			Config.save();
			this.rebuildWidgets();
		}).bounds(x + 34, y, W - 68, H).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			Config.placeVariant = Math.floorMod(Config.placeVariant + 1, PlaceStyles.COUNT);
			PlaceStyles.apply(PlaceStyles.variant(Config.placeVariant));
			Config.save();
			this.rebuildWidgets();
		}).bounds(x + W - 30, y, 30, H).build());
		y += H + GAP;

		this.addRenderableWidget(Button.builder(
				Component.literal(Config.placeRandom ? "Random look every alert: ON" : "Random look every alert: OFF")
						.withStyle(Config.placeRandom ? ChatFormatting.GREEN : ChatFormatting.RED), b -> {
			Config.placeRandom = !Config.placeRandom;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP + 6;

		this.addRenderableWidget(Button.builder(Component.literal("Font: " + PlaceStyles.FONT_NAMES[Config.placeFont]), b -> {
			Config.placeFont = (Config.placeFont + 1) % 3;
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(colourText("Colour 1", Config.placeColorA), b -> {
			Config.placeColorA = PlaceStyles.nextColor(Config.placeColorA, false);
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(colourText("Colour 2 (flashes to)", Config.placeColorB), b -> {
			Config.placeColorB = PlaceStyles.nextColor(Config.placeColorB, false);
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(colourText("Shadow", Config.placeShadow), b -> {
			Config.placeShadow = PlaceStyles.nextColor(Config.placeShadow, true);
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Exclamation marks: " + Config.placeBangs), b -> {
			Config.placeBangs = (Config.placeBangs + 1) % (PlaceStyles.MAX_BANGS + 1);
			Config.save();
			this.rebuildWidgets();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		this.addRenderableWidget(Button.builder(Component.literal("Size: " + Config.placeSize + "%"), b -> {
			int next = PlaceStyles.SIZES[0];
			for (int s : PlaceStyles.SIZES) {
				if (s > Config.placeSize) {
					next = s;
					break;
				}
			}
			Config.placeSize = next;
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
		String title = "PLACE alert style";
		Logo.draw(g, this.font, this.width / 2f, previewY - 20, Logo.SMALL);
		g.text(this.font, title, this.width / 2 - this.font.width(title) / 2, previewY - 2, Theme.gold(), true);

		PlaceStyles.Style st = Config.placeStyle();
		float s = Math.min(1f, (this.width - 20f) / Math.max(1f, ArachneHud.previewWidth(st, 1f)));
		s = Math.min(s, 0.9f * st.size() / 100f);
		float w = ArachneHud.previewWidth(st, s);
		ArachneHud.drawPlacePreview(g, st, this.width / 2f - w / 2f, previewY + 14, s);
	}
}
