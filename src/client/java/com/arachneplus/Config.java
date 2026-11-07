package com.arachneplus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class Config {

	public static final int PHASE = 0;
	public static final int BOSS_TIMER = 1;
	public static final int AVG_TIMER = 2;
	public static final int SPAWN_TIMER = 3;
	public static final int CPS = 4;
	public static final int BESTIARY = 5;
	public static final int PROFIT = 6;
	public static final int PLACE = 7;
	public static final int PANEL_COUNT = 8;
	static final String[] PANEL_KEYS = {"phase", "bossTimer", "avgTimer", "spawnTimer", "cps", "bestiary", "profit", "place"};
	static final int[] DEFAULT_X = {6, 6, 6, 6, 6, 6, 6, -1};
	static final int[] DEFAULT_Y = {40, 94, 112, 146, 174, 202, 240, -1};

	public static final int[] posX = DEFAULT_X.clone();
	public static final int[] posY = DEFAULT_Y.clone();

	public static final float[] scale = {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
	public static final boolean[] background = {true, true, true, true, true, true, true, true};

	public static final boolean[] centered = {false, false, false, false, false, false, false, true};

	public static final int[] textColor = new int[PANEL_COUNT];
	public static final int[] letterSpacing = new int[PANEL_COUNT];
	public static final boolean[] bold = new boolean[PANEL_COUNT];
	public static final boolean[] italic = new boolean[PANEL_COUNT];
	public static final boolean[] strike = new boolean[PANEL_COUNT];

	public static final String[] COLOR_NAMES = {"Default", "White", "Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple", "Pink", "Grey"};
	public static final int[] COLOR_VALUES = {0, 0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFB000FF, 0xFFFF55FF, 0xFFAAAAAA};

	public static int lineThickness = 2;

	public static int lineColor = 0xFFFF2A2A;

	public static int lineStyle = 0;

	public static int dotColor = 0xFFFFFF55;

	public static final int SPAWN_AUTO = 0;
	public static final int SPAWN_QUICK = 1;
	public static final int SPAWN_NO_QUICK = 2;

	public static int spawnMode = SPAWN_QUICK;

	public static boolean showProfit = true;

	public static boolean profitClassic = false;
	public static boolean profitPlacer = false;

	public static int avgView = 0;

	public static int placeColorA = 0xFFFF2A2A;
	public static int placeColorB = 0xFFFFE040;

	public static int placeShadow = 0xFF000000;
	public static int placeBangs = 0;

	public static int placeFont = 0;

	public static int placeSize = 100;

	public static boolean placeRandom = false;
	public static int placeVariant = 0;
	public static PlaceStyles.Style placeStyle() {
		return new PlaceStyles.Style(placeColorA, placeColorB, placeShadow, placeBangs, placeFont, placeSize);
	}

	public static boolean profitNoticeShown = false;

	public static boolean welcomeShown = false;
	public static boolean partyFact = true;
	public static boolean party8ball = true;

	public static String achievements = "";

	public static String achievementsNatural = "";

	public static int celebration = 75;

	public static boolean partyWarp = false;
	public static boolean partyTransfer = false;
	public static boolean partyPromote = false;
	public static boolean partyKills = true;
	public static boolean partyGyro = true;
	public static boolean partyGoon = true;
	public static boolean partyAch = true;
	public static boolean partyAc = true;
	public static boolean partyArachne = true;
	public static boolean partyPb = true;
	public static boolean partyTotal = true;
	public static boolean partySkills = true;
	public static boolean partySession = true;
	public static boolean partyCoins = true;
	public static boolean partyEssence = true;
	public static boolean partyPbTime = true;
	public static boolean partyDamage = true;

	public static long longestSessionMs = 0;
	public static long maxDamage = 0;
	public static boolean partyHop = true;
	public static boolean partySplit = true;
	public static boolean partyTop = true;
	public static boolean partyJoinCheck = true;

	public static boolean darkGui = true;
	public static boolean partyRank = true;
	public static boolean rankOptIn = false;
	public static String rankUrl = "";
	public static boolean updateAlerts = true;
	public static String lastUpdateSeen = "";
	public static boolean showSplits = true;

	public static boolean partyShareAch = true;
	public static boolean partyAlways = false;

	public static int broodKills = -1;
	public static int crystalsPlaced = 0;
	public static int finalHits = 0;

	public static boolean silenceArachne = false;

	public static int profitView = 0;

	public static boolean profitShowTotals = true;
	public static final String[] PROFIT_VIEWS = {"Per hour", "Total (bestiary)", "Session"};
	public static long profitPerKill() {
		return (profitClassic ? 250_000L : 180_000L) + (profitPlacer ? 35_000L : 0L) + (profitClassic ? EssenceCalc.coinsPerKill() : 0L);
	}

	public static boolean showBestiary = true;

	public static String apiKey = "";

	public static int bestiaryKills = -1;

	public static final String DISCORD_DEFAULT = "https://discord.gg/5yh3sJGTw";
	public static String discordUrl = DISCORD_DEFAULT;

	public static boolean minimalPhase = false;
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 3.0f;

	public static boolean enabled = true;
	public static boolean showPhase = true;
	public static boolean showBroods = true;
	public static boolean glow = true;
	public static boolean runicGlow = true;
	public static boolean tracer = true;

	public static boolean placer = true;
	public static boolean showBossTimer = true;
	public static boolean showAvgTimer = true;
	public static boolean showSpawnTimer = true;

	public static int spawnDisplay = 3;

	public static boolean holoScaleWithDistance = false;
	public static int holoSize = 100;
	public static boolean holoBackground = true;

	public static boolean holoMinimal = false;
	public static final String[] SPAWN_DISPLAY_NAMES = {"None", "GUI only", "Hologram only", "Both"};
	public static boolean spawnGui() {
		return showSpawnTimer && (spawnDisplay & 1) != 0;
	}
	public static boolean spawnHolo() {
		return showSpawnTimer && (spawnDisplay & 2) != 0;
	}
	public static boolean showCps = true;

	public static boolean sanctuaryOnly = false;

	public static String sanctuaryText = "arachne's sanctuary";

	public static boolean hideMobNames = false;

	public static boolean minimalBroodTags = false;
	public static boolean hideDamageNumbers = false;
	public static boolean hideParticles = false;

	public static boolean hidePlayerNames = false;

	public static int playerShrink = 1;

	public static boolean altarSet = false;
	public static double altarX, altarY, altarZ;

	public static boolean spawnParticles = false;

	public static int killCount = 0;
	public static long killTotalMs = 0;
	public static long lastKillMs = 0;
	public static long bestKillMs = 0;

	public static boolean debug = false;

	public static boolean countAllEntities = false;

	public static int radius = 48;

	private Config() {
	}

	public static boolean active() {
		return enabled && (!sanctuaryOnly || Location.inSanctuary);
	}

	public static void disableEverything() {
		enabled = false;
		showPhase = showBroods = glow = runicGlow = tracer = placer = false;
		showBossTimer = showAvgTimer = showSpawnTimer = showCps = showBestiary = showProfit = false;
		spawnDisplay = 0;
		hideMobNames = hideDamageNumbers = hideParticles = hidePlayerNames = silenceArachne = false;
		playerShrink = 1;
		save();
	}

	public static void enableEverything() {
		enabled = true;
		showPhase = showBroods = glow = runicGlow = tracer = placer = true;
		showBossTimer = showAvgTimer = showSpawnTimer = showCps = showBestiary = showProfit = true;
		if (spawnDisplay == 0) {
			spawnDisplay = 3;
		}
		save();
	}

	public static void resetPositions() {
		System.arraycopy(DEFAULT_X, 0, posX, 0, PANEL_COUNT);
		System.arraycopy(DEFAULT_Y, 0, posY, 0, PANEL_COUNT);
		for (int i = 0; i < PANEL_COUNT; i++) {
			scale[i] = 1f;
			background[i] = true;
			centered[i] = i == PLACE;
			textColor[i] = 0;
			letterSpacing[i] = 0;
			bold[i] = false;
			italic[i] = false;
			strike[i] = false;
		}
	}

	public static void resetStats() {
		killCount = 0;
		killTotalMs = 0;
		lastKillMs = 0;
		bestKillMs = 0;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("arachneplus.properties");
	}

	public static void load() {
		Path file = file();
		Properties props = new Properties();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				ArachnePlusClient.LOGGER.warn("Could not read {}: {}", file, e.toString());
			}
		}

		for (int i = 0; i < PANEL_COUNT; i++) {
			posX[i] = readInt(props, PANEL_KEYS[i] + "X", posX[i]);
			posY[i] = readInt(props, PANEL_KEYS[i] + "Y", posY[i]);
			scale[i] = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
					readInt(props, PANEL_KEYS[i] + "Scale", Math.round(scale[i] * 100)) / 100f));
			background[i] = readBool(props, PANEL_KEYS[i] + "Bg", background[i]);
			centered[i] = readBool(props, PANEL_KEYS[i] + "Center", centered[i]);
			textColor[i] = readInt(props, PANEL_KEYS[i] + "Color", textColor[i]);
			letterSpacing[i] = Math.max(0, Math.min(6, readInt(props, PANEL_KEYS[i] + "Spacing", letterSpacing[i])));
			bold[i] = readBool(props, PANEL_KEYS[i] + "Bold", bold[i]);
			italic[i] = readBool(props, PANEL_KEYS[i] + "Italic", italic[i]);
			strike[i] = readBool(props, PANEL_KEYS[i] + "Strike", strike[i]);
		}
		lineThickness = Math.max(1, Math.min(8, readInt(props, "lineThickness", lineThickness)));
		lineColor = readInt(props, "lineColor", lineColor);
		lineStyle = Math.max(0, Math.min(2, readInt(props, "lineStyle", lineStyle)));
		dotColor = readInt(props, "dotColor", dotColor);
		spawnMode = Math.max(0, Math.min(2, readInt(props, "spawnMode", spawnMode)));
		showBestiary = readBool(props, "showBestiary", showBestiary);
		showProfit = readBool(props, "showProfit", showProfit);
		profitClassic = readBool(props, "profitClassic", profitClassic);
		profitPlacer = readBool(props, "profitPlacer", profitPlacer);
		avgView = Math.max(0, Math.min(1, readInt(props, "avgView", avgView)));
		placeColorA = readInt(props, "placeColorA", placeColorA);
		placeColorB = readInt(props, "placeColorB", placeColorB);
		placeShadow = readInt(props, "placeShadow", placeShadow);
		placeBangs = Math.max(0, Math.min(PlaceStyles.MAX_BANGS, readInt(props, "placeBangs", placeBangs)));
		placeFont = Math.max(0, Math.min(2, readInt(props, "placeFont", placeFont)));
		placeSize = Math.max(30, Math.min(300, readInt(props, "placeSize", placeSize)));
		placeRandom = readBool(props, "placeRandom", placeRandom);
		placeVariant = readInt(props, "placeVariant", placeVariant);
		profitNoticeShown = readBool(props, "profitNoticeShown", profitNoticeShown);
		welcomeShown = readBool(props, "welcomeShown", profitNoticeShown);
		partyFact = readBool(props, "partyFact", partyFact);
		party8ball = readBool(props, "party8ball", party8ball);
		achievements = props.getProperty("achievements", achievements).trim();
		achievementsNatural = props.getProperty("achievementsNatural", achievements).trim();
		celebration = Math.max(0, Math.min(100, readInt(props, "celebration", celebration)));
		partyWarp = readBool(props, "partyWarp", partyWarp);
		partyTransfer = readBool(props, "partyTransfer", partyTransfer);
		partyPromote = readBool(props, "partyPromote", partyPromote);
		partyKills = readBool(props, "partyKills", partyKills);
		partyGyro = readBool(props, "partyGyro", partyGyro);
		partyGoon = readBool(props, "partyGoon", partyGoon);
		partyAch = readBool(props, "partyAch", partyAch);
		partyAc = readBool(props, "partyAc", partyAc);
		partyArachne = readBool(props, "partyArachne", partyArachne);
		partyPb = readBool(props, "partyPb", partyPb);
		partyTotal = readBool(props, "partyTotal", partyTotal);
		partySkills = readBool(props, "partySkills", partySkills);
		partySession = readBool(props, "partySession", partySession);
		partyCoins = readBool(props, "partyCoins", partyCoins);
		partyEssence = readBool(props, "partyEssence", partyEssence);
		partyPbTime = readBool(props, "partyPbTime", partyPbTime);
		partyDamage = readBool(props, "partyDamage", partyDamage);
		longestSessionMs = readLong(props, "longestSessionMs", longestSessionMs);
		maxDamage = readLong(props, "maxDamage", maxDamage);
		partyHop = readBool(props, "partyHop", partyHop);
		partySplit = readBool(props, "partySplit", partySplit);
		partyTop = readBool(props, "partyTop", partyTop);
		partyJoinCheck = readBool(props, "partyJoinCheck", partyJoinCheck);
		darkGui = readBool(props, "darkGui", darkGui);
		partyRank = readBool(props, "partyRank", partyRank);
		rankOptIn = readBool(props, "rankOptIn", rankOptIn);
		updateAlerts = readBool(props, "updateAlerts", updateAlerts);
		lastUpdateSeen = props.getProperty("lastUpdateSeen", lastUpdateSeen).trim();
		rankUrl = props.getProperty("rankUrl", rankUrl);
		showSplits = readBool(props, "showSplits", showSplits);
		if (!readBool(props, "partyOnV2", false)) {

			partyKills = partyGyro = partyGoon = partyAch = partyAc = partyArachne = partyPb = partyTotal = true;
		}
		partyShareAch = readBool(props, "partyShareAch", partyShareAch);
		partyAlways = readBool(props, "partyAlways", partyAlways);
		broodKills = readInt(props, "broodKills", broodKills);
		crystalsPlaced = readInt(props, "crystalsPlaced", crystalsPlaced);
		finalHits = readInt(props, "finalHits", finalHits);
		silenceArachne = readBool(props, "silenceArachne", silenceArachne);
		profitView = Math.max(0, Math.min(2, readInt(props, "profitView", profitView)));
		profitShowTotals = readBool(props, "profitShowTotals", profitShowTotals);
		apiKey = props.getProperty("apiKey", apiKey).trim();
		bestiaryKills = readInt(props, "bestiaryKills", bestiaryKills);
		discordUrl = props.getProperty("discordUrl", DISCORD_DEFAULT).trim();
		if (discordUrl.isEmpty()) {
			discordUrl = DISCORD_DEFAULT;
		}
		radius = readInt(props, "radius", radius);

		enabled = readBool(props, "enabled", enabled);
		showPhase = readBool(props, "showPhase", showPhase);
		showBroods = readBool(props, "showBroods", showBroods);
		glow = readBool(props, "glow", glow);
		runicGlow = readBool(props, "runicGlow", runicGlow);
		tracer = readBool(props, "tracer", tracer);
		placer = readBool(props, "placer", placer);
		showBossTimer = readBool(props, "showBossTimer", showBossTimer);
		showAvgTimer = readBool(props, "showAvgTimer", showAvgTimer);
		showSpawnTimer = readBool(props, "showSpawnTimer", showSpawnTimer);
		spawnDisplay = Math.max(0, Math.min(3, readInt(props, "spawnDisplay", spawnDisplay)));
		holoScaleWithDistance = readBool(props, "holoScaleWithDistance", holoScaleWithDistance);
		holoSize = Math.max(30, Math.min(400, readInt(props, "holoSize", holoSize)));
		holoBackground = readBool(props, "holoBackground", holoBackground);
		holoMinimal = readBool(props, "holoMinimal", holoMinimal);
		showCps = readBool(props, "showCps", showCps);
		sanctuaryOnly = readBool(props, "sanctuaryOnly", sanctuaryOnly);
		sanctuaryText = props.getProperty("sanctuaryText", sanctuaryText).trim();
		spawnParticles = readBool(props, "spawnParticles", spawnParticles);
		minimalPhase = readBool(props, "minimalPhase", minimalPhase);
		hideMobNames = readBool(props, "hideMobNames", hideMobNames);
		minimalBroodTags = readBool(props, "minimalBroodTags", minimalBroodTags);
		hideDamageNumbers = readBool(props, "hideDamageNumbers", hideDamageNumbers);
		hideParticles = readBool(props, "hideParticles", hideParticles);
		hidePlayerNames = readBool(props, "hidePlayerNames", hidePlayerNames);
		playerShrink = Math.max(1, Math.min(10, readInt(props, "playerShrink", playerShrink)));
		altarSet = readBool(props, "altarSet", altarSet);
		altarX = readLong(props, "altarX100", Math.round(altarX * 100)) / 100.0;
		altarY = readLong(props, "altarY100", Math.round(altarY * 100)) / 100.0;
		altarZ = readLong(props, "altarZ100", Math.round(altarZ * 100)) / 100.0;
		debug = readBool(props, "debug", debug);
		countAllEntities = readBool(props, "countAllEntities", countAllEntities);

		killCount = readInt(props, "killCount", killCount);
		killTotalMs = readLong(props, "killTotalMs", killTotalMs);
		lastKillMs = readLong(props, "lastKillMs", lastKillMs);
		bestKillMs = readLong(props, "bestKillMs", bestKillMs);

		save();
	}

	public static void save() {
		Properties props = new Properties();
		for (int i = 0; i < PANEL_COUNT; i++) {
			props.setProperty(PANEL_KEYS[i] + "X", Integer.toString(posX[i]));
			props.setProperty(PANEL_KEYS[i] + "Y", Integer.toString(posY[i]));
			props.setProperty(PANEL_KEYS[i] + "Scale", Integer.toString(Math.round(scale[i] * 100)));
			props.setProperty(PANEL_KEYS[i] + "Bg", Boolean.toString(background[i]));
			props.setProperty(PANEL_KEYS[i] + "Center", Boolean.toString(centered[i]));
			props.setProperty(PANEL_KEYS[i] + "Color", Integer.toString(textColor[i]));
			props.setProperty(PANEL_KEYS[i] + "Spacing", Integer.toString(letterSpacing[i]));
			props.setProperty(PANEL_KEYS[i] + "Bold", Boolean.toString(bold[i]));
			props.setProperty(PANEL_KEYS[i] + "Italic", Boolean.toString(italic[i]));
			props.setProperty(PANEL_KEYS[i] + "Strike", Boolean.toString(strike[i]));
		}
		props.setProperty("lineThickness", Integer.toString(lineThickness));
		props.setProperty("lineColor", Integer.toString(lineColor));
		props.setProperty("lineStyle", Integer.toString(lineStyle));
		props.setProperty("dotColor", Integer.toString(dotColor));
		props.setProperty("spawnMode", Integer.toString(spawnMode));
		props.setProperty("showBestiary", Boolean.toString(showBestiary));
		props.setProperty("showProfit", Boolean.toString(showProfit));
		props.setProperty("profitClassic", Boolean.toString(profitClassic));
		props.setProperty("profitPlacer", Boolean.toString(profitPlacer));
		props.setProperty("avgView", Integer.toString(avgView));
		props.setProperty("placeColorA", Integer.toString(placeColorA));
		props.setProperty("placeColorB", Integer.toString(placeColorB));
		props.setProperty("placeShadow", Integer.toString(placeShadow));
		props.setProperty("placeBangs", Integer.toString(placeBangs));
		props.setProperty("placeFont", Integer.toString(placeFont));
		props.setProperty("placeSize", Integer.toString(placeSize));
		props.setProperty("placeRandom", Boolean.toString(placeRandom));
		props.setProperty("placeVariant", Integer.toString(placeVariant));
		props.setProperty("profitNoticeShown", Boolean.toString(profitNoticeShown));
		props.setProperty("welcomeShown", Boolean.toString(welcomeShown));
		props.setProperty("partyFact", Boolean.toString(partyFact));
		props.setProperty("party8ball", Boolean.toString(party8ball));
		props.setProperty("achievements", achievements);
		props.setProperty("achievementsNatural", achievementsNatural);
		props.setProperty("celebration", Integer.toString(celebration));
		props.setProperty("partyWarp", Boolean.toString(partyWarp));
		props.setProperty("partyTransfer", Boolean.toString(partyTransfer));
		props.setProperty("partyPromote", Boolean.toString(partyPromote));
		props.setProperty("partyKills", Boolean.toString(partyKills));
		props.setProperty("partyGyro", Boolean.toString(partyGyro));
		props.setProperty("partyGoon", Boolean.toString(partyGoon));
		props.setProperty("partyAch", Boolean.toString(partyAch));
		props.setProperty("partyAc", Boolean.toString(partyAc));
		props.setProperty("partyArachne", Boolean.toString(partyArachne));
		props.setProperty("partyPb", Boolean.toString(partyPb));
		props.setProperty("partyTotal", Boolean.toString(partyTotal));
		props.setProperty("partySkills", Boolean.toString(partySkills));
		props.setProperty("partySession", Boolean.toString(partySession));
		props.setProperty("partyCoins", Boolean.toString(partyCoins));
		props.setProperty("partyEssence", Boolean.toString(partyEssence));
		props.setProperty("partyPbTime", Boolean.toString(partyPbTime));
		props.setProperty("partyDamage", Boolean.toString(partyDamage));
		props.setProperty("longestSessionMs", Long.toString(longestSessionMs));
		props.setProperty("maxDamage", Long.toString(maxDamage));
		props.setProperty("partyHop", Boolean.toString(partyHop));
		props.setProperty("partySplit", Boolean.toString(partySplit));
		props.setProperty("partyTop", Boolean.toString(partyTop));
		props.setProperty("partyJoinCheck", Boolean.toString(partyJoinCheck));
		props.setProperty("darkGui", Boolean.toString(darkGui));
		props.setProperty("partyRank", Boolean.toString(partyRank));
		props.setProperty("rankOptIn", Boolean.toString(rankOptIn));
		props.setProperty("updateAlerts", Boolean.toString(updateAlerts));
		props.setProperty("lastUpdateSeen", lastUpdateSeen);
		props.setProperty("rankUrl", rankUrl);
		props.setProperty("showSplits", Boolean.toString(showSplits));
		props.setProperty("partyOnV2", "true");
		props.setProperty("partyShareAch", Boolean.toString(partyShareAch));
		props.setProperty("partyAlways", Boolean.toString(partyAlways));
		props.setProperty("broodKills", Integer.toString(broodKills));
		props.setProperty("crystalsPlaced", Integer.toString(crystalsPlaced));
		props.setProperty("finalHits", Integer.toString(finalHits));
		props.setProperty("silenceArachne", Boolean.toString(silenceArachne));
		props.setProperty("profitView", Integer.toString(profitView));
		props.setProperty("profitShowTotals", Boolean.toString(profitShowTotals));
		props.setProperty("apiKey", apiKey);
		props.setProperty("bestiaryKills", Integer.toString(bestiaryKills));
		props.setProperty("discordUrl", discordUrl);
		props.setProperty("radius", Integer.toString(radius));
		props.setProperty("enabled", Boolean.toString(enabled));
		props.setProperty("showPhase", Boolean.toString(showPhase));
		props.setProperty("showBroods", Boolean.toString(showBroods));
		props.setProperty("glow", Boolean.toString(glow));
		props.setProperty("runicGlow", Boolean.toString(runicGlow));
		props.setProperty("tracer", Boolean.toString(tracer));
		props.setProperty("placer", Boolean.toString(placer));
		props.setProperty("showBossTimer", Boolean.toString(showBossTimer));
		props.setProperty("showAvgTimer", Boolean.toString(showAvgTimer));
		props.setProperty("showSpawnTimer", Boolean.toString(showSpawnTimer));
		props.setProperty("spawnDisplay", Integer.toString(spawnDisplay));
		props.setProperty("holoScaleWithDistance", Boolean.toString(holoScaleWithDistance));
		props.setProperty("holoSize", Integer.toString(holoSize));
		props.setProperty("holoBackground", Boolean.toString(holoBackground));
		props.setProperty("holoMinimal", Boolean.toString(holoMinimal));
		props.setProperty("showCps", Boolean.toString(showCps));
		props.setProperty("sanctuaryOnly", Boolean.toString(sanctuaryOnly));
		props.setProperty("sanctuaryText", sanctuaryText);
		props.setProperty("spawnParticles", Boolean.toString(spawnParticles));
		props.setProperty("minimalPhase", Boolean.toString(minimalPhase));
		props.setProperty("hideMobNames", Boolean.toString(hideMobNames));
		props.setProperty("minimalBroodTags", Boolean.toString(minimalBroodTags));
		props.setProperty("hideDamageNumbers", Boolean.toString(hideDamageNumbers));
		props.setProperty("hideParticles", Boolean.toString(hideParticles));
		props.setProperty("hidePlayerNames", Boolean.toString(hidePlayerNames));
		props.setProperty("playerShrink", Integer.toString(playerShrink));
		props.setProperty("altarSet", Boolean.toString(altarSet));
		props.setProperty("altarX100", Long.toString(Math.round(altarX * 100)));
		props.setProperty("altarY100", Long.toString(Math.round(altarY * 100)));
		props.setProperty("altarZ100", Long.toString(Math.round(altarZ * 100)));
		props.setProperty("debug", Boolean.toString(debug));
		props.setProperty("countAllEntities", Boolean.toString(countAllEntities));
		props.setProperty("killCount", Integer.toString(killCount));
		props.setProperty("killTotalMs", Long.toString(killTotalMs));
		props.setProperty("lastKillMs", Long.toString(lastKillMs));
		props.setProperty("bestKillMs", Long.toString(bestKillMs));

		Path file = file();
		try (Writer writer = Files.newBufferedWriter(file)) {
			props.store(writer, "Arachne Plus settings. Change them in game with the settings key (Right Shift by default).");
		} catch (IOException e) {
			ArachnePlusClient.LOGGER.warn("Could not write {}: {}", file, e.toString());
		}
	}

	private static int readInt(Properties props, String key, int fallback) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static long readLong(Properties props, String key, long fallback) {
		try {
			return Long.parseLong(props.getProperty(key, Long.toString(fallback)).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static boolean readBool(Properties props, String key, boolean fallback) {
		return Boolean.parseBoolean(props.getProperty(key, Boolean.toString(fallback)).trim());
	}
}
