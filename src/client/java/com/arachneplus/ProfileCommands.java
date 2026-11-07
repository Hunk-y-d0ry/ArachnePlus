package com.arachneplus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;

public final class ProfileCommands {
	private ProfileCommands() { }

	static boolean command(String lower, String c) {
		if (lower.equals("apstats") || lower.startsWith("apstats ")) {
			withName(c, "apstats", "Usage: /apstats <username>", ProfileCommands::stats);
			return true;
		}
		if (lower.equals("apslayer") || lower.startsWith("apslayer ")) {
			withName(c, "apslayer", "Usage: /apslayer <username>", ProfileCommands::slayer);
			return true;
		}
		if (lower.equals("apcompare") || lower.startsWith("apcompare ")) {
			withName(c, "apcompare", "Usage: /apcompare <username>", ProfileCommands::compare);
			return true;
		}
		if (lower.equals("apgear") || lower.startsWith("apgear ")) {
			withName(c, "apgear", "Usage: /apgear <username>", ProfileCommands::gear);
			return true;
		}
		if (lower.equals("apweb") || lower.equals("apwebsite")) {
			web();
			return true;
		}
		if (lower.equals("appf") || lower.startsWith("appf ") || lower.equals("aplfp") || lower.startsWith("aplfp ")) {
			pf(c.length() > 5 ? c.substring(5).trim() : "");
			return true;
		}
		if (lower.equals("apparty")) {
			party();
			return true;
		}
		return false;
	}

	private static void withName(String c, String cmd, String usage, java.util.function.Consumer<String> action) {
		String who = c.substring(cmd.length()).trim();
		if (who.isEmpty()) {
			ArachnePlusClient.say(usage);
			return;
		}
		who = who.split("\\s+")[0];
		if (!PlayerLookup.validName(who)) {
			ArachnePlusClient.say("That does not look like a username. " + usage);
			return;
		}
		if (!PlayerLookup.ready()) {
			return;
		}
		ArachnePlusClient.say("Looking up " + who + "...");
		action.accept(who);
	}

	private static void stats(String name) {
		PlayerLookup.run(name, i -> {
			if (i.error != null) {
				ArachnePlusClient.say(i.error);
				return;
			}
			long[] k = PlayerLookup.kills(i);
			if (k == null) {
				ArachnePlusClient.say(i.name + " has their Skyblock API access turned off (no bestiary data)");
				return;
			}
			ArachnePlusClient.say(i.name + ": " + PlayerLookup.n(k[0]) + " Arachne kills, " + PlayerLookup.n(k[1]) + " Brood kills");
		});
	}

	private static void slayer(String name) {
		PlayerLookup.run(name, i -> {
			if (i.error != null) {
				ArachnePlusClient.say(i.error);
				return;
			}
			long[] s = PlayerLookup.spider(i);
			if (s == null) {
				ArachnePlusClient.say(i.name + " has no slayer data (their Skyblock API access may be off)");
				return;
			}
			ArachnePlusClient.say(i.name + ": Spider Slayer level " + s[1] + " (" + PlayerLookup.n(s[0]) + " XP)");
			ArachnePlusClient.say("Boss kills: T1 " + PlayerLookup.n(s[2]) + " | T2 " + PlayerLookup.n(s[3]) + " | T3 " + PlayerLookup.n(s[4])
					// need to finish this
					+ " | T4 " + PlayerLookup.n(s[5]) + " | T5 " + PlayerLookup.n(s[6]));
		});
	}

	private static String ratio(long arachne, long brood) {
		return arachne <= 0 ? "-" : String.format(Locale.ROOT, "%.1f", brood / (double) arachne);
	}

	private static void compare(String name) {
		long myA = Config.bestiaryKills;
		long myB = Config.broodKills;
		if (myA < 0) {
			ArachnePlusClient.say("Your own kills are not known yet: add your API key (/apapikey <key>) or type /apbestiary <arachne> <brood>");
			return;
		}
		PlayerLookup.run(name, i -> {
			if (i.error != null) {
				ArachnePlusClient.say(i.error);
				return;
			}
			long[] k = PlayerLookup.kills(i);
			if (k == null) {
				ArachnePlusClient.say(i.name + " has their Skyblock API access turned off (no bestiary data)");
				return;
			}
			long diff = k[0] - myA;
			String lead = diff == 0 ? "You are level" : diff > 0 ? i.name + " leads by " + PlayerLookup.n(diff) : "You lead by " + PlayerLookup.n(-diff);
			ArachnePlusClient.say("Arachne kills: You " + PlayerLookup.n(myA) + " vs " + i.name + " " + PlayerLookup.n(k[0]) + " (" + lead + ")");
			if (myB >= 0) {
				ArachnePlusClient.say("Brood kills: You " + PlayerLookup.n(myB) + " vs " + i.name + " " + PlayerLookup.n(k[1]));
				ArachnePlusClient.say("Brood per Arachne kill: You " + ratio(myA, myB) + " vs " + i.name + " " + ratio(k[0], k[1]));
			}
		});
	}

