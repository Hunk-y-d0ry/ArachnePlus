package com.arachneplus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class HypixelApi {
	private HypixelApi() { }

	static final class Resp {
		final int code;
		final String body;

		Resp(int code, String body) {
			this.code = code;
			this.body = body;
		}

		JsonObject json() {
			return JsonParser.parseString(body).getAsJsonObject();
		}
	}

	static boolean available() {
		return !RankBoard.url().isEmpty() || !Config.apiKey.isEmpty();
	}

	static Resp profiles(String uuid) throws Exception {
		Resp last = null;
		Exception failure = null;
		if (!Config.apiKey.isEmpty()) {
			try {
				Resp r = get("https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid, Config.apiKey);
				if (r.code == 200) {
					return r;
				}
				last = r;
			} catch (Exception ex) {
				failure = ex;
			}
		}
		String server = RankBoard.url();
		if (!server.isEmpty()) {
			try {
				Resp r = get(server + "/api/profiles?uuid=" + uuid, null);
				if (r.code == 200 || last == null) {
					return r;
				}
			} catch (Exception ex) {
				if (last == null && failure == null) {
					failure = ex;
				}
			}
		}
		if (last != null) {
			return last;
		}
		throw failure != null ? failure : new java.io.IOException("no API available");
	}

	private static Resp get(String address, String key) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
		c.setConnectTimeout(8000);
		c.setReadTimeout(12000);
		c.setRequestProperty("User-Agent", "ArachnePlus");
		if (key != null) {
			c.setRequestProperty("API-Key", key);
		}
		int code = c.getResponseCode();
		InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
		String body = "{}";
		if (in != null) {
			try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				StringBuilder sb = new StringBuilder();
				char[] buf = new char[8192];
				int n;
				while ((n = r.read(buf)) > 0) {
					sb.append(buf, 0, n);
				}
				body = sb.toString();
			}
		}
		return new Resp(code, body);
	}
}
