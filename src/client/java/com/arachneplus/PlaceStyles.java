package com.arachneplus;

import java.util.Random;

public final class PlaceStyles {
	private PlaceStyles() { }

	public record Style(int a, int b, int shadow, int bangs, int font, int size) { }

	public static final String[] COLOR_NAMES = {
			"Red", "Orange", "Yellow", "Gold", "Lime", "Green", "Aqua", "Cyan",
			"Blue", "Purple", "Magenta", "Pink", "White", "Grey", "Black", "Brown", "Dark Red"};
	public static final int[] COLOR_VALUES = {
			0xFFFF2A2A, 0xFFFF8A1E, 0xFFFFE040, 0xFFFFC21A, 0xFF9CFF3A, 0xFF2EE05A, 0xFF35E8D8, 0xFF3AC8FF,
			0xFF3A5BFF, 0xFFA040FF, 0xFFFF3AE0, 0xFFFF8AC8, 0xFFFFFFFF, 0xFFAAAAAA, 0xFF000000, 0xFF7A4A1E, 0xFF7A0A0A};
	public static final String[] FONT_NAMES = {"Block", "Slim", "Text"};
	public static final int[] SIZES = {50, 70, 100, 130, 160, 200, 250};
	public static final int MAX_BANGS = 5;
	public static final int COUNT = 50;

	private static int c(String name) {
		for (int i = 0; i < COLOR_NAMES.length; i++) {
			if (COLOR_NAMES[i].equals(name)) {
				return COLOR_VALUES[i];
			}
		}
		return 0xFFFFFFFF;
	}

	private static final String[][] THEMES = {
			{"Red", "Yellow", "Black"}, {"Orange", "White", "Dark Red"}, {"Green", "Lime", "Black"},
			{"Cyan", "White", "Blue"}, {"Purple", "Pink", "Black"}, {"Pink", "White", "Purple"},
			{"White", "Red", "Black"}, {"Gold", "Orange", "Brown"}, {"Blue", "Aqua", "Black"},
			{"Lime", "Magenta", "Black"}};

	private static final int[][] SHAPES = {{162, 0, 0}, {173, 1, 0}, {150, 2, 1}, {200, 3, 2}, {185, 1, 1}};

	public static Style variant(int i) {
		i = Math.floorMod(i, COUNT);
		String[] t = THEMES[i % THEMES.length];
		int[] s = SHAPES[i / THEMES.length];
		return new Style(c(t[0]), c(t[1]), c(t[2]), s[1], s[2], s[0]);
	}

	private static final Random RNG = new Random();

	public static Style random() {
		return variant(RNG.nextInt(COUNT));
	}

	public static void apply(Style s) {
		Config.placeColorA = s.a();
		Config.placeColorB = s.b();
		Config.placeShadow = s.shadow();
		Config.placeBangs = s.bangs();
		Config.placeFont = s.font();
		Config.placeSize = s.size();
	}

	public static String colorName(int argb) {
		if (argb == 0) {
			return "None";
		}
		for (int i = 0; i < COLOR_VALUES.length; i++) {
			if (COLOR_VALUES[i] == argb) {
				return COLOR_NAMES[i];
			}
		}
		return "Custom";
	}

	public static int nextColor(int argb, boolean allowNone) {
		if (allowNone && argb != 0 && argb == COLOR_VALUES[COLOR_VALUES.length - 1]) {
			return 0;
		}
		for (int i = 0; i < COLOR_VALUES.length; i++) {
			if (COLOR_VALUES[i] == argb) {
				return COLOR_VALUES[(i + 1) % COLOR_VALUES.length];
			}
		}
		return COLOR_VALUES[0];
	}
}
