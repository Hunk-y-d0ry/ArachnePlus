package com.arachneplus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

public final class RankBoard {
	private RankBoard() { }

	static final String DEFAULT_URL = "https://arachneplus.com";

	static volatile int myRank = -1;
	static volatile int total = -1;
	static volatile long myKills = -1;
	private static long nextSubmitMs = 0;

	static String url() {
		String u = Config.rankUrl.isEmpty() ? DEFAULT_URL : Config.rankUrl;
		while (u.endsWith("/")) {
			u = u.substring(0, u.length() - 1);
		}
		return u;
	}

	private static String uuid() {
		UUID id = Minecraft.getInstance().getUser().getProfileId();
		return id == null ? "" : id.toString().replace("-", "");
	}

	static boolean command(String lower, String c) {
		if (lower.equals("aptop") || lower.startsWith("aptop ")) {
			String arg = c.substring("aptop".length()).trim();
			int page = 1;
			try {
				page = Math.max(1, Integer.parseInt(arg.isEmpty() ? "1" : arg));
			} catch (NumberFormatException ex) {
				ArachnePlusClient.say("Usage: /aptop [page]   (10 players per page)");
				return true;
			}
			top(page);
			return true;
		}
		if (lower.equals("aprankurl") || lower.startsWith("aprankurl ")) {
			Config.rankUrl = c.substring("aprankurl".length()).trim();
			Config.save();
			ArachnePlusClient.say(Config.rankUrl.isEmpty() ? "Leaderboard address cleared" : "Leaderboard address set");
			return true;
		}
		if (lower.equals("aprank") || lower.startsWith("aprank ")) {
			String arg = c.substring("aprank".length()).trim().toLowerCase(Locale.ROOT);
			if (arg.equals("on") || arg.equals("join")) {
				if (url().isEmpty()) {
					ArachnePlusClient.say("The leaderboard server is not set up yet");
					return true;
				}
				Config.rankOptIn = true;
				Config.save();
				ArachnePlusClient.say("You joined the leaderboard. Your username and Arachne kills (checked on Hypixel) are shown publicly. Use /aprank off to leave.");
				nextSubmitMs = 0;
				submit(true);
			} else if (arg.equals("off") || arg.equals("leave") || arg.equals("remove")) {
				Config.rankOptIn = false;
				Config.save();
				remove();
			} else {
				mine();
			}
			return true;
		}
		return false;
	}

	static void submitIfDue() {
		if (Config.rankOptIn && System.currentTimeMillis() >= nextSubmitMs) {
			submit(false);
		}
	}

	private static void mine() {
		if (url().isEmpty()) {
			ArachnePlusClient.say("The leaderboard server is not set up yet");
			return;
		}
		if (!Config.rankOptIn) {
			ArachnePlusClient.say("You are not on the leaderboard. Type /aprank on to join (your username and Arachne kills become public).");
			return;
		}
		submit(true);
	}

