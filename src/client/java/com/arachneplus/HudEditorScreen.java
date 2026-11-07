package com.arachneplus;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
	private static final String[] NAMES = {
			"Phase display", "Boss timer", "Average timer", "Spawn timer", "CPS counter", "Arachne kills", "Profit estimate", "PLACE alert"
	};

	private static final int BOTTOM = 90;

	private final Screen parent;
	private int dragging = -1;
	private int selected = -1;
	private int dragDX;
	private int dragDY;
	private boolean snapped = false;

	private Button smaller;
	private Button bigger;
	private Button backgroundBtn;
	private Button alignBtn;
	private Button colourBtn;
	private Button spacingBtn;
	private Button boldBtn;
	private Button italicBtn;
	private Button strikeBtn;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Edit HUD layout"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int row1 = this.height - 54;
		int row2 = this.height - 28;

		int row0 = this.height - 80;
		int sx = cx - 193;
		colourBtn = this.addRenderableWidget(Button.builder(Component.literal("Colour"), b -> {
			if (selected >= 0) {
				int i = 0;
				for (int k = 0; k < Config.COLOR_VALUES.length; k++) {
					if (Config.COLOR_VALUES[k] == Config.textColor[selected]) {
						i = k;
					}
				}
				Config.textColor[selected] = Config.COLOR_VALUES[(i + 1) % Config.COLOR_VALUES.length];
				Config.save();
			}
		}).bounds(sx, row0, 110, 20).build());
		spacingBtn = this.addRenderableWidget(Button.builder(Component.literal("Spacing"), b -> {
			if (selected >= 0) {
				Config.letterSpacing[selected] = (Config.letterSpacing[selected] + 1) % 5;
				Config.save();
			}
		}).bounds(sx + 114, row0, 90, 20).build());
		boldBtn = this.addRenderableWidget(Button.builder(Component.literal("Bold"), b -> {
			if (selected >= 0) {
				Config.bold[selected] = !Config.bold[selected];
				Config.save();
			}
		}).bounds(sx + 208, row0, 52, 20).build());
		italicBtn = this.addRenderableWidget(Button.builder(Component.literal("Italic"), b -> {
			if (selected >= 0) {
				Config.italic[selected] = !Config.italic[selected];
				Config.save();
			}
		}).bounds(sx + 264, row0, 52, 20).build());
		strikeBtn = this.addRenderableWidget(Button.builder(Component.literal("Strike"), b -> {
			if (selected >= 0) {
				Config.strike[selected] = !Config.strike[selected];
				Config.save();
			}
		}).bounds(sx + 320, row0, 66, 20).build());

		smaller = this.addRenderableWidget(Button.builder(Component.literal("-"), b -> changeScale(-0.1f))
				.bounds(cx - 160, row1, 30, 20).build());
		bigger = this.addRenderableWidget(Button.builder(Component.literal("+"), b -> changeScale(0.1f))
				.bounds(cx - 124, row1, 30, 20).build());
		backgroundBtn = this.addRenderableWidget(Button.builder(Component.literal("Background"), b -> {
			if (selected >= 0) {
				Config.background[selected] = !Config.background[selected];
				Config.save();
			}
		}).bounds(cx - 54, row1, 108, 20).build());
		alignBtn = this.addRenderableWidget(Button.builder(Component.literal("Align"), b -> {
			if (selected >= 0) {
				int[] bb = boundsOf(selected);
				int mid = (bb[0] + bb[2]) / 2;
				int pad = Math.round(ArachneHud.PAD * Config.scale[selected]);
				Config.centered[selected] = !Config.centered[selected];

				Config.posX[selected] = Config.centered[selected] ? mid : bb[0] + pad;
				Config.save();
			}
		}).bounds(cx + 58, row1, 102, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Reset layout"), b -> {
			Config.resetPositions();
			Config.save();
		}).bounds(cx - 160, row2, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Reset stats"), b -> {
			Config.resetStats();
			Config.save();
		}).bounds(cx - 50, row2, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(cx + 60, row2, 100, 20).build());

		this.addRenderableWidget(Button.builder(powerText(), b -> {
			if (Config.enabled) {
				Config.disableEverything();
			} else {
				Config.enableEverything();
			}
			Config.save();
			b.setMessage(powerText());
		}).bounds(cx - 80, 6, 160, 20).build());
	}

	private static Component powerText() {
		boolean on = Config.enabled;
		return Component.literal(on ? "Turn mod OFF" : "Turn mod ON")
				.withStyle(on ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN,
						net.minecraft.ChatFormatting.BOLD);
	}

	private void changeScale(float delta) {
		if (selected < 0) {
			return;
		}
		float s = Math.round((Config.scale[selected] + delta) * 10f) / 10f;
		Config.scale[selected] = Math.max(Config.MIN_SCALE, Math.min(Config.MAX_SCALE, s));
		Config.save();
	}

	private static String colourLabel(int argb) {
		for (int k = 0; k < Config.COLOR_VALUES.length; k++) {
			if (Config.COLOR_VALUES[k] == argb) {
				return Config.COLOR_NAMES[k];
			}
		}
		return "Default";
	}

	private int[] boundsOf(int panel) {
		return ArachneHud.bounds(this.font, ArachneHud.lines(panel, true), panel);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0 || event.y() >= this.height - BOTTOM) {
			return false;
		}
		double mx = event.x();
		double my = event.y();
		dragging = -1;
		for (int i = Config.PANEL_COUNT - 1; i >= 0; i--) {
			int[] b = boundsOf(i);
			if (mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]) {
				dragging = i;
				selected = i;
				dragDX = (int) Math.round(mx) - Config.posX[i];
				dragDY = (int) Math.round(my) - Config.posY[i];
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging >= 0 && event.button() == 0) {
			int i = dragging;
			int x = (int) Math.round(event.x()) - dragDX;
			int y = (int) Math.round(event.y()) - dragDY;
			Config.posX[i] = x;
			Config.posY[i] = y;
			int[] b = boundsOf(i);
			int boxW = b[2] - b[0];
			int boxH = b[3] - b[1];

			snapped = Math.abs((b[0] + b[2]) / 2 - this.width / 2) <= 5;
			if (snapped) {
				x += this.width / 2 - (b[0] + b[2]) / 2;
			}
			int padPx = Math.round(ArachneHud.PAD * Config.scale[i]);
			int minX = Config.centered[i] ? boxW / 2 : padPx;
			int maxX = Config.centered[i] ? this.width - boxW / 2 : this.width - boxW + padPx;
			Config.posX[i] = Math.max(minX, Math.min(maxX, x));
			Config.posY[i] = Math.max(padPx, Math.min(this.height - BOTTOM - boxH + padPx, y));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging >= 0) {
			dragging = -1;
			snapped = false;
			Config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		boolean has = selected >= 0;
		smaller.active = has && Config.scale[selected] > Config.MIN_SCALE + 0.01f;
		bigger.active = has && Config.scale[selected] < Config.MAX_SCALE - 0.01f;
		backgroundBtn.active = has;
		alignBtn.active = has;
		colourBtn.active = has;
		spacingBtn.active = has;
		boldBtn.active = has;
		italicBtn.active = has;
		strikeBtn.active = has;
		colourBtn.setMessage(Component.literal(has ? "Colour: " + colourLabel(Config.textColor[selected]) : "Colour"));
		spacingBtn.setMessage(Component.literal(has ? "Spacing: " + Config.letterSpacing[selected] : "Spacing"));
		boldBtn.setMessage(Component.literal(has ? "Bold: " + (Config.bold[selected] ? "ON" : "OFF") : "Bold"));
		italicBtn.setMessage(Component.literal(has ? "Italic: " + (Config.italic[selected] ? "ON" : "OFF") : "Italic"));
		strikeBtn.setMessage(Component.literal(has ? "Strike: " + (Config.strike[selected] ? "ON" : "OFF") : "Strike"));
		backgroundBtn.setMessage(Component.literal(has
				? "Background: " + (Config.background[selected] ? "ON" : "OFF")
				: "Background"));
		alignBtn.setMessage(Component.literal(has
				? "Align: " + (Config.centered[selected] ? "Centre" : "Left")
				: "Align"));

		String help = "Drag a box to move it. Click a box to select it, then use - / + to resize it.";
		g.text(this.font, help, this.width / 2 - this.font.width(help) / 2, 8, 0xFFFFFF55, true);
		if (has) {
			String size = NAMES[selected] + " - size " + Math.round(Config.scale[selected] * 100) + "%";
			g.text(this.font, size, this.width / 2 - this.font.width(size) / 2, this.height - 94, 0xFFFFFFFF, true);
		} else {
			String hint = "Select a box to change its size, background and text style";
			g.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height - 94, 0xFFAAAAAA, true);
		}

		if (dragging >= 0 && snapped) {
			g.fill(this.width / 2, 0, this.width / 2 + 1, this.height - BOTTOM, 0x8855FF55);
		}
		for (int i = 0; i < Config.PANEL_COUNT; i++) {
			List<ArachneHud.Line> lines = ArachneHud.lines(i, true);
			ArachneHud.drawBox(g, this.font, i, lines);

			int[] b = ArachneHud.bounds(this.font, lines, i);
			boolean hover = mouseX >= b[0] && mouseX <= b[2] && mouseY >= b[1] && mouseY <= b[3];
			int c = selected == i ? 0xFFFFFF55 : hover ? 0xFFFFFFFF : 0xFF777777;
			g.fill(b[0], b[1], b[2], b[1] + 1, c);
			g.fill(b[0], b[3] - 1, b[2], b[3], c);
			g.fill(b[0], b[1], b[0] + 1, b[3], c);
			g.fill(b[2] - 1, b[1], b[2], b[3], c);
		}
	}

	@Override
	public void onClose() {
		Config.save();
		this.minecraft.setScreenAndShow(parent);
	}
}
