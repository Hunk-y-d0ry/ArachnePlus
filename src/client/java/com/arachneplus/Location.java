package com.arachneplus;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;

public final class Location {

	static boolean inSanctuary = false;

	static String lastLocation = "";

	static long enteredAtMs = 0;

	static long sinceEnteredMs() {
		return enteredAtMs == 0 ? Long.MAX_VALUE : System.currentTimeMillis() - enteredAtMs;
	}

	private static int ticks = 0;

	private Location() {}

	static void update(Minecraft mc) {
		if (++ticks % 20 != 0) {
			return;
		}
		if (mc.level == null) {
			inSanctuary = false;
			lastLocation = "";
			return;
		}
		try {
			var scoreboard = mc.level.getScoreboard();
			var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
			if (objective == null) {
				inSanctuary = false;
				lastLocation = "";
				return;
			}
			String wanted = clean(Config.sanctuaryText).toLowerCase(Locale.ROOT);
			boolean found = false;
			String area = "";
			for (var entry : scoreboard.listPlayerScores(objective)) {
				String owner = entry.owner();
				var team = scoreboard.getPlayersTeam(owner);
				String line = (team == null ? "" : team.getPlayerPrefix().getString())
						+ owner
						+ (team == null ? "" : team.getPlayerSuffix().getString());
				line = clean(line).trim();
				if (line.toLowerCase(Locale.ROOT).contains(wanted)) {
					found = true;
				}

				if (area.isEmpty() && (line.contains("⏣") || line.contains("ф"))) {
					area = line;
				}
			}
			if (found && !inSanctuary) {
				enteredAtMs = System.currentTimeMillis();
			}
			inSanctuary = found;
			lastLocation = area;
		} catch (RuntimeException e) {
			inSanctuary = false;
			lastLocation = "(could not read sidebar)";
		}
	}

	private static String clean(String s) {
		return s.replaceAll("§.", "").replace('’', '\'');
	}
}
