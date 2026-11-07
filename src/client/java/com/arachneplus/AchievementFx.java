package com.arachneplus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class AchievementFx {
	private AchievementFx() { }

	private record Toast(String name, String desc, boolean big) { }

	private static final ArrayDeque<Toast> QUEUE = new ArrayDeque<>();
	private static Toast current;
	private static long shownAt;

	private static final class Piece {
		float x, y, vx, vy;
		int color;
		int size;
	}

	private static final List<Piece> CONFETTI = new ArrayList<>();
	private static final Random RNG = new Random();
	private static long confettiUntil;
	private static boolean bigConfetti;
	private static long lastFrame;
	private static final int[] COLORS = {0xFFFF3030, 0xFFFFD020, 0xFF40E060, 0xFF30B0FF, 0xFFFF60E0, 0xFFFFFFFF, 0xFFFF9020};

	static void show(String name, String desc, boolean big) {
		QUEUE.add(new Toast(name, desc, big));
		long until = System.currentTimeMillis() + (big ? 9000 : 4500);
		confettiUntil = Math.max(confettiUntil, until);
		if (big) {
			bigConfetti = true;
		}
	}

	static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		var font = mc.font;

		if (current == null && !QUEUE.isEmpty()) {
			current = QUEUE.poll();
			shownAt = now;
		}
		if (current != null) {
			long age = now - shownAt;
			if (age > 5500) {
				current = null;
			} else {
				boolean flash = (age / 200) % 2 == 0;
				int border = flash ? 0xFFFFD020 : 0xFFFF3030;
				String title = current.big() ? "COMPLETIONIST!" : "Achievement Unlocked!";
				int bw = Math.max(font.width(current.desc()), Math.max(font.width(current.name()), font.width(title))) + 24;
				bw = Math.min(bw, (w - 10) / 2);
				int bh = 46;

				float px = Config.posX[Config.PLACE] < 0 ? w / 2f : Config.posX[Config.PLACE];
				float py = Config.posY[Config.PLACE] < 0 ? h / 4f : Config.posY[Config.PLACE];
				g.pose().pushMatrix();
				g.pose().translate(px - bw, py);
				g.pose().scale(2f, 2f);
				int x = 0;
				int y = 0;
				g.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, border);
				g.fill(x, y, x + bw, y + bh, flash ? 0xF0302000 : 0xF0200000);
				String desc = font.plainSubstrByWidth(current.desc(), bw - 6);
				g.text(font, title, bw / 2 - font.width(title) / 2, y + 6, flash ? 0xFFFFD020 : 0xFFFF5555, true);
				g.text(font, current.name(), bw / 2 - font.width(current.name()) / 2, y + 19, 0xFFFFFFFF, true);
				g.text(font, desc, bw / 2 - font.width(desc) / 2, y + 32, 0xFFAAAAAA, true);
				g.pose().popMatrix();
			}
		}

		float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
		lastFrame = now;
		if (now < confettiUntil) {
			int rate = (int) Math.round((bigConfetti ? 40 : 18) * Config.celebration / 75.0);
			for (int i = 0; i < rate; i++) {
				Piece p = new Piece();
				p.x = RNG.nextFloat() * w;
				p.y = -10 - RNG.nextFloat() * 30;
				p.vx = (RNG.nextFloat() - 0.5f) * 90;
				p.vy = 60 + RNG.nextFloat() * 140;
				p.color = COLORS[RNG.nextInt(COLORS.length)];
				p.size = 2 + RNG.nextInt(4);
				CONFETTI.add(p);
			}
		}
		if (now >= confettiUntil) {
			bigConfetti = false;
		}
		for (int i = CONFETTI.size() - 1; i >= 0; i--) {
			Piece p = CONFETTI.get(i);
			p.x += p.vx * dt;
			p.y += p.vy * dt;
			p.vx += (RNG.nextFloat() - 0.5f) * 40 * dt;
			if (p.y > h + 10) {
				CONFETTI.remove(i);
				continue;
			}
			int px = Math.round(p.x);
			int py = Math.round(p.y);
			g.fill(px, py, px + p.size, py + p.size, p.color);
		}
	}
}
