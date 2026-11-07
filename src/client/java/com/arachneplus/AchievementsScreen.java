package com.arachneplus;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class AchievementsScreen extends ModScreen {
	private static final int COLS = 3;
	private static final int GAP = 6;
	private static final int TILE_H = 58;
	private static final int TOP_H = 28;
	private static final int BOTTOM_H = 38;
	private final Screen parent;
	private double scroll = 0;

	public AchievementsScreen(Screen parent) {
		super(Component.literal("Achievements"));
		this.parent = parent;
	}

	private static List<Achievements.Def> items() {
		List<Achievements.Def> list = new ArrayList<>();
		for (Achievements.Def d : Achievements.ALL) {
			if (!d.id().equals("completionist")) {
				list.add(d);
			}
		}
		return list;
	}

	private int margin() {
		return 8;
	}

	private int listTop() {
		return margin() + TOP_H + GAP;
	}

	private int listBottom() {
		return this.height - margin() - BOTTOM_H - GAP - 24;
	}

	private int contentHeight() {
		int rows = (items().size() + COLS - 1) / COLS;
		return rows * (TILE_H + GAP) - GAP;
	}

	private double maxScroll() {
		return Math.max(0, contentHeight() - (listBottom() - listTop()));
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreenAndShow(parent))
				.bounds(margin() + 4, margin() + 4, 50, 20).build());
		this.addRenderableWidget(new AbstractSliderButton(margin(), this.height - margin() - BOTTOM_H - GAP - 20, 170, 20,
				celebrationText(Config.celebration), Config.celebration / 100.0) {
			@Override
			protected void updateMessage() {
				this.setMessage(celebrationText((int) Math.round(this.value * 100)));
			}

			@Override
			protected void applyValue() {
				Config.celebration = (int) Math.round(this.value * 100);
				Config.save();
			}
		});
	}

	private static Component celebrationText(int pct) {
		return Component.literal("Size of Celebration: " + pct + "%");
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 20));
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int m = margin();
		int left = m;
		int right = this.width - m;
		int fullW = right - left;

		g.fill(left, m, right, m + TOP_H, Theme.box());
		g.fill(left, m + TOP_H - 2, right, m + TOP_H, 0xFFFFAA00);
		String title = "Achievements   " + Achievements.unlockedCount() + " / " + Achievements.total();
		g.text(this.font, title, this.width / 2 - this.font.width(title) / 2, m + 10, Theme.gold(), true);
		Logo.draw(g, this.font, right - Logo.width(this.font, Logo.SMALL) / 2f - 8, m + 8, Logo.SMALL);

		int top = listTop();
		int bottom = listBottom();
		int tileW = (fullW - GAP * (COLS - 1)) / COLS;
		g.enableScissor(left, top, right, bottom);
		List<Achievements.Def> list = items();
		for (int i = 0; i < list.size(); i++) {
			int col = i % COLS;
			int row = i / COLS;
			int x = left + col * (tileW + GAP);
			int y = top + row * (TILE_H + GAP) - (int) scroll;
			if (y + TILE_H < top || y > bottom) {
				continue;
			}
			Achievements.Def d = list.get(i);
			boolean got = Achievements.has(d.id());
			g.fill(x, y, x + tileW, y + TILE_H, got ? 0xC0403000 : Theme.box());
			g.fill(x, y, x + tileW, y + 2, got ? 0xFFFFAA00 : 0xFF555555);
			String name = (got ? "★ " : "☆ ") + d.name();
			g.text(this.font, this.font.plainSubstrByWidth(name, tileW - 8), x + 5, y + 6, got ? 0xFFFFD83A : Theme.dim(), true);
			List<FormattedCharSequence> lines = this.font.split(Component.literal(d.desc()), tileW - 10);
			for (int k = 0; k < Math.min(3, lines.size()); k++) {
				g.text(this.font, lines.get(k), x + 5, y + 19 + k * 10, got ? 0xFFFFFFFF : Theme.dimmer(), true);
			}
		}
		g.disableScissor();

		if (maxScroll() > 0) {
			int trackH = bottom - top;
			int barH = Math.max(16, (int) (trackH * (trackH / (double) contentHeight())));
			int barY = top + (int) ((trackH - barH) * (scroll / maxScroll()));
			g.fill(right - 3, top, right, bottom, 0x60000000);
			g.fill(right - 3, barY, right, barY + barH, 0xFFFFAA00);
		}

		boolean done = Achievements.has("completionist");
		int by = this.height - m - BOTTOM_H;
		g.fill(left, by, right, by + BOTTOM_H, done ? 0xC0604800 : Theme.box());
		g.fill(left, by, right, by + 2, done ? 0xFFFFD83A : 0xFF555555);
		g.fill(left, by + BOTTOM_H - 2, right, by + BOTTOM_H, done ? 0xFFFFD83A : 0xFF555555);
		String cname = (done ? "★ " : "☆ ") + "COMPLETIONIST";
		g.text(this.font, cname, this.width / 2 - this.font.width(cname) / 2, by + 8, done ? 0xFFFFD83A : Theme.dim(), true);
		String cdesc = "Unlock all " + Achievements.total() + " achievements (" + Achievements.unlockedCount() + " / " + Achievements.total() + ")";
		g.text(this.font, cdesc, this.width / 2 - this.font.width(cdesc) / 2, by + 21, done ? 0xFFFFFFFF : Theme.dimmer(), true);
	}
}
