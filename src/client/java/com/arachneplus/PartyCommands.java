package com.arachneplus;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;

public final class PartyCommands {
	private PartyCommands() { }

	private static final Pattern PARTY = Pattern.compile(
			"Party\\s*>\\s*(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{3,16})\\s*:\\s*(.+?)\\s*$");
	private static String lastText = "";
	private static long lastTextMs = 0;
	private static String lastResult = "";
	private static final Random RNG = new Random();

	private static final ArrayDeque<String> QUEUE = new ArrayDeque<>();
	private static long nextSendMs = 0;
	private static String lastChatSent = "";
	private static int repeatCount = 0;
	private static int partyLinesSeen = 0;
	private static int recognised = 0;
	private static int sentCount = 0;
	private static String lastParty = "-";
	private static String lastUnmatched = "-";
	private static String lastSent = "-";
	private static long lastCommandMs = 0;
	private static String lastArgs = "";

	private static final String[] FACTS = {
			"%s most resembles a Grammostola quirogai",
			"Hunkydory_ is the best Ironman Arachne lover",
			"Spiders have eight legs, which is one way to tell them apart from insects",
			"Spider silk is stronger than steel for its weight",
			"Most spiders have eight eyes, but some have six, four, two or none at all",
			"Spider blood is blue because it uses copper, not iron",
			"Spiders cannot chew. They turn their food into liquid with digestive juices first",
			"The Goliath birdeater is the heaviest spider in the world",
			"Female tarantulas can live for over 20 years",
			"Jumping spiders can see in colour and jump many times their own body length",
			"Spiders breathe through book lungs, named after the pages of a book",
			"There are more than 50,000 known species of spider",
			"Spiders have lived on Earth for over 300 million years",
			"Spiders are arachnids, not insects",
			"Many orb weaver spiders eat their old web and build a new one every day",
			"Some tarantulas flick tiny hairs off their body to defend themselves",
			"Arachne is named after a weaver in Greek myth who was turned into a spider"};
	private static final String[] EIGHT = {
			"It is certain", "It is decidedly so", "Without a doubt", "Yes definitely", "You may rely on it",
			"As I see it, yes", "Most likely", "Outlook good", "Yes", "Signs point to yes",
			"Reply hazy, try again", "Ask again later", "Better not tell you now", "Cannot predict now",
			"Concentrate and ask again", "Don't count on it", "My reply is no", "My sources say no",
			"Outlook not so good", "Very doubtful"};
	private static final java.util.HashMap<String, Long> LAST_BY_COMMAND = new java.util.HashMap<>();

	static String info() {
		return "party lines seen " + partyLinesSeen + ", commands recognised " + recognised + ", sent " + sentCount
				+ " | switches: w=" + Config.partyWarp + " pt=" + Config.partyTransfer + " pp=" + Config.partyPromote
				// need to finish this
				+ " kills=" + Config.partyKills + " gyro=" + Config.partyGyro + " goon=" + Config.partyGoon
				+ " ach=" + Config.partyAch + " ac=" + Config.partyAc
				+ " | last party line: " + lastParty + " | last read loosely: " + lastUnmatched + " | last sent: " + lastSent;
	}

	static String test(String text) {
		lastResult = "nothing";
		lastText = "";
		chat(text);
		return lastResult;
	}

	private static final Pattern TOP_LINE = Pattern.compile("\\[AP\\]\\[([A-Za-z0-9_]{3,16})\\]\\s+has\\s+([0-9,]+)\\s+Arachne kills");
	private static long topEndMs = 0;
	private static final java.util.LinkedHashMap<String, Long> TOP = new java.util.LinkedHashMap<>();

	private static final Pattern PB_LINE = Pattern.compile("\\[AP\\]\\[([A-Za-z0-9_]{3,16})\\]\\s+has a longest session of \\((\\d+) min\\) and a pb of \\(([0-9.]+)s\\)");
	private static final Pattern DMG_LINE = Pattern.compile("\\[AP\\]\\[([A-Za-z0-9_]{3,16})\\]\\s+has a highest damage of \\(([0-9,]+)\\)");
	private static long pbEndMs = 0, dmgEndMs = 0;
	private static final java.util.LinkedHashMap<String, Long> PB_SESSION = new java.util.LinkedHashMap<>();
	private static final java.util.LinkedHashMap<String, Double> PB_BEST = new java.util.LinkedHashMap<>();
	private static final java.util.LinkedHashMap<String, Long> DMG = new java.util.LinkedHashMap<>();

