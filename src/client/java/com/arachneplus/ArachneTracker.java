package com.arachneplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class ArachneTracker {
	static final int SCAN_EVERY_TICKS = 5;

	static final int RESET_AFTER_TICKS = 100;

	static final int EMPTY_SCANS_TO_END_BROODS = 3;

	static final int[] BROOD_TOTALS = {0, 0, 10, 0, 20, 0};

	static int phase = 0;
	static int broodsAlive = 0;
	static int broodsTotal = 0;

	static String lastScan = "";

	static String lastChat = "";

	static long fightStartNs = 0;
	static long fightLastSeenNs = 0;

	static long fightEndNs = 0;

	private static long hopSetMs = 0;

	static boolean hopOn() {
		return hopSetMs != 0 && System.currentTimeMillis() - hopSetMs < 30 * 60_000L;
	}

	static void setHop() {
		hopSetMs = System.currentTimeMillis();
		ArachnePlusClient.say("HOP: the PLACE alert will say HOP for this boss");
	}
	private static boolean killRecorded = false;

	private static String pendingMsg = null;
	private static long pendingDeadlineMs = 0;
	private static long pendingReleaseMs = 0;
	private static long awaitBorderUntilMs = 0;

	public static boolean interceptBorder(net.minecraft.network.chat.Component line) {
		if (pendingMsg == null || awaitBorderUntilMs == 0 || System.currentTimeMillis() > awaitBorderUntilMs) {
			return false;
		}
		String s = line.getString().replaceAll("\u00a7.", "").replaceAll("\\s", "");
		if (s.length() < 10 || !s.matches("[-=\u25AC_]+")) {
			return false;
		}
		String m = pendingMsg;
		pendingMsg = null;
		awaitBorderUntilMs = 0;
		ArachnePlusClient.sayCentered(m);
		var mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(line);
		}
		return true;
	}

	static long recentUntilNs = 0;

	static long spawnEndNs = 0;

	static boolean altarKnown = false;
	static double altarX, altarY, altarZ;

	static boolean bossPresent = false;

	static Entity bossBody = null;

	static double bossTagX, bossTagY, bossTagZ;

	private static List<Entity> glowTargets = new ArrayList<>();

	private static volatile Set<Integer> runicIds = new HashSet<>();

	private static Map<Integer, Integer> runicFlip = new HashMap<>();

	private static final Set<Integer> seenBefore = new HashSet<>();

	public static boolean isRunic(Entity e) {
		return Config.runicGlow && Config.active() && runicIds.contains(e.getId());
	}

	static String lastTagInfo = "";

	private static boolean isPurple(int c) {
		int r = (c >> 16) & 255;
		int g = (c >> 8) & 255;
		int b = c & 255;
		return r >= 0x80 && b >= 0x80 && g <= 0x80 && Math.abs(r - b) <= 0x60;
	}

	private static boolean hasPurpleName(Component name) {
		boolean[] purple = {false};
		name.visit((style, str) -> {
			String t = str.trim();
			if (t.isEmpty()) {
				return java.util.Optional.empty();
			}
			boolean letters = false;
			for (int i = 0; i < t.length(); i++) {
				if (Character.isLetter(t.charAt(i))) {
					letters = true;
					break;
				}
			}
			if (!letters) {
				return java.util.Optional.empty();
			}
			String low = t.toLowerCase(Locale.ROOT);
			if (low.contains("runic")) {
				purple[0] = true;
			}

			if ((low.contains("brood") || low.contains("arachne")) && style.getColor() != null
					&& isPurple(style.getColor().getValue())) {
				purple[0] = true;
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return purple[0];
	}

	private static String describe(Component name) {
		StringBuilder sb = new StringBuilder();
		name.visit((style, str) -> {
			if (!str.isBlank()) {
				sb.append(str.trim()).append('#')
						.append(style.getColor() == null ? "none" : String.format("%06X", style.getColor().getValue()))
						.append(' ');
			}
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return sb.toString().trim();
	}

	private static final Set<Entity> glowed = new HashSet<>();

	private static int tickCounter = 0;
	private static int absentTicks = 0;
	private static int emptyScans = 0;

	private ArachneTracker() {}

	static void reset() {
		phase = 0;
		broodsAlive = 0;
		broodsTotal = 0;
		absentTicks = 0;
		emptyScans = 0;
		enterScans = 0;
		overflowScans = 0;
		advancedFrom = 0;
		bossPresent = false;
		bossBody = null;
		fightStartNs = 0;
		fightEndNs = 0;
		killRecorded = false;
		placePending = false;
		java.util.Arrays.fill(phaseStartNs, 0L);
		markedPhase = 0;
		glowTargets = new ArrayList<>();
		clearGlow();
	}

	static void clearSpawn() {
		spawnEndNs = 0;
		spawnVerdict = 0;
		spawnExtended = false;
	}

	private static long lastSpawnStartNs = 0;

	static long spawnStartNs = 0;

	static int spawnVerdict = 0;

	static boolean spawnExtended = false;

	private static long observeUntilNs = 0;

	private static long lastDarkNs = 0;
	private static long darkWindowStartNs = 0;
	private static int darkHits = 0;

	static String lastParticle = "";

	private static final long WATCH_NS = 9_000_000_000L;
	private static final long LOOKBACK_NS = 3_000_000_000L;

	private static double riseBaseY = Double.NaN;

	static void startSpawnTimer() {
		long now = System.nanoTime();
		lastSpawnStartNs = now;
		spawnStartNs = now;
		spawnVerdict = 0;
		spawnExtended = false;
		observeUntilNs = now + WATCH_NS;
		riseBaseY = Double.NaN;
		spawnEndNs = now + 22_000_000_000L;
		Config.spawnParticles = false;
		if (Config.spawnMode == Config.SPAWN_QUICK) {

			spawnVerdict = 1;
			observeUntilNs = 0;
			return;
		}
		if (Config.spawnMode == Config.SPAWN_NO_QUICK) {
			spawnVerdict = 2;
			observeUntilNs = 0;
			spawnEndNs = now + 41_000_000_000L;
			Config.spawnParticles = true;
			return;
		}
		if (lastDarkNs != 0 && now - lastDarkNs < LOOKBACK_NS && darkHits >= 3) {
			markParticles();
		}
	}

	private static void markParticles() {
		spawnVerdict = 2;
		spawnEndNs = spawnStartNs + 41_000_000_000L;
		Config.spawnParticles = true;
	}

	static long spawnTotalMs() {
		if (spawnExtended) {
			return 18000;
		}
		return spawnVerdict == 2 ? 41000 : 22000;
	}

	private static boolean isDarkParticle(String key) {
		for (String k : new String[] {"smoke", "squid_ink", "ash", "soul", "dust", "entity_effect", "witch",
				"dragon_breath", "sculk"}) {
			if (key.contains(k)) {
				return true;
			}
		}
		return false;
	}

	public static void particleSeen(String key, double x, double y, double z, double xa, double ya, double za) {
		if (!Config.active() || !isDarkParticle(key)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		double cx = altarKnown ? altarX : mc.player.getX();
		double cy = altarKnown ? altarY : mc.player.getY();
		double cz = altarKnown ? altarZ : mc.player.getZ();
		double dx = x - cx;
		double dz = z - cz;
		double dy = y - cy;
		double reach = altarKnown ? 7.0 : 12.0;
		if (dx * dx + dz * dz > reach * reach || dy < -3 || dy > 10) {
			return;
		}
		long now = System.nanoTime();
		double dist = Math.sqrt(dx * dx + dz * dz);
		lastParticle = key + " " + String.format("%.1f", dist) + "m up " + String.format("%.1f", dy) + " v " + String.format("%.2f %.2f %.2f", xa, ya, za);

		boolean inward = dist > 0.3 && (-(dx * xa + dz * za) / dist) > 0.01;
		if (Double.isNaN(riseBaseY) || now > observeUntilNs) {
			riseBaseY = y;
		}
		boolean movingUp = ya > 0.01 && dist < 3.0;
		boolean risen = dist < 3.5 && y - riseBaseY > 1.2;
		boolean rising = movingUp || risen;

		if (!(inward || rising)) {
			return;
		}
		lastDarkNs = now;
		if (now - darkWindowStartNs > 1_500_000_000L) {
			darkWindowStartNs = now;
			darkHits = 0;
		}

		darkHits += (inward || movingUp) ? 2 : 1;
		if (spawnEndNs != 0 && spawnVerdict == 0 && now < observeUntilNs && darkHits >= 3) {
			markParticles();
		}
	}

	private static int ownPlaced = 0;
	private static boolean placerAutoDone = false;

	private static boolean placePending = false;
	private static InteractionHand placeHand = InteractionHand.MAIN_HAND;
	private static int placeCount = 0;
	private static String placeName = "";
	private static int placeTicks = 0;
	private static int placeBX, placeBY, placeBZ;

	private static final int PLACE_TIMEOUT_TICKS = 80;

	static void crystalUsed(Player player, InteractionHand hand, int bx, int by, int bz) {
		if (placePending) {
			return;
		}
		ItemStack stack = player.getItemInHand(hand);
		placePending = true;
		placeHand = hand;
		placeCount = stack.getCount();
		placeName = stack.getHoverName().getString();
		placeTicks = 0;
		placeBX = bx;
		placeBY = by;
		placeBZ = bz;
	}

	static void chatSeen(String text) {
		Achievements.chat(text);
		PartyCommands.chat(text);
		if (!Config.active()) {
			return;
		}
		String t = text.toLowerCase(Locale.ROOT);
		if (pendingMsg != null && t.contains("your damage")) {

			long nowMs = System.currentTimeMillis();
			awaitBorderUntilMs = nowMs + 1500;
			pendingReleaseMs = nowMs + 1500;
		}
		if (t.contains("arachne down!") && !t.contains(":")) {
			bossDied();
		}
		boolean placedMsg = (t.contains("arachne") && t.contains("placed")) || t.contains("something is awakening");
		if (!placedMsg) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		boolean mine = mc.player != null && t.contains(mc.player.getName().getString().toLowerCase(Locale.ROOT));
		if (mine) {
			ownPlaced++;
			if (ownPlaced >= 10 && !placerAutoDone) {
				placerAutoDone = true;
				if (!Config.profitPlacer) {
					Config.profitPlacer = true;
					Config.save();
					ArachnePlusClient.say("You have placed 10 crystals, so the profit box is now using the Placer version (change it in Stats)");
				}
			}
		}
		if (placePending && mine) {
			confirmPlacement();
		} else {
			othersPlaced();
		}
	}

	private static void confirmPlacement() {
		placePending = false;
		Achievements.onCrystal();
		startSpawnTimerAtBlock(placeBX, placeBY, placeBZ);
	}

	private static void checkPlacement(Minecraft mc) {
		if (!placePending) {
			return;
		}

		if (++placeTicks > PLACE_TIMEOUT_TICKS) {
			placePending = false;
		}
	}

	static void startSpawnTimerAtBlock(int bx, int by, int bz) {
		double cx = bx + 0.5;
		double cz = bz + 0.5;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			var clicked = mc.level.getBlockState(new net.minecraft.core.BlockPos(bx, by, bz)).getBlock();
			double sumX = 0;
			double sumZ = 0;
			int squares = 0;
			for (int ox = bx - 1; ox <= bx; ox++) {
				for (int oz = bz - 1; oz <= bz; oz++) {
					boolean ok = true;
					for (int i = 0; i < 2 && ok; i++) {
						for (int j = 0; j < 2 && ok; j++) {
							var b = mc.level.getBlockState(new net.minecraft.core.BlockPos(ox + i, by, oz + j)).getBlock();
							ok = b == clicked;
						}
					}
					if (ok) {
						sumX += ox + 1.0;
						sumZ += oz + 1.0;
						squares++;
					}
				}
			}
			if (squares > 0) {
				cx = sumX / squares;
				cz = sumZ / squares;
			}
		}
		startSpawnTimerAt(cx, by + 1.0, cz);
	}

	static void startSpawnTimerAt(double x, double y, double z) {
		altarKnown = true;
		altarX = x;
		altarY = y;
		altarZ = z;
		Config.altarSet = true;
		Config.altarX = x;
		Config.altarY = y;
		Config.altarZ = z;
		Config.save();
		startSpawnTimer();
	}

	static void loadAltar() {
		if (Config.altarSet) {
			altarKnown = true;
			altarX = Config.altarX;
			altarY = Config.altarY;
			altarZ = Config.altarZ;
		}
	}

	private static void othersPlaced() {
		long now = System.nanoTime();
		if (spawnEndNs != 0 && now < spawnEndNs) {
			return;
		}
		if (now - lastSpawnStartNs < 5_000_000_000L) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (altarKnown && mc.player != null) {
			double dx = mc.player.getX() - altarX;
			double dy = mc.player.getY() - altarY;
			double dz = mc.player.getZ() - altarZ;
			if (dx * dx + dy * dy + dz * dz > 50.0 * 50.0) {
				return;
			}
		}
		startSpawnTimer();
	}

	static long spawnRemainingMs() {
		return (spawnEndNs - System.nanoTime()) / 1_000_000L;
	}

	static long bossElapsedMs() {
		if (phase == 0 || fightStartNs == 0) {
			return 0;
		}
		long end = fightEndNs != 0 ? fightEndNs : System.nanoTime();
		return Math.max(0L, (end - fightStartNs) / 1_000_000L);
	}

	static boolean fightActive() {
		return phase > 0 || spawnEndNs != 0 || System.nanoTime() < recentUntilNs;
	}

	static int sessionKills = 0;
	static long sessionFirstKillMs = 0, sessionLastKillMs = 0;

	static double killsPerHour() {
		if (sessionKills < 2 || sessionLastKillMs <= sessionFirstKillMs) {
			return -1;
		}
		return (sessionKills - 1) * 3_600_000.0 / (sessionLastKillMs - sessionFirstKillMs);
	}

	static final long SESSION_TIMEOUT_MS = 15 * 60_000L;

	static long sessionKillMs = 0, sessionLastMs = 0, sessionBestMs = 0;

	static int sessionBoards = 0;
	static long sessionPosSum = 0;
	static double sessionDamageSum = 0;

	static void recordBoard(int position, double damage) {
		sessionBoards++;
		if (damage > Config.maxDamage) {
			Config.maxDamage = (long) damage;
			Config.save();
		}
		sessionPosSum += position;
		sessionDamageSum += damage;
	}

	static void resetSession() {
		sessionBoards = 0;
		sessionPosSum = 0;
		sessionDamageSum = 0;
		sessionKillMs = 0;
		sessionLastMs = 0;
		sessionBestMs = 0;
		sessionKills = 0;
		sessionFirstKillMs = 0;
		sessionLastKillMs = 0;
	}

	static void sessionTick() {
		if (pendingMsg != null) {
			long nowMs = System.currentTimeMillis();
			long release = pendingReleaseMs != 0 ? pendingReleaseMs : pendingDeadlineMs;
			if (nowMs >= release) {
				String m = pendingMsg;
				pendingMsg = null;
				ArachnePlusClient.sayCentered(m);
			}
		}
		if (sessionKills > 0 && System.currentTimeMillis() - sessionLastKillMs > SESSION_TIMEOUT_MS) {
			resetSession();
		}
	}

	static void bossDied() {
		if (phase == 0 || fightStartNs == 0 || fightEndNs != 0) {
			return;
		}
		fightEndNs = System.nanoTime();
		recordKill(fightEndNs);
	}

	private static void recordKill(long endNs) {
		long ms = (endNs - fightStartNs) / 1_000_000L;
		if (killRecorded || fightStartNs == 0 || ms < 3000) {
			return;
		}
		killRecorded = true;
		hopSetMs = 0;

		boolean timed = Location.sinceEnteredMs() >= 60_000L;
		captureSplits(endNs, timed);
		long addMs = ms;
		if (timed) {
			pendingMsg = "Arachne boss took (" + String.format(java.util.Locale.ROOT, "%.1f", ms / 1000.0) + "s)!";
			pendingDeadlineMs = System.currentTimeMillis() + 8000;
			pendingReleaseMs = 0;
			awaitBorderUntilMs = 0;
		} else {

			addMs = Config.killCount > 0 ? Config.killTotalMs / Config.killCount : 0;
		}
		Config.killCount++;
		long nowMs = System.currentTimeMillis();
		if (sessionKills == 0) {
			sessionFirstKillMs = nowMs;
		}
		sessionKills++;
		if (Config.showProfit && !Config.profitNoticeShown) {
			Config.profitNoticeShown = true;
			ArachnePlusClient.say("Profit estimate: this does NOT track real drops, it uses averages from the available drop chances. Scavenger coins are ignored."
					// dont touch this
					+ (Config.profitPlacer ? " Placer: the extra 12 soul string is included, but the cost of crystals is NOT deducted." : ""));
		}
		sessionLastKillMs = nowMs;
		if (sessionLastKillMs - sessionFirstKillMs > Config.longestSessionMs) {
			Config.longestSessionMs = sessionLastKillMs - sessionFirstKillMs;
		}
		sessionKillMs += addMs;
		Config.killTotalMs += addMs;
		if (timed) {
			sessionLastMs = ms;
			sessionBestMs = sessionBestMs == 0 ? ms : Math.min(sessionBestMs, ms);
			Config.lastKillMs = ms;
			Config.bestKillMs = Config.bestKillMs == 0 ? ms : Math.min(Config.bestKillMs, ms);
		}
		Config.save();
		recentUntilNs = System.nanoTime() + 30_000_000_000L;
		BestiaryTracker.countKill();
		BestiaryTracker.requestSoon();
		Achievements.onKill(ms, timed);
	}

	// Dory was here
	static void tick(Minecraft mc) {
		if (!Config.active() || mc.level == null || mc.player == null) {
			reset();
			clearSpawn();
			return;
		}

		if (spawnEndNs != 0 && System.nanoTime() > spawnEndNs + 3_000_000_000L) {
			clearSpawn();
		}
		checkPlacement(mc);
		if (spawnEndNs != 0 && spawnVerdict == 0 && System.nanoTime() >= observeUntilNs) {
			spawnVerdict = 1;
		}

		if (spawnEndNs != 0 && spawnVerdict == 1 && !spawnExtended && phase == 0 && !bossPresent
				&& System.nanoTime() > spawnEndNs + 1_500_000_000L) {
			spawnExtended = true;
			spawnVerdict = 2;
			Config.spawnParticles = true;
			spawnEndNs = System.nanoTime() + 18_000_000_000L;
		}
		applyGlow();
		if (++tickCounter % SCAN_EVERY_TICKS != 0) {
			return;
		}
		scan(mc);
	}

	private static void applyGlow() {
		if (!Config.glow) {
			clearGlow();
			return;
		}
		for (Entity e : glowTargets) {
			if (!e.isRemoved()) {
				setGlow(e, true);
				glowed.add(e);
			}
		}
	}

	private static void clearGlow() {
		if (glowed.isEmpty()) {
			return;
		}
		for (Entity e : glowed) {
			setGlow(e, false);
		}
		glowed.clear();
	}

	private static java.lang.reflect.Method sharedFlagSetter;
	private static boolean sharedFlagFailed = false;

	private static void setGlow(Entity e, boolean on) {
		if (sharedFlagFailed) {
			return;
		}
		try {
			if (sharedFlagSetter == null) {
				sharedFlagSetter = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
				sharedFlagSetter.setAccessible(true);
			}
			sharedFlagSetter.invoke(e, 6, on);
		} catch (ReflectiveOperationException | RuntimeException ex) {
			sharedFlagFailed = true;
			ArachnePlusClient.LOGGER.warn("Could not switch on glow: {}", ex.toString());
		}
	}

	private static boolean isSpider(Entity e) {
		EntityType<?> t = e.getType();
		return t == EntityTypes.SPIDER || t == EntityTypes.CAVE_SPIDER;
	}

	private static boolean isMob(Entity e) {
		return e instanceof LivingEntity && !(e instanceof Player) && e.getType() != EntityTypes.ARMOR_STAND;
	}

	private static boolean isNotBoss(String text) {
		for (String w : new String[] {"keeper", "armor", "armour", "helmet", "chestplate", "leggings", "boots",
				"crystal", "fragment", "altar", "calling", "sword", "dagger", "minion", "pet "}) {
			if (text.contains(w)) {
				return true;
			}
		}
		return false;
	}

	private static List<Entity> slayerTags = new ArrayList<>();

	private static boolean isSlayerText(String text) {
		return text.contains("broodfather") || text.contains("tarantula") || text.contains("vermin") || text.contains("slayer");
	}

	private static boolean nearerSlayerTag(Entity s, double than) {
		for (Entity t : slayerTags) {
			double dx = s.getX() - t.getX();
			double dz = s.getZ() - t.getZ();
			double above = t.getY() - s.getY();
			double d = dx * dx + dz * dz;
			if (d <= 12.0 && above >= -1.5 && above <= 6.0 && d < than) {
				return true;
			}
		}
		return false;
	}

	private static Entity bodyUnder(Entity tag, List<Entity> mobs, List<Entity> broodTags) {
		Entity best = null;
		float bestScale = -1;
		double bestDist = Double.MAX_VALUE;
		for (Entity s : mobs) {
			if (!(s instanceof LivingEntity living) || !isSpider(s)) {
				continue;
			}
			double dx = s.getX() - tag.getX();
			double dz = s.getZ() - tag.getZ();
			double above = tag.getY() - s.getY();
			double d = dx * dx + dz * dz;
			if (d > 16.0 || above < -1.5 || above > 7.0) {
				continue;
			}
			boolean nearerBrood = false;
			for (Entity b : broodTags) {
				double bx = s.getX() - b.getX();
				double bz = s.getZ() - b.getZ();
				if (bx * bx + bz * bz < d) {
					nearerBrood = true;
					break;
				}
			}
			if (nearerBrood || nearerSlayerTag(s, d)) {
				continue;
			}
			float sc = living.getScale();
			if (sc > bestScale + 0.01f || (Math.abs(sc - bestScale) <= 0.01f && d < bestDist)) {
				best = s;
				bestScale = sc;
				bestDist = d;
			}
		}
		return best;
	}

	private static void scan(Minecraft mc) {
		AABB box = mc.player.getBoundingBox().inflate(Config.radius);
		List<Entity> found = mc.level.getEntities(mc.player, box,
				e -> isMob(e) || e.getType() == EntityTypes.ARMOR_STAND || Config.countAllEntities);

		List<Entity> spiders = new ArrayList<>();
		List<Entity> tags = new ArrayList<>();
		List<Entity> broodTags = new ArrayList<>();
		List<Entity> bossCandidates = new ArrayList<>();
		List<Entity> runicTags = new ArrayList<>();
		int broods = 0;

		List<Entity> namedMobs = new ArrayList<>();
		List<Entity> slayerNow = new ArrayList<>();
		for (Entity e : found) {
			if (isMob(e)) {
				spiders.add(e);
				Component own = e.getCustomName();
				if (own != null && own.getString().toLowerCase(Locale.ROOT).contains("arachne")) {
					namedMobs.add(e);
				}
			}
			if (!(e.getType() == EntityTypes.ARMOR_STAND || Config.countAllEntities)) {
				continue;
			}
			Component name = e.getCustomName();
			if (name == null) {
				continue;
			}
			String text = name.getString().toLowerCase(Locale.ROOT);
			if (isSlayerText(text) && !text.contains("arachne")) {
				slayerNow.add(e);
				continue;
			}
			if (!text.contains("arachne")) {
				continue;
			}
			if (text.contains("brood")) {
				if (hasPurpleName(name)) {
					runicTags.add(e);
				}
				if (Config.debug) {
					lastTagInfo = describe(name);
				}
				broods++;
				tags.add(e);
				broodTags.add(e);
			} else if (!isNotBoss(text)) {
				tags.add(e);
				bossCandidates.add(e);
			}
		}

		slayerTags = slayerNow;

		Entity bossTag = null;
		Entity bossBodyFound = null;
		double bossScore = Double.MAX_VALUE;
		for (Entity c : bossCandidates) {
			Entity body = bodyUnder(c, spiders, broodTags);
			String ctext = c.getCustomName().getString().toLowerCase(Locale.ROOT);
			boolean looksLikeMob = ctext.contains("\u2764") || ctext.contains("[lv") || ctext.contains("\ufd3e")
					|| ctext.contains("\ufd3f") || ctext.contains("/") || ctext.contains("miniboss");
			if (body == null && !looksLikeMob) {
				continue;
			}
			double d = c.distanceToSqr(mc.player) + (body == null ? 10000 : 0);
			if (d < bossScore) {
				bossScore = d;
				bossTag = c;
				bossBodyFound = body;
			}
		}
		boolean bossSeen = bossTag != null;

		List<Entity> targets = new ArrayList<>();
		Set<Integer> newRunic = new HashSet<>();
		for (Entity s : spiders) {

			Entity best = null;
			double bestD = Double.MAX_VALUE;
			for (Entity t : tags) {
				double dx = s.getX() - t.getX();
				double dz = s.getZ() - t.getZ();
				double above = t.getY() - s.getY();
				double d = dx * dx + dz * dz;
				if (d <= 12.0 && above >= -1.5 && above <= 6.0 && d < bestD) {
					bestD = d;
					best = t;
				}
			}
			if (best != null && !nearerSlayerTag(s, bestD)) {
				targets.add(s);
				if (runicTags.contains(best)) {
					newRunic.add(s.getId());
				}
			}
		}
		for (Entity m : namedMobs) {
			if (!targets.contains(m)) {
				targets.add(m);
			}
			Component own = m.getCustomName();
			if (own != null && own.getString().toLowerCase(Locale.ROOT).contains("brood") && hasPurpleName(own)) {
				newRunic.add(m.getId());
			}
		}

		boolean glowHere = Location.inSanctuary || Location.lastLocation.isEmpty();
		if (!glowHere) {
			targets.clear();
			newRunic.clear();
		}

		for (Entity old : new ArrayList<>(glowed)) {
			if (!targets.contains(old)) {
				setGlow(old, false);
				glowed.remove(old);
			}
		}
		glowTargets = targets;

		Set<Integer> stable = new HashSet<>();
		Set<Integer> known = new HashSet<>(runicIds);
		known.addAll(newRunic);
		Map<Integer, Integer> keep = new HashMap<>();
		for (Integer id : known) {
			boolean was = runicIds.contains(id);
			boolean now = newRunic.contains(id);
			if (was == now) {
				if (was) {
					stable.add(id);
				}
			} else {
				int n = runicFlip.getOrDefault(id, 0) + 1;
				if (n >= 3 || (!was && !seenBefore.contains(id))) {
					if (now) {
						stable.add(id);
					}
				} else {
					keep.put(id, n);
					if (was) {
						stable.add(id);
					}
				}
			}
		}
		runicFlip = keep;
		seenBefore.addAll(known);
		for (Entity t : targets) {
			seenBefore.add(t.getId());
		}
		if (seenBefore.size() > 400) {
			seenBefore.clear();
		}
		runicIds = stable;

		if (bossSeen && !bossPresent && spawnEndNs != 0 && spawnVerdict == 2 && spawnRemainingMs() > 10_000) {
			spawnEndNs = System.nanoTime() + 5_000_000_000L;
		}
		bossPresent = bossSeen;
		bossBody = bossBodyFound;
		if (bossSeen) {
			bossTagX = bossTag.getX();
			bossTagY = bossTag.getY() - 1.0;
			bossTagZ = bossTag.getZ();
			if (glowHere && bossBodyFound != null && !targets.contains(bossBodyFound)) {
				targets.add(bossBodyFound);
			}
		}

		lastScan = "boss=" + bossSeen + " broods=" + broods + " glowing=" + targets.size();
		update(bossSeen, broods);
	}

	private static int enterScans = 0;

	private static int advancedAtScan = 0;
	private static int scanNo = 0;
	private static int advancedFrom = 0;

	private static final int UNDO_WINDOW_SCANS = 24;

	private static final int UNDO_WINDOW_P3_SCANS = 8;
	private static int overflowScans = 0;

	private static final int EMPTY_SCANS_TO_END = 3;

	private static void startBroodPhase(int p, int broods) {
		phase = p;
		broodsTotal = BROOD_TOTALS[p];
		broodsAlive = Math.min(broods, broodsTotal);
		emptyScans = 0;
		enterScans = 0;
	}

	private static boolean enterBroodPhase(int broods) {
		if (broods <= 0) {
			enterScans = 0;
			return false;
		}
		enterScans++;
		return broods >= 3 ? enterScans >= 2 : enterScans >= 4;
	}

	static final String[] SPLIT_NAMES = {"A1", "B1", "A2", "B2", "A3"};

	private static final long[] phaseStartNs = new long[7];
	private static int markedPhase = 0;

	static final long[] lastSplitsMs = new long[5];
	static boolean splitsValid = false;
	static long lastSplitsTotalMs = 0;

	private static void markPhase() {
		int p = phase;
		if (p < 1 || p > 5 || p == markedPhase) {
			return;
		}
		if (p > markedPhase) {
			phaseStartNs[p] = p == 1 ? fightStartNs : System.nanoTime();
		} else {

			for (int i = p + 1; i <= 5; i++) {
				phaseStartNs[i] = 0;
			}
		}
		markedPhase = p;
	}

	private static void captureSplits(long endNs, boolean timed) {
		markPhase();
		java.util.Arrays.fill(lastSplitsMs, 0L);
		splitsValid = false;
		if (!timed) {
			return;
		}
		for (int i = 1; i <= 5; i++) {
			if (phaseStartNs[i] == 0) {
				continue;
			}
			long next = endNs;
			for (int j = i + 1; j <= 5; j++) {
				if (phaseStartNs[j] != 0) {
					next = phaseStartNs[j];
					break;
				}
			}
			lastSplitsMs[i - 1] = Math.max(0L, (next - phaseStartNs[i]) / 1_000_000L);
		}
		lastSplitsTotalMs = Math.max(0L, (endNs - fightStartNs) / 1_000_000L);
		splitsValid = true;
	}

	static String splitsText(String between) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			if (lastSplitsMs[i] <= 0) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(between);
			}
			sb.append(SPLIT_NAMES[i]).append(' ').append(String.format(java.util.Locale.ROOT, "%.1fs", lastSplitsMs[i] / 1000.0));
		}
		return sb.toString();
	}

	private static void update(boolean bossSeen, int broods) {
		updateInner(bossSeen, broods);
		markPhase();
	}

	private static void updateInner(boolean bossSeen, int broods) {
		scanNo++;
		if (!bossSeen && broods == 0 && phase != 0 && phase != 2 && phase != 4) {
			absentTicks += SCAN_EVERY_TICKS;
			if (absentTicks >= RESET_AFTER_TICKS) {
				if (phase == 5) {
					recordKill(fightLastSeenNs);
				}
				reset();
			}
			return;
		}
		if (!bossSeen && broods == 0 && phase == 0) {
			return;
		}
		absentTicks = 0;
		fightLastSeenNs = System.nanoTime();

		switch (phase) {
			case 0 -> {
				phase = 1;
				enterScans = 0;

				fightStartNs = fightLastSeenNs + 5_000_000_000L;
			}
			case 1 -> {
				if (enterBroodPhase(broods)) {
					startBroodPhase(2, broods);
				}
			}
			case 2, 4 -> {

				if (phase == 2 && broods > BROOD_TOTALS[2]) {
					if (++overflowScans >= 2) {
						startBroodPhase(4, broods);
						overflowScans = 0;
						return;
					}
				} else {
					overflowScans = 0;
				}
				if (broods > 0) {
					broodsAlive = Math.min(broods, broodsTotal);
					emptyScans = 0;
				} else if (++emptyScans >= EMPTY_SCANS_TO_END) {
					broodsAlive = 0;
					advancedFrom = phase;
					advancedAtScan = scanNo;
					phase++;
					emptyScans = 0;
					enterScans = 0;
				}
			}
			case 3, 5 -> {
				int window = phase == 3 ? UNDO_WINDOW_P3_SCANS : UNDO_WINDOW_SCANS;
				boolean justEnded = advancedFrom == phase - 1 && scanNo - advancedAtScan < window;

				boolean flicker = phase == 5 || broods <= BROOD_TOTALS[2];
				if (justEnded && broods > 0 && flicker) {

					startBroodPhase(phase - 1, broods);
				} else if (phase == 3 && enterBroodPhase(broods)) {
					startBroodPhase(4, broods);
				}
			}
			default -> { }
		}
	}
}