	private static void gear(String name) {
		PlayerLookup.run(name, i -> {
			if (i.error != null) {
				ArachnePlusClient.say(i.error);
				return;
			}
			int combat = PlayerLookup.combatLevel(i);
			ArachnePlusClient.say(i.name + " - Combat level: " + (combat < 0 ? "unknown (skills API off)" : String.valueOf(combat)));
			String gd = PlayerLookup.goldenDragon(i);
			ArachnePlusClient.say("Golden Dragon pet: " + (gd == null ? "unknown (can't read their pets)" : gd));
			PlayerLookup.Gear g = PlayerLookup.gear(i);
			if (g == null) {
				ArachnePlusClient.say("Their inventory API is off, so Sting, wand and armor can't be checked");
				return;
			}
			String sting = g.stingChimera < 0 ? "no" : g.stingChimera == 0 ? "yes, no Chimera" : "yes, Chimera " + roman(g.stingChimera);
			ArachnePlusClient.say("Sting: " + sting + " | Gyrokinetic Wand: " + PlayerLookup.yesNo(g.ids.contains("GYROKINETIC_WAND")));
			String belt;
			if (g.beltReforge == null) {
				belt = "no";
			} else if (g.beltReforge.equals("bloodshot")) {
				belt = "yes, Bloodshot reforge";
			} else if (g.beltReforge.isEmpty()) {
				belt = "yes, no reforge";
			} else {
				String r = g.beltReforge.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + g.beltReforge.substring(1);
				belt = "yes, but " + r + " reforge (not Bloodshot)";
			}
			ArachnePlusClient.say("Primordial Belt: " + belt);
			ArachnePlusClient.say("Armor: " + PlayerLookup.crimsonArmor(g.ids));
		});
	}

	private static String roman(int n) {
		String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
	}

	private static long listUntilMs = 0;
	private static final List<String> LIST_NAMES = new ArrayList<>();

