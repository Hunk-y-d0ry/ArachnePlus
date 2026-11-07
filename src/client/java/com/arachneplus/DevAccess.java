package com.arachneplus;

final class DevAccess {
	private DevAccess() { }

	private static final String PASSWORD = "6767";

	private static boolean unlocked = false;

	static boolean unlocked() {
		return unlocked;
	}

	static void lock() {
		unlocked = false;
	}

	static boolean tryPassword(String attempt) {
		if (attempt != null && attempt.trim().equals(PASSWORD)) {
			unlocked = true;
			return true;
		}
		return false;
	}
}
