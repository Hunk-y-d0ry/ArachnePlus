package com.arachneplus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

public final class PlayerLookup {
	private PlayerLookup() { }

	public static final class Info {
		String name;
		String id;
		JsonObject me;
		String error;
	}

	private static final Map<String, Info> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Long> CACHE_AT = new ConcurrentHashMap<>();
	private static final long CACHE_MS = 60_000L;
	private static long nextCommandMs = 0;

	static boolean ready() {
		if (!HypixelApi.available()) {
			ArachnePlusClient.say("The Arachne Plus website is not set up and you have no API key: type /apapikey <key>");
			return false;
		}
		long now = System.currentTimeMillis();
		if (now < nextCommandMs) {
			ArachnePlusClient.say("Wait a few seconds between lookups");
			return false;
		}
		nextCommandMs = now + 3000;
		return true;
	}

	static boolean validName(String name) {
		return name != null && name.matches("[A-Za-z0-9_]{1,16}");
	}

	static void run(String name, Consumer<Info> done) {
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			Info info = load(name);
			mc.execute(() -> done.accept(info));
		}, "ArachnePlus-lookup");
		t.setDaemon(true);
		t.start();
	}

	static void runAll(List<String> names, Consumer<List<Info>> done) {
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			List<Info> all = new ArrayList<>();
			for (String n : names) {
				all.add(load(n));
				try {
					Thread.sleep(250);
				} catch (InterruptedException ignored) {
					return;
				}
			}
			mc.execute(() -> done.accept(all));
		}, "ArachnePlus-lookup-all");
		t.setDaemon(true);
		t.start();
	}

	static Info load(String name) {
		String key = name.toLowerCase(Locale.ROOT);
		Long at = CACHE_AT.get(key);
		if (at != null && System.currentTimeMillis() - at < CACHE_MS && CACHE.get(key) != null) {
			return CACHE.get(key);
		}
		Info info = new Info();
		info.name = name;
		try {
			HttpURLConnection mj = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
			mj.setConnectTimeout(8000);
			mj.setReadTimeout(8000);
			mj.setRequestProperty("User-Agent", "ArachnePlus");
			int mcode = mj.getResponseCode();
			if (mcode == 204 || mcode == 404) {
				info.error = "No player called " + name + " exists";
				return info;
			}
			if (mcode != 200) {
				info.error = "Could not find " + name + " (http " + mcode + ")";
				return info;
			}
			try (InputStream in = mj.getInputStream(); InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
				info.id = o.get("id").getAsString();
				info.name = o.get("name").getAsString();
			}
			HypixelApi.Resp resp = HypixelApi.profiles(info.id);
			int code = resp.code;
			if (code != 200) {
				info.error = code == 403 ? "Hypixel rejected the request (http 403). The Arachne Plus key may be out of date, try again later"
						: code == 429 ? "Too many requests, try again in a minute (a personal key with /apapikey also helps)" : "Hypixel error (http " + code + ")";
				return info;
			}
			{
				JsonObject root = resp.json();
				if (!root.has("profiles") || root.get("profiles").isJsonNull()) {
					info.error = info.name + " has no Skyblock profiles";
					return info;
				}
				for (JsonElement p : root.getAsJsonArray("profiles")) {
					JsonObject po = p.getAsJsonObject();
					if (po.has("selected") && po.get("selected").getAsBoolean()) {
						JsonObject members = po.getAsJsonObject("members");
						if (members != null && members.has(info.id)) {
							info.me = members.getAsJsonObject(info.id);
						}
					}
				}
				if (info.me == null) {
					info.error = info.name + " has no selected Skyblock profile";
					return info;
				}
			}
			CACHE.put(key, info);
			CACHE_AT.put(key, System.currentTimeMillis());
		} catch (Throwable ex) {
			info.error = "Lookup failed (" + ex.getClass().getSimpleName() + ")";
		}
		return info;
	}

	static long[] kills(Info i) {
		if (!(i.me.has("bestiary") && i.me.get("bestiary").isJsonObject() && i.me.getAsJsonObject("bestiary").has("kills"))) {
			return null;
		}
		long total = 0;
		long brood = 0;
		for (Map.Entry<String, JsonElement> e : i.me.getAsJsonObject("bestiary").getAsJsonObject("kills").entrySet()) {
			String k = e.getKey().toLowerCase(Locale.ROOT);
			if (!k.contains("arachne")) {
				continue;
			}
			long v;
			try {
				v = e.getValue().getAsLong();
			} catch (Exception ignored) {
				continue;
			}
			if (k.contains("brood")) {
				brood += v;
			} else if (!k.contains("keeper")) {
				total += v;
			}
		}
		return new long[] {total, brood};
	}

	private static final long[] TARANTULA_XP = {5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000};

	static long[] spider(Info i) {
		if (!i.me.has("slayer") || !i.me.get("slayer").isJsonObject()) {
			return null;
		}
		JsonObject s = i.me.getAsJsonObject("slayer");
		if (!s.has("slayer_bosses") || !s.get("slayer_bosses").isJsonObject()) {
			return null;
		}
		JsonObject b = s.getAsJsonObject("slayer_bosses");
		if (!b.has("spider") || !b.get("spider").isJsonObject()) {
			return new long[] {0, 0, 0, 0, 0, 0, 0};
		}
		JsonObject sp = b.getAsJsonObject("spider");
		long xp = sp.has("xp") ? sp.get("xp").getAsLong() : 0;
		long level = 0;
		for (long need : TARANTULA_XP) {
			if (xp >= need) {
				level++;
			}
		}
		long[] r = new long[7];
		r[0] = xp;
		r[1] = level;
		for (int t = 0; t < 5; t++) {
			String k = "boss_kills_tier_" + t;
			r[2 + t] = sp.has(k) ? sp.get(k).getAsLong() : 0;
		}
		return r;
	}

	private static final long[] SKILL_STEP = {50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000,
			30000, 50000, 75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
			1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000,
			2600000, 2700000, 2800000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000, 5800000,
			6100000, 6400000, 6700000, 7000000};

	static int combatLevel(Info i) {
		double xp = -1;
		try {
			if (i.me.has("player_data") && i.me.getAsJsonObject("player_data").has("experience")
					&& i.me.getAsJsonObject("player_data").getAsJsonObject("experience").has("SKILL_COMBAT")) {
				xp = i.me.getAsJsonObject("player_data").getAsJsonObject("experience").get("SKILL_COMBAT").getAsDouble();
			} else if (i.me.has("experience_skill_combat")) {
				xp = i.me.get("experience_skill_combat").getAsDouble();
			}
		} catch (Exception ex) {
			return -1;
		}
		if (xp < 0) {
			return -1;
		}
		int level = 0;
		double left = xp;
		for (long step : SKILL_STEP) {
			if (left >= step) {
				left -= step;
				level++;
			} else {
				break;
			}
		}
		return level;
	}

	private static final Pattern TOKEN = Pattern.compile("[A-Z][A-Z0-9_]{3,}");

	static final class Gear {
		final Set<String> ids = new HashSet<>();

		int stingChimera = -1;

		String beltReforge = null;
	}

	static Gear gear(Info i) {
		if (!i.me.has("inventory") || !i.me.get("inventory").isJsonObject()) {
			return null;
		}
		Gear g = new Gear();
		walk(i.me.getAsJsonObject("inventory"), g);
		return g;
	}

	private static void walk(JsonElement e, Gear g) {
		if (e.isJsonObject()) {
			for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
				JsonElement v = en.getValue();
				if (en.getKey().equals("data") && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
					decode(v.getAsString(), g);
				} else {
					walk(v, g);
				}
			}
		} else if (e.isJsonArray()) {
			for (JsonElement x : e.getAsJsonArray()) {
				walk(x, g);
			}
		}
	}

	private static void decode(String b64, Gear g) {
		byte[] bytes;
		try {
			byte[] raw = java.util.Base64.getMimeDecoder().decode(b64);
			try (java.util.zip.GZIPInputStream z = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(raw))) {
				bytes = z.readAllBytes();
			}
		} catch (Exception ex) {
			return;
		}
		try {
			java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
			int type = in.readUnsignedByte();
			in.readUTF();
			Object root = readTag(in, type);
			if (root instanceof Map<?, ?> m && m.get("i") instanceof List<?> items) {
				for (Object it : items) {
					if (!(it instanceof Map<?, ?> item)) {
						continue;
					}
					Map<?, ?> extra = null;
					if (item.get("tag") instanceof Map<?, ?> tag && tag.get("ExtraAttributes") instanceof Map<?, ?> ex1) {
						extra = ex1;
					} else if (item.get("components") instanceof Map<?, ?> comp && comp.get("minecraft:custom_data") instanceof Map<?, ?> ex2) {
						extra = ex2;
					}
					if (extra == null) {
						continue;
					}
					Object id = extra.get("id");
					if (!(id instanceof String ids)) {
						continue;
					}
					g.ids.add(ids);
					if (ids.contains("PRIMORDIAL") && ids.contains("BELT")) {
						String mod = extra.get("modifier") instanceof String ms ? ms.toLowerCase(Locale.ROOT) : "";
						if (g.beltReforge == null || !g.beltReforge.equals("bloodshot")) {
							g.beltReforge = mod;
						}
					}
					if (ids.equals("STING")) {
						int lvl = 0;
						if (extra.get("enchantments") instanceof Map<?, ?> ench && ench.get("ultimate_chimera") instanceof Number n) {
							lvl = n.intValue();
						}
						g.stingChimera = Math.max(g.stingChimera, lvl);
					}
				}
			}
		} catch (Exception ex) {

		}
		Matcher m = TOKEN.matcher(new String(bytes, StandardCharsets.ISO_8859_1));
		while (m.find()) {
			g.ids.add(m.group());
			if (m.group().contains("PRIMORDIAL") && m.group().contains("BELT") && g.beltReforge == null) {
				g.beltReforge = "";
			}
			if (m.group().equals("STING") && g.stingChimera < 0) {
				g.stingChimera = 0;
			}
		}
	}

	private static Object readTag(java.io.DataInputStream in, int type) throws java.io.IOException {
		switch (type) {
			case 1: return in.readByte();
			case 2: return in.readShort();
			case 3: return in.readInt();
			case 4: return in.readLong();
			case 5: return in.readFloat();
			case 6: return in.readDouble();
			case 7: {
				int n = in.readInt();
				in.skipNBytes(n);
				return null;
			}
			case 8: return in.readUTF();
			case 9: {
				int et = in.readUnsignedByte();
				int n = in.readInt();
				List<Object> list = new ArrayList<>();
				for (int k = 0; k < n; k++) {
					list.add(readTag(in, et));
				}
				return list;
			}
			case 10: {
				Map<String, Object> map = new java.util.HashMap<>();
				while (true) {
					int t = in.readUnsignedByte();
					if (t == 0) {
						return map;
					}
					String name = in.readUTF();
					map.put(name, readTag(in, t));
				}
			}
			case 11: {
				int n = in.readInt();
				in.skipNBytes(4L * n);
				return null;
			}
			case 12: {
				int n = in.readInt();
				in.skipNBytes(8L * n);
				return null;
			}
			default: throw new java.io.IOException("bad tag " + type);
		}
	}

	private static final Pattern KUUDRA = Pattern.compile("^(INFERNAL_|FIERY_|BURNING_|HOT_)?CRIMSON_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)$");
	private static final String[] TIER_NAMES = {"Basic", "Hot", "Burning", "Fiery", "Infernal"};

	static String crimsonArmor(Set<String> ids) {
		int best = -1;
		java.util.TreeSet<String> parts = new java.util.TreeSet<>();
		for (String id : ids) {
			Matcher m = KUUDRA.matcher(id);
			if (!m.matches()) {
				continue;
			}
			String pre = m.group(1);
			int tier = pre == null ? 0 : pre.startsWith("HOT") ? 1 : pre.startsWith("BURNING") ? 2 : pre.startsWith("FIERY") ? 3 : 4;
			if (tier > best) {
				best = tier;
				parts.clear();
			}
			if (tier == best) {
				parts.add(m.group(2).charAt(0) + m.group(2).substring(1).toLowerCase(Locale.ROOT));
			}
		}
		if (best < 0) {
			return "no Crimson armor";
		}
		String list = "";
		int count = 0;
		for (String piece : parts) {
			list = list + piece;
			count++;
			if (count != parts.size()) {
				list = list + ", ";
			}
		}
		String word = " pieces: ";
		if (parts.size() == 1) {
			word = " piece: ";
		}
		return TIER_NAMES[best] + " Crimson (" + parts.size() + word + list + ")";
	}

	static String goldenDragon(Info i) {
		JsonElement pets = null;
		if (i.me.has("pets_data") && i.me.get("pets_data").isJsonObject() && i.me.getAsJsonObject("pets_data").has("pets")) {
			pets = i.me.getAsJsonObject("pets_data").get("pets");
		} else if (i.me.has("pets")) {
			pets = i.me.get("pets");
		}
		if (pets == null || !pets.isJsonArray()) {
			return null;
		}
		String best = null;
		double bestXp = -1;
		for (JsonElement p : pets.getAsJsonArray()) {
			if (!p.isJsonObject()) {
				continue;
			}
			JsonObject po = p.getAsJsonObject();
			if (po.has("type") && "GOLDEN_DRAGON".equals(po.get("type").getAsString())) {
				double xp = po.has("exp") ? po.get("exp").getAsDouble() : 0;
				if (xp > bestXp) {
					bestXp = xp;
					String tier = po.has("tier") ? po.get("tier").getAsString() : "";
					tier = tier.isEmpty() ? "" : tier.charAt(0) + tier.substring(1).toLowerCase(Locale.ROOT);
					String lvl = xp >= 210_255_385D ? "Lvl 200" : xp >= 25_353_230D ? "Lvl 100-199" : "under Lvl 100";
					best = (tier.isEmpty() ? "" : tier + ", ") + lvl;
				}
			}
		}
		return best == null ? "no" : best;
	}

	static String yesNo(boolean b) {
		return b ? "yes" : "no";
	}

	static String n(long v) {
		return String.format(Locale.ROOT, "%,d", v);
	}
}
