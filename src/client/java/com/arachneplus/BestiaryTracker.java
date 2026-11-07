package com.arachneplus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

public final class BestiaryTracker {
	private BestiaryTracker() {}

	private static final AtomicBoolean busy = new AtomicBoolean(false);
	private static long nextFetchMs = 0;

	public static volatile String status = "";

	public static volatile String foundKeys = "";

	private static int retries = 0;
	private static boolean announce = false;

	public static void requestSoon() {
		nextFetchMs = System.currentTimeMillis() + (fast() ? 4000L : 8000L);
		retries = fast() ? 20 : 12;
		announce = true;
	}

	private static boolean fast() {
		return !Config.apiKey.isEmpty();
	}

	public static void countKill() {
		if (Config.bestiaryKills >= 0) {
			Config.bestiaryKills++;
		}
	}

	public static void resetTimer() {
		nextFetchMs = 0;
	}

	public static void tick(Minecraft mc) {
		if (!HypixelApi.available() || mc.player == null || busy.get()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextFetchMs) {
			return;
		}

		nextFetchMs = now + (fast() ? (Location.inSanctuary ? 15_000L : 60_000L) : (Location.inSanctuary ? 60_000L : 180_000L));
		UUID id = mc.getUser().getProfileId();
		if (id == null) {
			return;
		}
		busy.set(true);
		Thread t = new Thread(() -> fetch(id), "ArachnePlus-api");
		t.setDaemon(true);
		t.start();
	}

	private static void fetch(UUID id) {
		try {
			HypixelApi.Resp resp = HypixelApi.profiles(id.toString().replace("-", ""));
			int code = resp.code;
			if (code != 200) {
				status = code == 403 ? "key rejected (http 403), the Arachne Plus key may be out of date" : code == 429 ? "too many requests, retrying later" : "http " + code;
				return;
			}
			{
				JsonObject root = resp.json();
				if (!root.has("profiles") || root.get("profiles").isJsonNull()) {
					status = "no profiles (is the key right?)";
					return;
				}
				String key = id.toString().replace("-", "");
				JsonObject best = null;
				for (JsonElement p : root.getAsJsonArray("profiles")) {
					JsonObject po = p.getAsJsonObject();
					if (po.has("selected") && po.get("selected").getAsBoolean()) {
						best = po;
					}
				}
				if (best == null) {
					status = "no selected Skyblock profile";
					return;
				}
				JsonObject members = best.getAsJsonObject("members");
				if (members == null || !members.has(key)) {
					status = "your account is not in the profile";
					return;
				}
				JsonObject me = members.getAsJsonObject(key);
				int total = 0;
				int brood = 0;
				boolean found = false;
				boolean hasBestiary = me.has("bestiary") && me.get("bestiary").isJsonObject()
						&& me.getAsJsonObject("bestiary").has("kills");
				StringBuilder keys = new StringBuilder();
				if (hasBestiary) {
					for (Map.Entry<String, JsonElement> e : me.getAsJsonObject("bestiary").getAsJsonObject("kills").entrySet()) {
						String k = e.getKey().toLowerCase();
						if (!k.contains("arachne")) {
							continue;
						}
						int v = 0;
						try {
							v = e.getValue().getAsInt();
						} catch (Exception ignored) {
							continue;
						}
						keys.append(k).append('=').append(v).append(' ');
						if (k.contains("brood")) {
							brood += v;
						} else if (!k.contains("keeper")) {
							total += v;
							found = true;
						}
					}
				}
				foundKeys = keys.length() == 0 ? "none" : keys.toString().trim();
				if (!hasBestiary) {
					status = "no bestiary data (switch on the Skyblock API settings in-game)";
					return;
				}
				int apiKills = found ? total : 0;
				boolean behind = apiKills < Config.bestiaryKills;
				if (!behind) {
					Config.bestiaryKills = apiKills;
				}
				Config.broodKills = brood;
				Config.save();
				status = behind ? "api behind, checking again" : "api ok";
				if (!behind) {
					RankBoard.submitIfDue();
				}
				if (behind && retries > 0) {
					retries--;
					nextFetchMs = System.currentTimeMillis() + (fast() ? 5_000L : 10_000L);
				} else {
					if (announce && !behind) {
						final int k = Config.bestiaryKills;
						final int b = Config.broodKills;
						Minecraft.getInstance().execute(() -> ArachnePlusClient.say("Bestiary updated: " + String.format("%,d", k)
								// tired, come back to this
								+ " Arachne kills, " + String.format("%,d", b) + " Brood kills."));
					}
					retries = 0;
					announce = false;
				}
			}
		} catch (Throwable ex) {
			status = "api error: " + ex.getClass().getSimpleName();
		} finally {
			busy.set(false);
		}
	}

