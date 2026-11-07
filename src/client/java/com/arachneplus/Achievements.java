package com.arachneplus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;

public final class Achievements {
	private Achievements() { }

	public record Def(String id, String category, String name, String desc) { }

	public static final List<Def> ALL = new ArrayList<>();

	private static void add(String id, String cat, String name, String desc) {
		ALL.add(new Def(id, cat, name, desc));
	}

	static {
		add("k100", "Arachne kills", "Web Starter", "Kill Arachne 100 times");
		add("k500", "Arachne kills", "Spider Slayer", "Kill Arachne 500 times");
		add("k1000", "Arachne kills", "Eight Legs Down", "Kill Arachne 1,000 times");
		add("k2500", "Arachne kills", "Arachne's Nightmare", "Kill Arachne 2,500 times");
		add("k5000", "Arachne kills", "Silk Cutter", "Kill Arachne 5,000 times");
		add("k10000", "Arachne kills", "Soul String Addict", "Kill Arachne 10,000 times");
		add("k25000", "Arachne kills", "Arachne Addict", "Kill Arachne 25,000 times");
		add("k50000", "Arachne kills", "Arachne Is My Job", "Kill Arachne 50,000 times");
		add("k100000", "Arachne kills", "Arachne Gooner", "Kill Arachne 100,000 times");

		add("t10", "Speed", "QUICK QUICK QUICK", "Kill Arachne in under 10 seconds");
		add("t75", "Speed", "Need for Speed", "Kill Arachne in under 7.5 seconds");
		add("t5", "Speed", "How He Do Dat", "Kill Arachne in under 5 seconds");

		add("gyro", "Fights", "Sucked In", "Left-click a Gyrokinetic Wand in the sanctuary");
		add("solo", "Fights", "Alone at the end of the universe humming a song", "Solo Arachne");
		add("d500", "Fights", "Heavy Hitter", "Do over 500k damage in one fight");
		add("d750", "Fights", "Wrecking Ball", "Do over 750k damage in one fight");
		add("d1m", "Fights", "Million Dollar Hit", "Do over 1 million damage in one fight");
		add("free", "Fights", "Freeloading", "Do under 10k damage and still finish top 5");
		add("final1000", "Fights", "Kill Stealer", "Get the final hit on Arachne 1,000 times");

		add("c64", "Crystals placed", "Crystal Stack", "Place 64 crystals");
		add("c250", "Crystals placed", "Crystal Collector", "Place 250 crystals");
		add("c1000", "Crystals placed", "Crystal Hoarder", "Place 1,000 crystals");
		add("c10000", "Crystals placed", "Crystal Baron", "Place 10,000 crystals");
		add("c25000", "Crystals placed", "Crystal Overlord", "Place 25,000 crystals");

		add("s1", "Session length", "Warming Up", "A 1 hour Arachne session");
		add("s25", "Session length", "Settling In", "A 2.5 hour Arachne session");
		add("s5", "Session length", "Dedicated", "A 5 hour Arachne session");
		add("s12", "Session length", "Touch Grass", "A 12 hour Arachne session");
		add("s24", "Session length", "No Life", "A 24 hour Arachne session");

		add("cps20", "Clicking", "Nice Air Conditioning Unit", "Hit 20 CPS with left click");
		add("cps10", "Clicking", "Turtle Master", "Hit 10 CPS with left click");
		add("bozo", "Slayer", "L Bozo", "Die to Arachne or her brood");
		add("multi", "Slayer", "Multitasking", "Kill a T5 spider slayer boss during an Arachne session");
		add("wasp", "Slayer", "Shriveled Spider", "Drop a Shriveled Wasp from a T5 spider boss in the Arachne Sanctuary");
		add("eye", "Slayer", "Eye on the Prize", "Drop a Primordial Eye from a T5 spider boss in the Arachne Sanctuary");
		add("completionist", "Completionist", "Completionist", "Unlock every achievement");
	}

	public static int total() {
		return ALL.size() - 1;
	}

	public static boolean has(String id) {
		return Config.achievements.contains("," + id + ",");
	}