	private static String fmtMin(long min) {
		return min >= 60 ? (min / 60) + "h " + (min % 60) + "m" : min + "m";
	}

	private static void extraCollect(String text) {
		try {
			if (pbEndMs != 0) {
				Matcher m = PB_LINE.matcher(text);
				if (m.find()) {
					long min = Long.parseLong(m.group(2));
					double pb = Double.parseDouble(m.group(3));
					if (min > 0) {
						PB_SESSION.put(m.group(1), min);
					}
					if (pb > 0) {
						PB_BEST.put(m.group(1), pb);
					}
				}
			}
			if (dmgEndMs != 0) {
				Matcher m = DMG_LINE.matcher(text);
				if (m.find()) {
					long d = Long.parseLong(m.group(2).replace(",", ""));
					if (d > 0) {
						DMG.put(m.group(1), d);
					}
				}
			}
		} catch (NumberFormatException ignored) {
		}
	}

	private static void extraTick() {
		long now = System.currentTimeMillis();
		if (pbEndMs != 0 && now > pbEndMs) {
			pbEndMs = 0;
			if (PB_SESSION.isEmpty() && PB_BEST.isEmpty()) {
				ArachnePlusClient.say("Nobody answered with a session or pb yet");
			} else {
				var s = new java.util.ArrayList<>(PB_SESSION.entrySet());
				s.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
				if (!s.isEmpty()) {
					ArachnePlusClient.say("Longest Arachne session (party):");
					int i = 1;
					for (var e : s) {
						ArachnePlusClient.say("#" + i++ + "  " + e.getKey() + "  -  " + fmtMin(e.getValue()));
					}
					if (s.size() > 1) {
						ArachnePlusClient.say(s.get(0).getKey() + " is a no life (" + fmtMin(s.get(0).getValue()) + " in one session)");
					}
				}
				var p = new java.util.ArrayList<>(PB_BEST.entrySet());
				p.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
				if (!p.isEmpty()) {
					ArachnePlusClient.say("Fastest pb (party):");
					int i = 1;
					for (var e : p) {
						ArachnePlusClient.say("#" + i++ + "  " + e.getKey() + "  -  " + String.format(Locale.ROOT, "%.1fs", e.getValue()));
					}
				}
			}
			PB_SESSION.clear();
			PB_BEST.clear();
		}
		if (dmgEndMs != 0 && now > dmgEndMs) {
			dmgEndMs = 0;
			if (DMG.isEmpty()) {
				ArachnePlusClient.say("Nobody answered with a highest damage yet");
			} else {
				var d = new java.util.ArrayList<>(DMG.entrySet());
				d.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
				ArachnePlusClient.say("Highest Arachne damage (party):");
				int i = 1;
				for (var e : d) {
					ArachnePlusClient.say("#" + i++ + "  " + e.getKey() + "  -  " + String.format(Locale.ROOT, "%,d", e.getValue()));
				}
				if (d.size() > 1) {
					ArachnePlusClient.say(d.get(0).getKey() + " is a hacker (" + String.format(Locale.ROOT, "%,d", d.get(0).getValue()) + " damage)");
				}
			}
			DMG.clear();
		}
	}