	private static long nextLookupMs = 0;

	public static void lookup(String name) {
		if (Config.apiKey.isEmpty()) {
			ArachnePlusClient.say("Looking up players needs a Hypixel API key: type /apapikey <key>");
			return;
		}
		if (!name.matches("[A-Za-z0-9_]{1,16}")) {
			ArachnePlusClient.say("That does not look like a username. Use: /apstats <username>");
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextLookupMs) {
			ArachnePlusClient.say("Wait a few seconds between lookups");
			return;
		}
		nextLookupMs = now + 4000;
		ArachnePlusClient.say("Looking up " + name + "...");
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			String msg;
			try {
				msg = lookupNow(name);
			} catch (Throwable ex) {
				msg = "Lookup failed (" + ex.getClass().getSimpleName() + ")";
			}
			final String m = msg;
			mc.execute(() -> ArachnePlusClient.say(m));
		}, "ArachnePlus-lookup");
		t.setDaemon(true);
		t.start();
	}

	private static String lookupNow(String name) throws Exception {
		HttpURLConnection mj = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
		mj.setConnectTimeout(8000);
		mj.setReadTimeout(8000);
		mj.setRequestProperty("User-Agent", "ArachnePlus");
		int mcode = mj.getResponseCode();
		if (mcode == 204 || mcode == 404) {
			return "No player called " + name + " exists";
		}
		if (mcode != 200) {
			return "Could not find " + name + " (http " + mcode + ")";
		}
		String id;
		String real;
		try (InputStream in = mj.getInputStream(); InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
			id = o.get("id").getAsString();
			real = o.get("name").getAsString();
		}
		HttpURLConnection c = (HttpURLConnection) new URL("https://api.hypixel.net/v2/skyblock/profiles?uuid=" + id).openConnection();
		c.setConnectTimeout(8000);
		c.setReadTimeout(8000);
		c.setRequestProperty("API-Key", Config.apiKey);
		c.setRequestProperty("User-Agent", "ArachnePlus");
		int code = c.getResponseCode();
		if (code != 200) {
			return code == 403 ? "Your API key was rejected (http 403)" : code == 429 ? "Too many requests, try again in a minute" : "Hypixel error (http " + code + ")";
		}
		try (InputStream in = c.getInputStream(); InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
			if (!root.has("profiles") || root.get("profiles").isJsonNull()) {
				return real + " has no Skyblock profiles";
			}
			JsonObject me = null;
			for (JsonElement p : root.getAsJsonArray("profiles")) {
				JsonObject po = p.getAsJsonObject();
				if (po.has("selected") && po.get("selected").getAsBoolean()) {
					JsonObject members = po.getAsJsonObject("members");
					if (members != null && members.has(id)) {
						me = members.getAsJsonObject(id);
					}
				}
			}
			if (me == null) {
				return real + " has no selected Skyblock profile";
			}
			if (!(me.has("bestiary") && me.get("bestiary").isJsonObject() && me.getAsJsonObject("bestiary").has("kills"))) {
				return real + " has their Skyblock API access turned off (no bestiary data)";
			}
			int total = 0;
			int brood = 0;
			for (Map.Entry<String, JsonElement> e : me.getAsJsonObject("bestiary").getAsJsonObject("kills").entrySet()) {
				String k = e.getKey().toLowerCase();
				if (!k.contains("arachne")) {
					continue;
				}
				int v;
				try {
					v = e.getValue().getAsInt();
				} catch (Exception ignored) {
					continue;
				}
				if (k.contains("brood")) {
					brood += v;
				} else if (!k.contains("keeper")) {
					total += v;
				}
			}
			return real + ": " + String.format("%,d", total) + " Arachne kills, " + String.format("%,d", brood) + " Brood kills";
		}
	}

	public static void essenceScan(java.util.function.Consumer<Long> done) {
		var mc = Minecraft.getInstance();
		UUID id = mc.getUser().getProfileId();
		if (id == null || !HypixelApi.available()) {
			done.accept(-1L);
			return;
		}
		Thread t = new Thread(() -> done.accept(essenceNow(id)), "ArachnePlus-essence");
		t.setDaemon(true);
		t.start();
	}

	private static long essenceNow(UUID id) {
		try {
			HypixelApi.Resp resp = HypixelApi.profiles(id.toString().replace("-", ""));
			if (resp.code != 200) {
				return -1;
			}
			JsonObject root = resp.json();
			if (!root.has("profiles") || root.get("profiles").isJsonNull()) {
				return -1;
			}
			String key = id.toString().replace("-", "");
			for (JsonElement p : root.getAsJsonArray("profiles")) {
				JsonObject po = p.getAsJsonObject();
				if (!(po.has("selected") && po.get("selected").getAsBoolean())) {
					continue;
				}
				JsonObject members = po.getAsJsonObject("members");
				if (members == null || !members.has(key)) {
					return -1;
				}
				JsonObject me = members.getAsJsonObject(key);
				if (!me.has("currencies") || !me.get("currencies").isJsonObject()) {
					return -1;
				}
				JsonObject cur = me.getAsJsonObject("currencies");
				if (!cur.has("essence") || !cur.get("essence").isJsonObject()) {
					return -1;
				}
				JsonObject ess = cur.getAsJsonObject("essence");
				if (!ess.has("SPIDER") || !ess.get("SPIDER").isJsonObject()) {
					return 0;
				}
				JsonObject sp = ess.getAsJsonObject("SPIDER");
				return sp.has("current") ? sp.get("current").getAsLong() : 0;
			}
		} catch (Throwable ex) {
			return -1;
		}
		return -1;
	}

	public static void gyroScan(java.util.function.Consumer<Integer> done) {
		var mc = Minecraft.getInstance();
		UUID id = mc.getUser().getProfileId();
		if (id == null || !HypixelApi.available()) {
			done.accept(-1);
			return;
		}
		Thread t = new Thread(() -> done.accept(gyroScanNow(id)), "ArachnePlus-gyro");
		t.setDaemon(true);
		t.start();
	}

	private static int gyroScanNow(UUID id) {
		try {
			HypixelApi.Resp resp = HypixelApi.profiles(id.toString().replace("-", ""));
			if (resp.code != 200) {
				return -1;
			}
			{
				JsonObject root = resp.json();
				if (!root.has("profiles") || root.get("profiles").isJsonNull()) {
					return -1;
				}
				String key = id.toString().replace("-", "");
				for (JsonElement p : root.getAsJsonArray("profiles")) {
					JsonObject po = p.getAsJsonObject();
					if (!(po.has("selected") && po.get("selected").getAsBoolean())) {
						continue;
					}
					JsonObject members = po.getAsJsonObject("members");
					if (members == null || !members.has(key)) {
						return -1;
					}
					JsonObject me = members.getAsJsonObject(key);
					if (!me.has("inventory") || !me.get("inventory").isJsonObject()) {
						return -1;
					}
					return scanJson(me.getAsJsonObject("inventory")) ? 1 : 0;
				}
			}
		} catch (Throwable ex) {
			return -1;
		}
		return -1;
	}

	private static boolean scanJson(JsonElement e) {
		if (e.isJsonObject()) {
			for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
				JsonElement v = en.getValue();
				if (en.getKey().equals("data") && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
					if (packedHasGyro(v.getAsString())) {
						return true;
					}
				} else if (scanJson(v)) {
					return true;
				}
			}
		} else if (e.isJsonArray()) {
			for (JsonElement x : e.getAsJsonArray()) {
				if (scanJson(x)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean packedHasGyro(String b64) {
		try {
			byte[] raw = java.util.Base64.getMimeDecoder().decode(b64);
			try (java.util.zip.GZIPInputStream z = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(raw))) {
				String s = new String(z.readAllBytes(), StandardCharsets.ISO_8859_1);
				return s.contains("GYROKINETIC_WAND");
			}
		} catch (Exception ex) {
			return false;
		}
	}
}