	public static int unlockedCount() {
		int n = 0;
		for (Def d : ALL) {
			if (!d.id().equals("completionist") && has(d.id())) {
				n++;
			}
		}
		return n;
	}

	public static void unlock(String id) {
		unlock(id, false, true);
	}

	public static void unlock(String id, boolean force) {
		unlock(id, force, false);
	}

	private static boolean hasNatural(String id) {
		return Config.achievementsNatural.contains("," + id + ",");
	}

	private static int naturalCount() {
		int n = 0;
		for (Def d : ALL) {
			if (!d.id().equals("completionist") && hasNatural(d.id())) {
				n++;
			}
		}
		return n;
	}

	private static void unlock(String id, boolean force, boolean natural) {
		boolean had = has(id);
		if (natural && !hasNatural(id)) {
			if (Config.achievementsNatural.isEmpty()) {
				Config.achievementsNatural = ",";
			}
			Config.achievementsNatural = Config.achievementsNatural + id + ",";
			Config.save();
			RankBoard.announce(id);
		}
		if (had && !force) {
			return;
		}
		if (!had) {
			if (Config.achievements.isEmpty()) {
				Config.achievements = ",";
			}
			Config.achievements = Config.achievements + id + ",";
			Config.save();
		}
		for (Def d : ALL) {
			if (d.id().equals(id)) {
				Minecraft mc = Minecraft.getInstance();
				String who = mc.player == null ? "Player" : mc.player.getName().getString();
				String text = "[AP] " + who + " got the \"" + d.name() + "\" Achievement!!!!! (" + d.desc() + ")";
				if (mc.player != null) {
					var white = net.minecraft.ChatFormatting.WHITE;
					mc.player.sendSystemMessage(ArachnePlusClient.fullPrefix()
							.append(net.minecraft.network.chat.Component.literal("[" + who + "]")
									.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.BOLD))
							.append(net.minecraft.network.chat.Component.literal(" got the \"").withStyle(white))
							.append(net.minecraft.network.chat.Component.literal(d.name())
									.withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD))
							.append(net.minecraft.network.chat.Component.literal("\" Achievement!!!!! (" + d.desc() + ")").withStyle(white)));
				}
				AchievementFx.show(d.name(), d.desc(), id.equals("completionist"));
				if (natural && Config.partyShareAch && mc.player != null) {
					PartyCommands.send("pc " + text);
				}
			}
		}
		if (!id.equals("completionist") && unlockedCount() >= total()) {
			unlock("completionist", false, naturalCount() >= total());
		}
	}

	public static void resetToEarned() {
		Config.achievements = Config.achievementsNatural;
		if (!hasNatural("completionist") && naturalCount() < total()) {
			Config.achievements = Config.achievements.replace(",completionist,", ",");
		}
		if (Config.achievements.equals(",")) {
			Config.achievements = "";
		}
		Config.save();
	}

	public static void giveAll() {
		for (Def d : ALL) {
			if (!d.id().equals("completionist")) {
				unlock(d.id(), false, false);
			}
		}
	}

	public static void removeAll() {
		Config.achievements = "";
		Config.achievementsNatural = "";
		Config.save();
	}

	public static String remove(String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return null;
		}
		Def best = null;
		for (Def d : ALL) {
			String n = d.name().toLowerCase(Locale.ROOT);
			if (n.equals(q) || d.id().equals(q)) {
				best = d;
				break;
			}
			if (best == null && n.contains(q)) {
				best = d;
			}
		}
		if (best == null) {
			return null;
		}
		Config.achievements = Config.achievements.replace("," + best.id() + ",", ",");
		Config.achievementsNatural = Config.achievementsNatural.replace("," + best.id() + ",", ",");
		if (!best.id().equals("completionist")) {
			Config.achievements = Config.achievements.replace(",completionist,", ",");
			Config.achievementsNatural = Config.achievementsNatural.replace(",completionist,", ",");
		}
		if (Config.achievements.equals(",")) {
			Config.achievements = "";
		}
		if (Config.achievementsNatural.equals(",")) {
			Config.achievementsNatural = "";
		}
		Config.save();
		return best.name();
	}

	public static String makeKey() {
		String raw = Config.achievements + "|" + Config.achievementsNatural + "|" + Config.crystalsPlaced + "|" + Config.finalHits;
		return "APK1." + java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public static boolean useKey(String key) {
		try {
			String k = key.trim();
			if (!k.startsWith("APK1.")) {
				return false;
			}
			String raw = new String(java.util.Base64.getUrlDecoder().decode(k.substring(5)), java.nio.charset.StandardCharsets.UTF_8);
			String[] p = raw.split("\\|", -1);
			if (p.length < 4) {
				return false;
			}
			Config.achievements = p[0];
			Config.achievementsNatural = p[1];
			Config.crystalsPlaced = Math.max(Config.crystalsPlaced, Integer.parseInt(p[2]));
			Config.finalHits = Math.max(Config.finalHits, Integer.parseInt(p[3]));
			Config.save();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean downSeen = false;
	private static boolean downFinalMine = false;
	private static long downAt = 0;
	private static final java.util.Set<String> DAMAGERS = new HashSet<>();
	private static final Pattern DAMAGER = Pattern.compile(
			"\\d+(?:st|nd|rd|th) Damager\\s*-\\s*(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{3,16})", Pattern.CASE_INSENSITIVE);
	private static final Pattern FINAL_BLOW = Pattern.compile(
			"(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{3,16}) dealt the final blow", Pattern.CASE_INSENSITIVE);

	private static void soloChat(String text, String me) {
		String lower = text.toLowerCase(Locale.ROOT);
		long now = System.currentTimeMillis();
		if (lower.contains("arachne down!")) {
			downSeen = true;
			downFinalMine = false;
			DAMAGERS.clear();
			downAt = now;
			return;
		}
		if (!downSeen || now - downAt > 15_000) {
			downSeen = false;
			return;
		}
		Matcher f = FINAL_BLOW.matcher(text);
		if (f.find() && f.group(1).equalsIgnoreCase(me)) {
			downFinalMine = true;
		}
		Matcher d = DAMAGER.matcher(text);
		if (d.find()) {
			DAMAGERS.add(d.group(1).toLowerCase(Locale.ROOT));
		}
		if (lower.contains("your damage")) {
			Matcher pm = Pattern.compile("position\\s*#\\s*(\\d+)").matcher(lower);
			Matcher dm = Pattern.compile("your damage\\D{0,6}?([\\d][\\d,.]*)\\s*([kmb]?)").matcher(lower);
			if (pm.find() && dm.find()) {
				try {
					double v = Double.parseDouble(dm.group(1).replace(",", ""));
					String u = dm.group(2);
					v *= u.equals("k") ? 1_000 : u.equals("m") ? 1_000_000 : u.equals("b") ? 1_000_000_000 : 1;
					ArachneTracker.recordBoard(Integer.parseInt(pm.group(1)), v);
				} catch (NumberFormatException ignored) {

				}
			}
			boolean pos1 = lower.contains("position #1");
			if (downFinalMine && pos1 && DAMAGERS.size() == 1 && DAMAGERS.contains(me.toLowerCase(Locale.ROOT))) {
				unlock("solo");
			}
			downSeen = false;
		}
	}

	public static String give(String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.equals("all")) {
			giveAll();
			return "all";
		}
		Def best = null;
		for (Def d : ALL) {
			String n = d.name().toLowerCase(Locale.ROOT);
			if (n.equals(q) || d.id().equals(q)) {
				best = d;
				break;
			}
			if (best == null && n.contains(q) && !q.isEmpty()) {
				best = d;
			}
		}
		if (best == null) {
			return null;
		}
		unlock(best.id(), true);
		return best.name();
	}

	private static void thresholds(long value, long[] limits, String[] ids) {
		for (int i = 0; i < limits.length; i++) {
			if (value >= limits[i]) {
				unlock(ids[i]);
			}
		}
	}

	private static final long[] KILL_AT = {100, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000};
	private static final String[] KILL_IDS = {"k100", "k500", "k1000", "k2500", "k5000", "k10000", "k25000", "k50000", "k100000"};
	private static final long[] CRYSTAL_AT = {64, 250, 1000, 10000, 25000};
	private static final String[] CRYSTAL_IDS = {"c64", "c250", "c1000", "c10000", "c25000"};
	private static final long H = 3_600_000L;
	private static final long[] SESSION_AT = {H, H * 5 / 2, H * 5, H * 12, H * 24};
	private static final String[] SESSION_IDS = {"s1", "s25", "s5", "s12", "s24"};

	static void onKill(long fightMs, boolean timed) {
		if (timed && fightMs < 10_000) {
			unlock("t10");
		}
		if (timed && fightMs < 7_500) {
			unlock("t75");
		}
		if (timed && fightMs < 5_000) {
			unlock("t5");
		}
		checkKills();
		checkSession();
		openChatWindow();
	}

	static void onCrystal() {
		Config.crystalsPlaced++;
		Config.save();
		thresholds(Config.crystalsPlaced, CRYSTAL_AT, CRYSTAL_IDS);
	}

	static void onGyro() {
		if (Location.inSanctuary || !Config.sanctuaryOnly) {
			unlock("gyro");
		}
	}

	private static void checkKills() {
		thresholds(Math.max(Config.killCount, Config.bestiaryKills), KILL_AT, KILL_IDS);
	}

	private static void checkSession() {
		if (ArachneTracker.sessionKills > 0) {
			long len = Math.max(ArachneTracker.sessionLastKillMs, System.currentTimeMillis()) - ArachneTracker.sessionFirstKillMs;

			thresholds(len, SESSION_AT, SESSION_IDS);
		}
	}

	private static int tickCount = 0;

	static void tick() {
		if (++tickCount % 100 == 0) {
			checkKills();
			checkSession();
		}
		closeChatWindow(false);
		if (CpsCounter.left() >= 10) {
			unlock("cps10");
		}
		if (CpsCounter.left() >= 20) {
			unlock("cps20");
		}
	}

	private static final ArrayDeque<String> RECENT = new ArrayDeque<>();
	private static long windowUntil = 0;
	private static boolean windowOpen = false;
	private static long ownDamage = -1;
	private static int ownRank = -1;
	private static boolean sawFinal = false;
	private static final Set<String> entries = new HashSet<>();
	private static int entryIndex = 0;

	private static final Pattern ENTRY = Pattern.compile(
			"(?:#?(\\d{1,2})[.):]?\\s+)?(?:\\[[^\\]]*\\]\\s*)*([A-Za-z0-9_]{3,16})[^0-9A-Za-z_]{1,8}(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmM]?)\\b");
	private static final Pattern YOURS = Pattern.compile(
			"(?:your|you).{0,25}damage[^0-9]{0,12}(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmM]?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern POSITION = Pattern.compile("position\\s*#\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

	private static long parseNum(String num, String suffix) {
		try {
			double v = Double.parseDouble(num.replace(",", ""));
			if (suffix.equalsIgnoreCase("k")) {
				v *= 1_000;
			} else if (suffix.equalsIgnoreCase("m")) {
				v *= 1_000_000;
			}
			return (long) v;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static boolean playerChat(String text) {
		String t = text.trim();
		String l = t.toLowerCase(Locale.ROOT);
		if (l.startsWith("party >") || l.startsWith("guild >") || l.startsWith("officer >") || l.startsWith("co-op >")
				|| l.startsWith("from ") || l.startsWith("to ") || l.startsWith("[ap]")) {
			return true;
		}
		return t.matches("^(?:\\[[^\\]]+\\]\\s*)*(?:[^\\w\\s]\\s*)?(?:\\[[^\\]]+\\]\\s*)*[A-Za-z0-9_]{3,16}: .*");
	}

	static void chat(String text) {
		if (playerChat(text)) {
			return;
		}
		synchronized (RECENT) {
			RECENT.addLast(text);
			while (RECENT.size() > 80) {
				RECENT.removeFirst();
			}
		}
		String lower = text.toLowerCase(Locale.ROOT);
		Minecraft mc = Minecraft.getInstance();
		String me = mc.player == null ? "" : mc.player.getName().getString();
		long now = System.currentTimeMillis();
		slayerChat(lower, now);
		soloChat(text, me);

		if (!windowOpen && lower.contains("arachne")
				&& (lower.contains("down") || lower.contains("defeated") || lower.contains("slain") || lower.contains("damage"))) {
			openChatWindow();
		}
		if (!windowOpen) {
			return;
		}
		windowUntil = Math.max(windowUntil, now + 4_000);

		if ((lower.contains("final blow") || lower.contains("final hit") || lower.contains("killing blow") || lower.contains("last hit"))
				&& (lower.contains(me.toLowerCase(Locale.ROOT)) || lower.contains("you"))) {
			sawFinal = true;
		}
		Matcher y = YOURS.matcher(text);
		if (y.find()) {
			long v = parseNum(y.group(1), y.group(2));
			if (v >= 0) {
				ownDamage = v;
			}

			Matcher pos = POSITION.matcher(text);
			if (pos.find()) {
				ownRank = Integer.parseInt(pos.group(1));
			}
		}
		Matcher m = ENTRY.matcher(text);
		while (m.find()) {
			String name = m.group(2);
			if (name.equalsIgnoreCase("arachne") || name.equalsIgnoreCase("damage") || name.equalsIgnoreCase("total")) {
				continue;
			}
			long v = parseNum(m.group(3), m.group(4));
			if (v < 1000) {
				continue;
			}
			entryIndex++;
			entries.add(name.toLowerCase(Locale.ROOT));
			if (!me.isEmpty() && name.equalsIgnoreCase(me)) {
				ownDamage = v;
				ownRank = m.group(1) != null ? Integer.parseInt(m.group(1)) : entryIndex;
			}
		}
	}

	private static long lastT5Line = 0;
	private static long lastSlayerDone = 0;

	private static boolean inArachneSession() {
		return Location.inSanctuary || ArachneTracker.sessionKills > 0;
	}

	private static void slayerChat(String lower, long now) {
		if (lower.contains("tarantula broodfather v") && !lower.contains("tarantula broodfather vi")) {
			lastT5Line = now;
			if (now - lastSlayerDone < 10_000 && inArachneSession()) {
				unlock("multi");
			}
		}
		if (lower.contains("slayer quest complete") || lower.contains("slayer boss slain")) {
			lastSlayerDone = now;
			if (lower.contains("slayer quest complete") && inArachneSession()) {
				unlock("multi");
			} else if (now - lastT5Line < 20_000 && inArachneSession()) {
				unlock("multi");
			}
		}
		if (lower.contains("you were killed by arachne")) {
			unlock("bozo");
		}

		boolean dropLine = (lower.contains("rare drop") || lower.contains("rngesus") || lower.contains("drop")) && !lower.contains(": ");
		if (dropLine && (Location.inSanctuary || inArachneSession())) {
			if (lower.contains("shrivelled wasp") || lower.contains("shriveled wasp")) {
				unlock("wasp");
			}
			if (lower.contains("primordial eye")) {
				unlock("eye");
			}
		}
	}

	private static void openChatWindow() {
		if (windowOpen) {
			return;
		}
		windowOpen = true;
		windowUntil = System.currentTimeMillis() + 20_000;
		ownDamage = -1;
		ownRank = -1;
		sawFinal = false;
		entries.clear();
		entryIndex = 0;
	}

	private static void closeChatWindow(boolean force) {
		if (!windowOpen || (!force && System.currentTimeMillis() < windowUntil)) {
			return;
		}
		windowOpen = false;
		if (sawFinal) {
			Config.finalHits++;
			Config.save();
			if (Config.finalHits >= 1000) {
				unlock("final1000");
			}
		}
		if (ownDamage >= 0) {
			thresholds(ownDamage, new long[] {500_000, 750_000, 1_000_000}, new String[] {"d500", "d750", "d1m"});
			if (ownDamage < 10_000 && ownRank >= 1 && ownRank <= 5) {
				unlock("free");
			}
		}
	}

	static String dumpChat() {
		try {
			java.nio.file.Path p = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("arachneplus-chat.txt");
			List<String> lines;
			synchronized (RECENT) {
				lines = new ArrayList<>(RECENT);
			}
			java.nio.file.Files.write(p, lines);
			return p.toString();
		} catch (Exception e) {
			return null;
		}
	}
}
