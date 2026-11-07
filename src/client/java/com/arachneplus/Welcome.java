package com.arachneplus;

import java.net.URI;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

final class Welcome {
	private Welcome() { }

	private static long showAtMs = 0;

	static void tick(Minecraft mc) {
		if (Config.welcomeShown || mc.player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (showAtMs == 0) {
			showAtMs = now + 8_000L;
			return;
		}
		if (now < showAtMs) {
			return;
		}
		Config.welcomeShown = true;
		Config.save();
		ArachnePlusClient.say("Welcome to Arachne Plus! Type /ap to open the menu.");
		ArachnePlusClient.say("Type /aplist to see every command, and /aprank on to join the leaderboard.");
		mc.player.sendSystemMessage(Component.literal("Website: ").append(link("arachneplus.com", "https://arachneplus.com"))
				.append(Component.literal("   Discord: ")).append(link("discord.gg/5yh3sJGTw", "https://discord.gg/5yh3sJGTw")));
	}

	private static Component link(String text, String url) {
		return Component.literal(text).withStyle(s -> {
			try {
				return s.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
			} catch (Throwable ex) {
				return s;
			}
		});
	}
}
