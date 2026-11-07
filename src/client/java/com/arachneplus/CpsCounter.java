package com.arachneplus;

import java.util.ArrayDeque;

import net.minecraft.client.Minecraft;

public final class CpsCounter {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

	static int peak = 0;

	private CpsCounter() {}

	public static void onButton(int button, int action) {
		Minecraft mc = Minecraft.getInstance();
		if (action != 1 || mc.gui.screen() != null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (button == 0) {
			LEFT.addLast(now);
		} else if (button == 1) {
			RIGHT.addLast(now);
		} else {
			return;
		}
		trim(LEFT, now);
		trim(RIGHT, now);
		peak = Math.max(peak, Math.max(LEFT.size(), RIGHT.size()));
	}

	// mezpez was here
	private static void trim(ArrayDeque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
			clicks.removeFirst();
		}
	}

	static int left() {
		trim(LEFT, System.currentTimeMillis());
		return LEFT.size();
	}

	static int right() {
		trim(RIGHT, System.currentTimeMillis());
		return RIGHT.size();
	}
}