	private static void web() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String me = mc.player.getName().getString();
		link("Arachne Plus website: ", "https://arachneplus.com");
		link("Your player page: ", "https://arachneplus.com/player/" + me);
		link("Guides: ", "https://arachneplus.com/guides");
	}

	private static void link(String label, String url) {
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.network.chat.Component c = net.minecraft.network.chat.Component.literal(label).append(
				net.minecraft.network.chat.Component.literal(url).withStyle(s -> {
					try {
						return s.withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
								.withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)));
					} catch (Throwable ex) {
						return s;
					}
				}));
		mc.player.sendSystemMessage(c);
	}

	private static boolean pfPending = false;
	private static int pfCrystals = 0;
	private static String pfNote = "";
	private static String leader = "";

	private static void pf(String args) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		pfCrystals = 0;
		StringBuilder note = new StringBuilder();
		for (String w : args.split("\\s+")) {
			if (w.isEmpty()) {
				continue;
			}
			if (pfCrystals == 0 && w.matches("\\d{1,4}")) {
				pfCrystals = Integer.parseInt(w);
			} else {
				note.append(note.length() > 0 ? " " : "").append(w);
			}
		}
		pfNote = note.toString();
		LIST_NAMES.clear();
		leader = "";
		pfPending = true;
		listUntilMs = System.currentTimeMillis() + 2000;
		mc.player.connection.sendCommand("party list");
	}

	private static void finishPf() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String me = mc.player.getName().getString();
		int n = Math.max(1, LIST_NAMES.size());
		String who = leader.isEmpty() ? me : leader;
		String msg = "AP Party Finder " + n + "/5" + (pfCrystals > 0 ? ", " + pfCrystals + " crystals" : "")
				+ (pfNote.isEmpty() ? "" : ", Note: " + pfNote);
		String full = msg + " | /p join " + who;
		mc.keyboardHandler.setClipboard(full);
		ArachnePlusClient.say(full);
		ArachnePlusClient.say("Copied to your clipboard - paste it where you want to advertise your party (Ctrl+V)");

		com.google.gson.JsonObject body = new com.google.gson.JsonObject();
		body.addProperty("name", me);
		body.addProperty("leader", who);
		body.addProperty("count", n);
		body.addProperty("crystals", pfCrystals);
		body.addProperty("note", pfNote);
		com.google.gson.JsonArray roles = new com.google.gson.JsonArray();
		for (String w : pfNote.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
			if (w.matches("t[1-5]|placer|gyro") && !roles.contains(new com.google.gson.JsonPrimitive(w))) {
				roles.add(w);
			}
		}
		body.add("roles", roles);
		final String leaderName = who;
		Thread t = new Thread(() -> {
			try {
				com.google.gson.JsonObject o = RankBoard.post("/pf", body.toString());
				if (o.has("error")) {
					String e = o.get("error").getAsString();
					ArachnePlusClient.say(e.equals("wait") ? "Discord post skipped: wait a minute between party finder posts"
							: "Discord post failed: " + e);
				} else {
					pfLeader = leaderName;
					ArachnePlusClient.say("Posted to the Arachne Plus Discord" + (roles.isEmpty() ? "" : " (pinging " + roles + ")"));
				}
			} catch (Exception ex) {
				ArachnePlusClient.say("Discord post failed: " + ex.getMessage());
			}
		}, "ArachnePlus-pf");
		t.setDaemon(true);
		t.start();
	}

	private static volatile String pfLeader = null;

	private static void endPf() {
		String l = pfLeader;
		pfLeader = null;
		if (l == null) {
			return;
		}
		Thread t = new Thread(() -> {
			try {
				RankBoard.post("/pf/end", "{\"leader\":\"" + l + "\"}");
				ArachnePlusClient.say("Party disbanded - your Discord post was updated");
			} catch (Exception ignored) {

			}
		}, "ArachnePlus-pfend");
		t.setDaemon(true);
		t.start();
	}

	private static final Pattern LIST_LINE = Pattern.compile("^\\s*Party (Leader|Moderators|Members):\\s*(.*)$");

	private static void party() {
		if (!PlayerLookup.ready()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		LIST_NAMES.clear();
		listUntilMs = System.currentTimeMillis() + 3500;
		ArachnePlusClient.say("Reading your party...");
		mc.player.connection.sendCommand("party list");
	}

	static void chatLine(String text) {
		String t = text.trim();
		if (pfLeader != null && (t.contains("has disbanded the party") || t.startsWith("The party was disbanded"))) {
			endPf();
		}
		if (listUntilMs != 0) {
			Matcher m = LIST_LINE.matcher(t);
			if (m.find()) {
				if (m.group(1).equals("Leader")) {
					Matcher ln = Pattern.compile("[A-Za-z0-9_]{3,16}").matcher(m.group(2).replaceAll("\\[[^\\]]*\\]", " "));
					if (ln.find()) {
						leader = ln.group();
					}
				}
				String names = m.group(2).replaceAll("\\[[^\\]]*\\]", " ");
				Matcher n = Pattern.compile("[A-Za-z0-9_]{3,16}").matcher(names);
				while (n.find()) {
					String nm = n.group();
					if (!LIST_NAMES.contains(nm)) {
						LIST_NAMES.add(nm);
					}
				}
			}
		}
		if (Config.partyJoinCheck && HypixelApi.available()) {
			Matcher j = JOINED.matcher(t);
			if (j.find()) {
				String who = j.group(1);
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && !who.equalsIgnoreCase(mc.player.getName().getString())) {
					PlayerLookup.run(who, i -> {
						if (i.error != null) {
							return;
						}
						long[] k = PlayerLookup.kills(i);
						ArachnePlusClient.say(i.name + " joined: " + (k == null ? "their API access is off"
								: PlayerLookup.n(k[0]) + " Arachne kills, " + PlayerLookup.n(k[1]) + " Brood kills"));
					});
				}
			}
		}
	}

	private static final Pattern JOINED = Pattern.compile("^(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{3,16}) joined the party\\.?$");

	static void tick() {
		if (listUntilMs == 0 || System.currentTimeMillis() < listUntilMs) {
			return;
		}
		listUntilMs = 0;
		if (pfPending) {
			pfPending = false;
			finishPf();
			return;
		}
		if (LIST_NAMES.isEmpty()) {
			ArachnePlusClient.say("Could not read a party (are you in one?)");
			return;
		}
		List<String> names = new ArrayList<>(LIST_NAMES.subList(0, Math.min(LIST_NAMES.size(), 8)));
		PlayerLookup.runAll(names, all -> {
			all.sort((a, b) -> {
				long[] ka = a.error != null ? null : PlayerLookup.kills(a);
				long[] kb = b.error != null ? null : PlayerLookup.kills(b);
				return Long.compare(kb == null ? -1 : kb[0], ka == null ? -1 : ka[0]);
			});
			ArachnePlusClient.say("Party Arachne kills:");
			int rank = 1;
			for (PlayerLookup.Info i : all) {
				long[] k = i.error != null ? null : PlayerLookup.kills(i);
				if (k == null) {
					ArachnePlusClient.say("-  " + i.name + "  -  API off / not found");
				} else {
					ArachnePlusClient.say("#" + rank++ + "  " + i.name + "  -  " + PlayerLookup.n(k[0]) + " Arachne, " + PlayerLookup.n(k[1]) + " Brood");
				}
			}
		});
	}
}