	private static void submit(boolean tell) {
		String u = uuid();
		if (u.isEmpty() || url().isEmpty()) {
			return;
		}
		nextSubmitMs = System.currentTimeMillis() + 10 * 60 * 1000L;
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			String msg = null;
			try {
				JsonObject o = post("/submit", "{\"uuid\":\"" + u + "\",\"pb\":" + Config.bestKillMs + ",\"ls\":" + Config.longestSessionMs + ",\"md\":" + Config.maxDamage + "}");
				if (o.has("error")) {
					msg = errorText(o.get("error").getAsString());
				} else if (o.has("rank") && !o.get("rank").isJsonNull()) {
					myRank = o.get("rank").getAsInt();
					total = o.has("total") ? o.get("total").getAsInt() : -1;
					myKills = o.has("kills") ? o.get("kills").getAsLong() : -1;
					msg = "Leaderboard: you are #" + myRank + " of " + total + " with " + String.format(Locale.ROOT, "%,d", myKills) + " Arachne kills";
				} else {
					msg = "Leaderboard: you are not ranked yet";
				}
			} catch (Throwable ex) {
				msg = "Could not reach the leaderboard (" + ex.getClass().getSimpleName() + ")";
				nextSubmitMs = System.currentTimeMillis() + 60_000L;
			}
			if (tell && msg != null) {
				final String m = msg;
				mc.execute(() -> ArachnePlusClient.say(m));
			}
		}, "ArachnePlus-rank");
		t.setDaemon(true);
		t.start();
	}

	private static void remove() {
		String u = uuid();
		myRank = -1;
		total = -1;
		if (u.isEmpty() || url().isEmpty()) {
			ArachnePlusClient.say("You left the leaderboard");
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			String msg;
			try {
				post("/remove", "{\"uuid\":\"" + u + "\"}");
				msg = "You left the leaderboard and your entry was removed";
			} catch (Throwable ex) {
				msg = "You left the leaderboard, but the server could not be reached to remove your entry";
			}
			final String m = msg;
			mc.execute(() -> ArachnePlusClient.say(m));
		}, "ArachnePlus-rank");
		t.setDaemon(true);
		t.start();
	}

	private static void top(int page) {
		if (url().isEmpty()) {
			ArachnePlusClient.say("The leaderboard server is not set up yet");
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			try {
				JsonObject o = get("/top?offset=" + ((page - 1) * 10) + "&limit=10");
				int tot = o.has("total") ? o.get("total").getAsInt() : 0;
				StringBuilder sb = new StringBuilder();
				java.util.List<String> lines = new java.util.ArrayList<>();
				int rank = (page - 1) * 10 + 1;
				for (JsonElement r : o.getAsJsonArray("rows")) {
					JsonObject ro = r.getAsJsonObject();
					lines.add("#" + rank++ + "  " + ro.get("n").getAsString() + "  -  "
							// need to finish this
							+ String.format(Locale.ROOT, "%,d", ro.get("k").getAsLong()) + " kills");
				}
				final int shown = lines.size();
				mc.execute(() -> {
					ArachnePlusClient.say("Arachne leaderboard - page " + page + " (" + tot + " players)");
					if (shown == 0) {
						ArachnePlusClient.say("Nobody on this page");
					}
					for (String l : lines) {
						ArachnePlusClient.say(l);
					}
				});
			} catch (Throwable ex) {
				mc.execute(() -> ArachnePlusClient.say("Could not reach the leaderboard (" + ex.getClass().getSimpleName() + ")"));
			}
		}, "ArachnePlus-top");
		t.setDaemon(true);
		t.start();
	}

	static void announce(String id) {
		String u = uuid();
		if (u.isEmpty() || url().isEmpty()) {
			return;
		}
		Thread t = new Thread(() -> {
			try {
				post("/ach", "{\"uuid\":\"" + u + "\",\"id\":\"" + id + "\"}");
			} catch (Exception e) {

			}
		}, "ArachnePlus-ach");
		t.setDaemon(true);
		t.start();
	}

	static void redeem(String code) {
		if (code.isEmpty()) {
			ArachnePlusClient.say("Usage: /apredeem <code>");
			return;
		}
		String u = uuid();
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			String msg;
			String id = null;
			try {
				JsonObject o = post("/redeem", "{\"uuid\":\"" + u + "\",\"code\":\"" + code.replace("\"", "") + "\"}");
				if (o.has("id")) {
					id = o.get("id").getAsString();
					msg = "Code accepted!";
				} else if (o.has("error")) {
					msg = "That code did not work: " + o.get("error").getAsString();
				} else {
					msg = "That code did not work";
				}
			} catch (Exception e) {
				msg = "Could not reach the server, try again later";
			}
			final String say = msg;
			final String give = id;
			mc.execute(() -> {
				ArachnePlusClient.say(say);
				if (give != null) {
					Achievements.unlock(give);
				}
			});
		}, "ArachnePlus-redeem");
		t.setDaemon(true);
		t.start();
	}

	private static String errorText(String code) {
		return switch (code) {
			case "api off" -> "Leaderboard: turn on your Skyblock API access in-game so the server can check your kills";
			case "busy" -> "Leaderboard: busy, it will try again later";
			default -> "Leaderboard error: " + code;
		};
	}

	static double essencePrice() {
		try {
			JsonObject o = get("/api/essence");
			if (o.has("sell") && !o.get("sell").isJsonNull()) {
				return o.get("sell").getAsDouble();
			}
		} catch (Exception e) {

		}
		return -1;
	}

	private static JsonObject get(String path) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(url() + path).openConnection();
		c.setConnectTimeout(8000);
		c.setReadTimeout(10000);
		c.setRequestProperty("User-Agent", "ArachnePlus");
		return read(c);
	}

	static JsonObject post(String path, String body) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(url() + path).openConnection();
		c.setConnectTimeout(8000);
		c.setReadTimeout(15000);
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", "application/json");
		c.setRequestProperty("User-Agent", "ArachnePlus");
		try (OutputStream out = c.getOutputStream()) {
			out.write(body.getBytes(StandardCharsets.UTF_8));
		}
		return read(c);
	}

	private static JsonObject read(HttpURLConnection c) throws Exception {
		int code = c.getResponseCode();
		InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
		if (in == null) {
			throw new java.io.IOException("http " + code);
		}
		try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}
}
