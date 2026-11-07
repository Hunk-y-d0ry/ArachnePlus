package com.arachneplus;

final class Theme {
	private Theme() {
	}

	private static boolean dark() {
		return Config.darkGui;
	}

	static int overlay() {
		return dark() ? 0xBF000000 : 0xB0D8D8D8;
	}

	static int box() {
		return dark() ? 0xB0000000 : 0xB0F2F2F2;
	}

	static int text() {
		return dark() ? 0xFFFFFFFF : 0xFF202020;
	}

	static int sub() {
		return dark() ? 0xFFAAAAAA : 0xFF555555;
	}

	static int gold() {
		return dark() ? 0xFFFFAA00 : 0xFFB36B00;
	}

	static int dim() {
		return dark() ? 0xFF999999 : 0xFF555555;
	}

	static int dimmer() {
		return dark() ? 0xFF777777 : 0xFF444444;
	}
}