	private static void topCollect(String text) {
		extraCollect(text);
		if (topEndMs == 0) {
			return;
		}
		Matcher m = TOP_LINE.matcher(text);
		if (m.find()) {
			try {
				TOP.put(m.group(1), Long.parseLong(m.group(2).replace(",", "")));
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private static void topTick() {
		extraTick();
		if (topEndMs != 0 && System.currentTimeMillis() > topEndMs) {
			topEndMs = 0;
			if (TOP.isEmpty()) {
				ArachnePlusClient.say("Leaderboard: nobody answered (they need Arachne Plus with the API set up)");
				return;
			}
			java.util.List<java.util.Map.Entry<String, Long>> rows = new java.util.ArrayList<>(TOP.entrySet());
			rows.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
			ArachnePlusClient.say("Arachne kills leaderboard (party):");
			int i = 1;
			for (var e : rows) {
				ArachnePlusClient.say("#" + i++ + "  " + e.getKey() + "  -  " + String.format(Locale.ROOT, "%,d", e.getValue()) + " kills");
			}
			TOP.clear();
		}
	}

	static void chat(String raw) {
		String text = raw.replaceAll("\u00a7.", "");
		long t = System.currentTimeMillis();
		if (text.equals(lastText) && t - lastTextMs < 800) {
			return;
		}
		lastText = text;
		lastTextMs = t;
		ProfileCommands.chatLine(text);
		String sender;
		String body;
		Matcher m = PARTY.matcher(text);
		if (m.find()) {
			sender = m.group(1);
			body = m.group(2);
		} else {
			int at = text.toLowerCase(Locale.ROOT).indexOf("party >");
			if (at < 0) {
				return;
			}
			String rest = text.substring(at + 7);
			int colon = rest.indexOf(':');
			if (colon < 0) {
				return;
			}
			String[] nameParts = rest.substring(0, colon).trim().split("\\s+");
			sender = nameParts[nameParts.length - 1];
			body = rest.substring(colon + 1).trim();
			lastUnmatched = text;
		}
		partyLinesSeen++;
		lastParty = text;
		topCollect(body);
		String cmd = body.toLowerCase(Locale.ROOT).trim();
		lastArgs = "";
		int sp = cmd.indexOf(' ');
		if (sp > 0) {
			String rest = cmd.substring(sp + 1).trim();
			lastArgs = rest;
			cmd = cmd.substring(0, sp);
			if (cmd.equals("!p") && (rest.equals("damage") || rest.startsWith("damage ") || rest.equals("dmg"))) {
				cmd = "!pbdmg";
			}
		}
		if (!cmd.startsWith("!")) {
			return;
		}
		recognised++;
		lastResult = "saw " + cmd + " from " + sender + " (if nothing is sent, that command is switched off or on cooldown)";
		handle(sender, cmd);
	}

	static void own(String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String cmd = message.trim().toLowerCase(Locale.ROOT);
		lastArgs = "";
		int sp = cmd.indexOf(' ');
		if (sp > 0) {
			String rest = cmd.substring(sp + 1).trim();
			lastArgs = rest;
			cmd = cmd.substring(0, sp);
			if (cmd.equals("!p") && (rest.equals("damage") || rest.startsWith("damage ") || rest.equals("dmg"))) {
				cmd = "!pbdmg";
			}
		}
		if (cmd.startsWith("!")) {
			handle(mc.player.getName().getString(), cmd);
		}
	}

	static void send(String command) {
		QUEUE.add(command);
	}

	private static String essence(long kills) {
		double total = kills * EssenceCalc.PER_KILL;
		if (total == Math.floor(total)) {
			return String.format(Locale.ROOT, "%,d", (long) total);
		}
		return String.format(Locale.ROOT, "%,.1f", total);
	}

	private static boolean enabled(boolean sw, String cmd) {
		if (Config.partyAlways || sw) {
			return true;
		}
		ArachnePlusClient.say(cmd + " is switched OFF - turn it on in the Party Commands screen");
		return false;
	}

	private static final String[][] NAMES = {
			{"!w", "!w"}, {"!hop", "!hop"}, {"!rank", "!rank"}, {"!rankings", "!rank"}, {"!place", "!rank"}, {"!top", "!top"}, {"!lb", "!top"}, {"!topkills", "!top"}, {"!leaderboard", "!top"}, {"!leaderbord", "!top"}, {"!ranking", "!top"}, {"!last", "!split"}, {"!split", "!split"}, {"!splits", "!split"}, {"!spilt", "!split"},
			{"!spilts", "!split"}, {"!lastkill", "!split"}, {"!lastsplit", "!split"}, {"!lastsplits", "!split"}, {"!lastspilt", "!split"}, {"!pt", "!pt"}, {"!pp", "!pp"}, {"!ac", "!ac"}, {"!pb", "!pb"}, {"!a", "!arachne"},
			{"!arachne", "!arachne"}, {"!kills", "!kills"}, {"!kill", "!kills"}, {"!gyro", "!gyro"},
			{"!goon", "!goon"}, {"!gooner", "!goon"}, {"!gooning", "!goon"}, {"!achievements", "!achievements"},
			{"!achievement", "!achievements"}, {"!ach", "!achievements"}, {"!total", "!total"}, {"!profit", "!total"},
			{"!fact", "!fact"}, {"!facts", "!fact"}, {"!8ball", "!8ball"}, {"!eightball", "!8ball"}, {"!pbtime", "!pbtime"}, {"!pbdmg", "!pbdmg"}, {"!damage", "!pbdmg"}, {"!dmg", "!pbdmg"}, {"!pdamage", "!pbdmg"}, {"!highestdamage", "!pbdmg"}, {"!coins", "!coins"}, {"!coin", "!coins"}, {"!essence", "!essence"}, {"!essance", "!essence"}, {"!essencesesh", "!essencesesh"}, {"!essencesession", "!essencesesh"}, {"!essencetotal", "!essencetotal"}, {"!totalessence", "!essencetotal"}, {"!essancetotal", "!essencetotal"}, {"!session", "!session"}, {"!sesh", "!session"}, {"!seshion", "!session"}, {"!skills", "!skills"}, {"!skill", "!skills"}, {"!stats", "!skills"}};

	static String canonical(String typed) {
		String t = typed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9!]", "");
		for (String[] n : NAMES) {
			if (n[0].equals(t)) {
				return n[1];
			}
		}
		if (t.length() < 5) {
			return typed;
		}
		String best = null;
		int bestD = 99;
		boolean tie = false;
		for (String[] n : NAMES) {
			if (n[0].length() < 5) {
				continue;
			}
			int d = distance(t, n[0]);
			int allowed = n[0].length() >= 8 ? 2 : 1;
			if (d <= allowed) {
				if (d < bestD) {
					best = n[1];
					bestD = d;
					tie = false;
				} else if (d == bestD && !n[1].equals(best)) {
					tie = true;
				}
			}
		}
		return best == null || tie ? typed : best;
	}

	private static int distance(String a, String b) {
		int[][] d = new int[a.length() + 1][b.length() + 1];
		for (int i = 0; i <= a.length(); i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= b.length(); j++) {
			d[0][j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + c);
			}
		}
		return d[a.length()][b.length()];
	}

	private static void handle(String sender, String cmd) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		cmd = canonical(cmd);
		Long again = LAST_BY_COMMAND.get(cmd);
		if (now - lastCommandMs < 800 || (again != null && now - again < 3000)) {
			return;
		}
		LAST_BY_COMMAND.put(cmd, now);
		nextSendMs = Math.max(nextSendMs, now + 1800);
		String me = mc.player.getName().getString();
		switch (cmd) {
			case "!w" -> {
				if (enabled(Config.partyWarp, cmd)) {
					lastCommandMs = now;
					QUEUE.add("p warp");
				}
			}
			case "!pt" -> {
				if (enabled(Config.partyTransfer, cmd)) {
					lastCommandMs = now;
					QUEUE.add("p transfer " + sender);
				}
			}
			case "!pp" -> {
				if (enabled(Config.partyPromote, cmd)) {
					lastCommandMs = now;
					QUEUE.add("p promote " + sender);
				}
			}
			case "!kills", "!kill" -> {
				if (enabled(Config.partyKills, cmd)) {
					lastCommandMs = now;
					if (Config.bestiaryKills < 0) {
						QUEUE.add("pc [AP] " + me + " does not have this API feature enabled");
					} else {
						String brood = Config.broodKills < 0 ? "?" : String.valueOf(Config.broodKills);
						QUEUE.add("pc [AP] " + me + " has " + Config.bestiaryKills + " Arachne kills and " + brood
								// need to finish, add brood rank?
								+ " Arachne's Brood kills");
					}
				}
			}
			case "!gyro" -> {
				if (enabled(Config.partyGyro, cmd)) {
					lastCommandMs = now;
					gyroCheck(mc, me);
				}
			}
			case "!goon", "!gooner", "!gooning" -> {
				if (enabled(Config.partyGoon, cmd)) {
					lastCommandMs = now;
					int pct = RNG.nextInt(101);
					QUEUE.add("pc [AP] " + sender + " is " + pct + "% an Arachne G O O N E R"
							+ (pct > 95 ? ". " + sender + " sleeps with an Arachne bodypillow!" : ""));
				}
			}
			case "!achievements", "!achievement", "!ach" -> {
				if (enabled(Config.partyAch, cmd)) {
					lastCommandMs = now;
					QUEUE.add("pc [AP] " + me + " has " + Achievements.unlockedCount() + "/" + Achievements.total()
							// my favourite one lol
							+ " achievements!");
				}
			}
			case "!ac" -> {
				if (enabled(Config.partyAc, cmd)) {
					lastCommandMs = now;
					if (Achievements.has("cps20")) {
						QUEUE.add("pc [AP] " + me + " has a nice Air Conditioning unit and it's nice and chilly");
					} else {
						QUEUE.add("pc [AP] " + me + " doesn't have an air conditioning unit and is a bit toasty");
					}
				}
			}
			case "!arachne" -> {
				if (enabled(Config.partyArachne, cmd)) {
					lastCommandMs = now;
					QUEUE.add("pc [AP] " + me + " loves Arachne");
				}
			}
			case "!rank" -> {
				if (enabled(Config.partyRank, cmd)) {
					lastCommandMs = now;
					if (!Config.rankOptIn || RankBoard.myRank < 1) {
						QUEUE.add("pc [AP] " + me + " is not on the Arachne leaderboard");
					} else {
						QUEUE.add("pc [AP] " + me + " is #" + RankBoard.myRank + " of " + RankBoard.total
								+ " on the Arachne leaderboard (" + String.format(Locale.ROOT, "%,d", RankBoard.myKills) + " kills)");
					}
				}
			}
			case "!top" -> {
				if (enabled(Config.partyTop, cmd)) {
					lastCommandMs = now;
					topEndMs = now + 30_000L;
					TOP.clear();
					if (Config.bestiaryKills < 0) {
						QUEUE.add("pc [AP] " + me + " does not have this API feature enabled");
					} else {
						QUEUE.add("pc [AP] " + me + " has " + String.format(Locale.ROOT, "%,d", Config.bestiaryKills) + " Arachne kills");
					}
				}
			}
			case "!split" -> {
				if (enabled(Config.partySplit, cmd)) {
					lastCommandMs = now;
					if (!ArachneTracker.splitsValid) {
						QUEUE.add("pc [AP] " + me + " has no split times for a last kill yet");
					} else {
						QUEUE.add("pc [AP] " + me + " last kill ("
								+ String.format(Locale.ROOT, "%.1fs", ArachneTracker.lastSplitsTotalMs / 1000.0) + "): "
								+ ArachneTracker.splitsText(" | "));
					}
				}
			}
			case "!hop" -> {
				if (enabled(Config.partyHop, cmd)) {
					lastCommandMs = now;
					ArachneTracker.setHop();
				}
			}
			case "!session" -> {
				if (enabled(Config.partySession, cmd)) {
					lastCommandMs = now;
					int kills = ArachneTracker.sessionKills;
					if (kills <= 0) {
						QUEUE.add("pc [AP] " + me + " has not killed any bosses this session yet");
					} else {
						long mins = Math.max(0, (System.currentTimeMillis() - ArachneTracker.sessionFirstKillMs) / 60_000L);
						long h = mins / 60;
						long m = mins % 60;
						String time = (h > 0 ? h + (h == 1 ? " hour" : " hours") + " and " : "") + m + (m == 1 ? " minute" : " minutes");
						QUEUE.add("pc [AP] " + me + " has killed " + kills + (kills == 1 ? " boss" : " bosses") + " in " + time
								+ ", this has made them " + String.format(Locale.ROOT, "%,d", Config.profitPerKill() * kills) + " coins.");
					}
				}
			}
			case "!pbtime" -> {
				if (enabled(Config.partyPbTime, cmd)) {
					lastCommandMs = now;
					pbEndMs = now + 30_000L;
					PB_SESSION.clear();
					PB_BEST.clear();
					QUEUE.add("pc [AP] " + me + " has a longest session of (" + (Config.longestSessionMs / 60_000L) + " min) and a pb of ("
							+ String.format(Locale.ROOT, "%.1f", Config.bestKillMs / 1000.0) + "s)");
				}
			}
			case "!pbdmg" -> {
				if (enabled(Config.partyDamage, cmd)) {
					lastCommandMs = now;
					dmgEndMs = now + 30_000L;
					DMG.clear();
					QUEUE.add("pc [AP] " + me + " has a highest damage of (" + String.format(Locale.ROOT, "%,d", Config.maxDamage) + ")");
				}
			}
			case "!fact" -> {
				if (sender.equals(me) && enabled(Config.partyFact, cmd)) {
					lastCommandMs = now;
					QUEUE.add("pc [AP] " + FACTS[RNG.nextInt(FACTS.length)].replace("%s", me));
				}
			}
			case "!8ball" -> {
				if (sender.equals(me) && enabled(Config.party8ball, cmd)) {
					lastCommandMs = now;
					QUEUE.add("pc [AP] " + me + " 8 ball says: " + EIGHT[RNG.nextInt(EIGHT.length)]);
				}
			}
			case "!essence" -> {
				if (enabled(Config.partyEssence, cmd)) {
					lastCommandMs = now;
					if (!HypixelApi.available()) {
						QUEUE.add("pc [AP] " + me + " does not have this API feature enabled");
					} else {
						BestiaryTracker.essenceScan(have -> {
							String text;
							if (have < 0) {
								text = "pc [AP] " + me + " does not have this API feature enabled";
							} else {
								text = "pc [AP] " + me + " has " + String.format(Locale.ROOT, "%,d", have) + " spider essence";
							}
							synchronized (QUEUE) {
								QUEUE.add(text);
							}
						});
					}
				}
			}
			case "!essencesesh" -> {
				if (enabled(Config.partyEssence, cmd)) {
					lastCommandMs = now;
					int kills = ArachneTracker.sessionKills;
					if (kills <= 0) {
						QUEUE.add("pc [AP] " + me + " has not earnt any spider essence this session yet");
					} else {
						QUEUE.add("pc [AP] " + me + " has earnt " + essence(kills) + " spider essence this session from " + kills
								+ (kills == 1 ? " boss" : " bosses"));
					}
				}
			}
			case "!essencetotal" -> {
				if (enabled(Config.partyEssence, cmd)) {
					lastCommandMs = now;
					long kills = Config.bestiaryKills >= 0 ? Config.bestiaryKills : Config.killCount;
					if (kills <= 0) {
						QUEUE.add("pc [AP] " + me + " has not earnt any spider essence from Arachne yet");
					} else {
						Thread t = new Thread(() -> {
							double price = RankBoard.essencePrice();
							String text = "pc [AP] " + me + " has earnt " + essence(kills) + " spider essence over all their kills";
							if (price > 0) {
								text += ", worth " + String.format(Locale.ROOT, "%,d", Math.round(kills * EssenceCalc.PER_KILL * price)) + " coins at the current Bazaar price";
							}
							synchronized (QUEUE) {
								QUEUE.add(text);
							}
						}, "ArachnePlus-essencetotal");
						t.setDaemon(true);
						t.start();
					}
				}
			}
			case "!coins" -> {
				if (enabled(Config.partyCoins, cmd)) {
					lastCommandMs = now;
					int kills = ArachneTracker.sessionKills;
					if (kills <= 0) {
						QUEUE.add("pc [AP] " + me + " has not made any coins this session yet");
					} else {
						QUEUE.add("pc [AP] " + me + " has made " + String.format(Locale.ROOT, "%,d", Config.profitPerKill() * kills)
								+ " coins this session from " + kills + (kills == 1 ? " boss" : " bosses"));
					}
				}
			}
			case "!skills" -> {
				if (enabled(Config.partySkills, cmd)) {
					lastCommandMs = now;
					if (ArachneTracker.sessionBoards <= 0) {
						QUEUE.add("pc [AP] " + me + " has no leaderboard stats this session yet");
					} else {
						double pos = ArachneTracker.sessionPosSum / (double) ArachneTracker.sessionBoards;
						long dmg = Math.round(ArachneTracker.sessionDamageSum / (double) ArachneTracker.sessionBoards);
						String p = Math.abs(pos - Math.rint(pos)) < 0.05 ? String.valueOf(Math.round(pos))
								: String.format(Locale.ROOT, "%.1f", pos);
						QUEUE.add("pc [AP] " + me + " has an average position of (#" + p + ") with an average damage of ("
								+ String.format(Locale.ROOT, "%,d", dmg) + ")!");
					}
				}
			}
			case "!pb" -> {
				if (enabled(Config.partyPb, cmd)) {
					lastCommandMs = now;
					if (Config.bestKillMs <= 0) {
						QUEUE.add("pc [AP] " + me + " does not have a pb yet");
					} else {
						QUEUE.add("pc [AP] " + me + " has a pb of (" + String.format(Locale.ROOT, "%.1f", Config.bestKillMs / 1000.0) + "s)");
					}
				}
			}
			case "!total" -> {
				if (enabled(Config.partyTotal, cmd)) {
					lastCommandMs = now;
					long total;
					if (Config.bestiaryKills >= 0) {
						total = Config.profitPerKill() * Config.bestiaryKills;
					} else {
						total = Config.profitPerKill() * Config.killCount;
					}
					if (total <= 0) {
						QUEUE.add("pc [AP] " + me + " has not earnt anything from Arachne yet");
					} else {
						QUEUE.add("pc [AP] " + me + " has earnt " + String.format(Locale.ROOT, "%,d", total)
								+ " coins over all their kills!");
					}
				}
			}
			default -> { }
		}
	}

