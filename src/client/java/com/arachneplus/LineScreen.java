package com.arachneplus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LineScreen extends ModScreen {
	private static final String[] STYLES = {"Solid", "Dashed", "Dotted"};
	private static final int W = 220;
	private static final int H = 20;
	private static final int GAP = 6;

	private final Screen parent;
	private Button thickness;
	private Button colour;
	private Button style;
	private Button dot;

	public LineScreen(Screen parent) {
		super(Component.literal("Line to Arachne"));
		this.parent = parent;
	}

	private static int colourIndex(int argb, boolean allowNone) {
		for (int i = allowNone ? 0 : 1; i < Config.COLOR_VALUES.length; i++) {
			if (Config.COLOR_VALUES[i] == argb) {
				return i;
			}
		}
		return allowNone ? 0 : 2;
	}

	private static String colourName(int argb, boolean dot) {
		if (dot && argb == 0) {
			return "None (no dot)";
		}
		int i = colourIndex(argb, false);
		return Config.COLOR_NAMES[i];
	}

	@Override
	protected void init() {
		int x = this.width / 2 - W / 2;
		int y = this.height / 2 - 70;
		thickness = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			Config.lineThickness = Config.lineThickness % 8 + 1;
			Config.save();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		colour = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			int i = colourIndex(Config.lineColor, false);
			i = i + 1 >= Config.COLOR_VALUES.length ? 1 : i + 1;
			Config.lineColor = Config.COLOR_VALUES[i];
			Config.save();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		style = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			Config.lineStyle = (Config.lineStyle + 1) % 3;
			Config.save();
		}).bounds(x, y, W, H).build());
		y += H + GAP;
		dot = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			if (Config.dotColor == 0) {
				Config.dotColor = Config.COLOR_VALUES[1];
			} else {
				int i = colourIndex(Config.dotColor, false) + 1;
				Config.dotColor = i >= Config.COLOR_VALUES.length ? 0 : Config.COLOR_VALUES[i];
			}
			Config.save();
		}).bounds(x, y, W, H).build());
		y += H + GAP + 6;
		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(x, y, W, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		thickness.setMessage(Component.literal("Thickness: " + Config.lineThickness));
		colour.setMessage(Component.literal("Line colour: " + colourName(Config.lineColor, false)));
		style.setMessage(Component.literal("Line style: " + STYLES[Config.lineStyle]));
		dot.setMessage(Component.literal("End dot colour: " + colourName(Config.dotColor, true)));
		super.extractRenderState(g, mouseX, mouseY, delta);

		String title = this.title.getString();
		Logo.draw(g, this.font, this.width / 2f, this.height / 2 - 114, Logo.SMALL);
		g.text(this.font, title, this.width / 2 - this.font.width(title) / 2, this.height / 2 - 96, Theme.gold(), true);

		int px = this.width / 2 - 90;
		int py = this.height / 2 + 86;
		int len = 180;
		int t = Config.lineThickness;
		int lo = t / 2;
		int hi = t - lo;
		int period = Config.lineStyle == 1 ? 14 + t * 2 : Math.max(4, t * 3);
		int onLen = Config.lineStyle == 1 ? 8 + t : t;
		for (int i = 0; i <= len; i++) {
			if (Config.lineStyle != 0 && i % period >= onLen) {
				continue;
			}
			g.fill(px + i - lo, py - lo, px + i + hi, py + hi, Config.lineColor);
		}
		if (Config.dotColor != 0) {
			int half = 3 + t;
			g.fill(px + len - half, py - half, px + len + half, py + half, Config.dotColor);
		}
	}

	@Override
	public void onClose() {
		Config.save();
		this.minecraft.setScreenAndShow(parent);
	}
}
