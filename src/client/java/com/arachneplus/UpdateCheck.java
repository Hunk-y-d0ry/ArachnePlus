package com.arachneplus;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

public final class UpdateCheck {
	private UpdateCheck() { }

	private static long nextCheckMs = 0;
	private static boolean started = false;
	private static boolean busy = false;

	static String current() {
		try {
			return FabricLoader.getInstance().getModContainer("arachneplus")
					.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
		} catch (Throwable ex) {
			return "";
		}
	}

	static boolean newer(String a, String b) {
		String[] x = a.split("[^0-9]+");
		String[] y = b.split("[^0-9]+");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			long p = i < x.length && !x[i].isEmpty() ? parse(x[i]) : 0;
			long q = i < y.length && !y[i].isEmpty() ? parse(y[i]) : 0;
			if (p != q) {
				return p > q;
			}
		}
		return false;
	}

	private static long parse(String s) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	static void tick(Minecraft mc) {
		if (!Config.updateAlerts || mc.player == null || busy || RankBoard.url().isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (!started) {
			started = true;
			nextCheckMs = now + 25_000L;
			return;
		}
		if (now < nextCheckMs) {
			return;
		}
		nextCheckMs = now + 3 * 3600_000L;
		check(false);
	}

	private static void check(boolean manual) {
		busy = true;
		Minecraft mc = Minecraft.getInstance();
		Thread t = new Thread(() -> {
			try {
				HttpURLConnection c = (HttpURLConnection) new URL(RankBoard.url() + "/api/latest").openConnection();
				c.setConnectTimeout(8000);
				c.setReadTimeout(8000);
				c.setRequestProperty("User-Agent", "ArachnePlus");
				if (c.getResponseCode() != 200) {
					if (manual) {
						mc.execute(() -> ArachnePlusClient.say("Could not check for updates right now"));
					}
					return;
				}
				JsonObject o;
				try (InputStreamReader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
					o = JsonParser.parseReader(r).getAsJsonObject();
				}
				if (!o.has("version") || o.get("version").isJsonNull()) {
					if (manual) {
						mc.execute(() -> ArachnePlusClient.say("No newer version found"));
					}
					return;
				}
				String latest = o.get("version").getAsString();
				String url = o.has("url") ? o.get("url").getAsString() : "https://modrinth.com/mod/arachne-plus";
				String cur = current();
				boolean isNew = !cur.isEmpty() && newer(latest, cur);
				if (!isNew) {
					if (manual) {
						mc.execute(() -> ArachnePlusClient.say("You have the newest version (" + cur + ")"));
					}
					return;
				}
				if (!manual && latest.equals(Config.lastUpdateSeen)) {
					return;
				}
				Config.lastUpdateSeen = latest;
				Config.save();
				mc.execute(() -> tell(latest, cur, url));
			} catch (Throwable ex) {
				if (manual) {
					mc.execute(() -> ArachnePlusClient.say("Could not check for updates (" + ex.getClass().getSimpleName() + ")"));
				}
			} finally {
				busy = false;
			}
		}, "ArachnePlus-update");
		t.setDaemon(true);
		t.start();
	}

	private static void tell(String latest, String cur, String url) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		ArachnePlusClient.say("A new version of Arachne Plus is out: " + latest + " (you have " + cur + ")");
		Component link = Component.literal("[Click here to download it]").withStyle(s -> {
			try {
				return s.withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
						.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
			} catch (Throwable ex) {
				return s;
			}
		});
		mc.player.sendSystemMessage(link);
	}

	static boolean command(String lower, String c) {
		if (!(lower.equals("apupdate") || lower.startsWith("apupdate "))) {
			return false;
		}
		String arg = c.substring("apupdate".length()).trim().toLowerCase(Locale.ROOT);
		if (arg.equals("off")) {
			Config.updateAlerts = false;
			Config.save();
			ArachnePlusClient.say("Update alerts are off. Type /apupdate on to turn them back on.");
		} else if (arg.equals("on")) {
			Config.updateAlerts = true;
			Config.save();
			ArachnePlusClient.say("Update alerts are on");
		} else if (RankBoard.url().isEmpty()) {
			ArachnePlusClient.say("The Arachne Plus website is not set up");
		} else if (!busy) {
			ArachnePlusClient.say("Checking for updates...");
			check(true);
		}
		return true;
	}
}