	private static boolean inventoryHasGyro(Minecraft mc) {
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			var s = inv.getItem(i);
			if (!s.isEmpty() && s.getHoverName().getString().toLowerCase(Locale.ROOT).contains("gyrokinetic")) {
				return true;
			}
		}
		return false;
	}

	private static void gyroCheck(Minecraft mc, String me) {
		boolean local = inventoryHasGyro(mc);
		if (local) {
			QUEUE.add("pc [AP] " + me + " has a Gyrokinetic Wand");
			return;
		}
		if (!HypixelApi.available()) {
			QUEUE.add("pc [AP] " + me + " does not have this API feature enabled");
			return;
		}
		BestiaryTracker.gyroScan(result -> {
			if (result == 1) {
				QUEUE.add("pc [AP] " + me + " has a Gyrokinetic Wand");
			} else if (result == 0) {
				QUEUE.add("pc [AP] " + me + " does not have a Gyrokinetic Wand");
			} else {
				QUEUE.add("pc [AP] " + me + " does not have this API feature enabled");
			}
		});
	}

	static void tick(Minecraft mc) {
		topTick();
		ProfileCommands.tick();
		if (QUEUE.isEmpty() || mc.player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextSendMs) {
			return;
		}
		String cmd;
		synchronized (QUEUE) {
			cmd = QUEUE.poll();
		}
		if (cmd != null) {
			nextSendMs = now + 2000;
			// dont touch this bit it fixed the spam
			if (cmd.startsWith("pc ") && cmd.equals(lastChatSent)) {
				repeatCount++;
				char end = cmd.charAt(cmd.length() - 1);
				cmd = cmd + String.valueOf(end == '.' || end == '!' ? end : '!').repeat(Math.min(repeatCount, 4));
			} else {
				repeatCount = 0;
				lastChatSent = cmd.startsWith("pc ") ? cmd : "";
			}
			sentCount++;
			lastSent = cmd;
			mc.player.connection.sendCommand(cmd);
		}
	}
}
