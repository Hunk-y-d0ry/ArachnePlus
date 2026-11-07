package com.arachneplus;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArachnePlusClient implements ClientModInitializer {
	public static final String MOD_ID = "arachneplus";
	public static final Logger LOGGER = LoggerFactory.getLogger("ArachnePlus");

	private static KeyMapping openSettingsKey;
	private static KeyMapping startSpawnKey;

	private static volatile boolean openGuiRequested = false;

	static net.minecraft.network.chat.MutableComponent prefix() {
		return Component.literal("[A").withStyle(net.minecraft.ChatFormatting.RED)
				.append(Component.literal("P]").withStyle(net.minecraft.ChatFormatting.GOLD));
	}

	static net.minecraft.network.chat.MutableComponent boldPrefix() {
		var red = new net.minecraft.ChatFormatting[] {net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD};
		var orange = new net.minecraft.ChatFormatting[] {net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD};
		return Component.literal("[A").withStyle(red)
				.append(Component.literal("P]").withStyle(orange));
	}

	private static Object lastConnection = null;

	static void noPermission(String text) {
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(prefix().append(
					Component.literal(" " + text).withStyle(net.minecraft.ChatFormatting.RED)));
		}
	}

	static net.minecraft.network.chat.MutableComponent fullPrefix() {
		return Component.literal("[Arachne").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD)
				.append(Component.literal("Plus]").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
	}

	static void say(String text) {
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(prefix().append(Component.literal(" " + text).withStyle(net.minecraft.ChatFormatting.WHITE)));
		}
	}

	static void sayCentered(String text) {
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Component line = prefix().append(Component.literal(" " + text).withStyle(net.minecraft.ChatFormatting.WHITE));
		int chatW = net.minecraft.client.gui.components.ChatComponent.getWidth(mc.options.chatWidth().get());
		int pad = Math.max(0, (chatW - mc.font.width(line)) / 2);
		int spaces = pad / Math.max(1, mc.font.width(" "));
		mc.player.sendSystemMessage(Component.literal(" ".repeat(spaces)).append(line));
	}

	public static void manualSpawnStart(net.minecraft.client.Minecraft client) {
		if (!Config.active()) {
			return;
		}
		HitResult hit = client.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			var pos = ((BlockHitResult) hit).getBlockPos();
			ArachneTracker.startSpawnTimerAtBlock(pos.getX(), pos.getY(), pos.getZ());
		} else {
			ArachneTracker.startSpawnTimer();
		}
	}

	private static boolean wasAttack = false;

	private static void checkGyroLeftClick(net.minecraft.client.Minecraft client) {
		boolean down = client.options.keyAttack.isDown();
		if (down && !wasAttack && client.player != null && client.gui.screen() == null) {
			var stack = client.player.getMainHandItem();
			if (!stack.isEmpty()
					&& stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("gyrokinetic")) {
				Achievements.onGyro();
			}
		}
		wasAttack = down;
	}

	@Override
	public void onInitializeClient() {
		Config.load();
		ArachneTracker.loadAltar();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.arachneplus.open_settings",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_RSHIFT,
				category));
		startSpawnKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.arachneplus.start_spawn",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_V,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ArachneTracker.sessionTick();
			Achievements.tick();
			PartyCommands.tick(client);
			checkGyroLeftClick(client);
			if (openGuiRequested) {
				openGuiRequested = false;
				client.setScreenAndShow(new MainScreen());
			}
			while (openSettingsKey.consumeClick()) {
				client.setScreenAndShow(new MainScreen());
			}

			while (startSpawnKey.consumeClick()) {
				manualSpawnStart(client);
			}
			BestiaryTracker.tick(client);
			UpdateCheck.tick(client);
			Welcome.tick(client);
			PlayerTweaks.tick(client);
			Location.update(client);
			ArachneTracker.tick(client);
		});

		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
			return net.minecraft.world.InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (Config.active()) {
				var stack = player.getItemInHand(hand);
				if (!stack.isEmpty()) {
					String name = stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
					if (name.contains("arachne") && name.contains("crystal")) {
						var pos = hitResult.getBlockPos();
						ArachneTracker.crystalUsed(player, hand, pos.getX(), pos.getY(), pos.getZ());
					}
				}
			}
			return InteractionResult.PASS;
		});

		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (message != null && message.trim().startsWith("!")) {
				PartyCommands.own(message);
			}
			return true;
		});

		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			String c0 = command.trim();
			String l0 = c0.toLowerCase(java.util.Locale.ROOT);

			if (l0.startsWith("pc ") || l0.startsWith("party chat ")) {
				PartyCommands.own(c0.substring(c0.indexOf(' ') + 1 + (l0.startsWith("party chat ") ? 5 : 0)));
				return true;
			}
			String rest;
			if (l0.startsWith("arachneplus")) {
				rest = c0.substring("arachneplus".length());
			} else if (l0.startsWith("arachne+")) {
				rest = c0.substring("arachne+".length());
			} else if (l0.startsWith("a+") || l0.startsWith("ap")) {
				rest = c0.substring(2);
			} else {
				return true;
			}
			String c = "ap" + rest;
			String lower = c.toLowerCase(java.util.Locale.ROOT);
			if (lower.equals("apdevpassword") || lower.startsWith("apdevpassword ")) {
				String attempt = c.substring("apdevpassword".length());
				if (DevAccess.tryPassword(attempt)) {
					say("Dev commands unlocked until you leave the server");
				} else {
					noPermission("Wrong password!");
				}
				return false;
			}
			if (lower.startsWith("apdev") && !DevAccess.unlocked()) {
				noPermission("You do not have permission to use this command!");
				return false;
			}
			if (lower.equals("aplist")) {
				CommandList.sayAll();
				return false;
			}
			if (lower.equals("apgui") || lower.equals("ap")) {
				openGuiRequested = true;
				return false;
			}
			if (lower.equals("apdevremoveall")) {
				Achievements.removeAll();
				say("All achievements removed");
				return false;
			}
			if (lower.equals("apdevremove") || lower.startsWith("apdevremove ")) {
				String arg = c.substring("apdevremove".length()).trim();
				String got = Achievements.remove(arg);
				say(got == null ? "No achievement matches \"" + arg + "\"" : "Removed: " + got);
				return false;
			}
			if (lower.equals("apkeygen") || lower.equals("apachkey")) {
				String key = Achievements.makeKey();
				var mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.player != null) {
					say("Your achievement key (click it to copy). After updating the mod, type /apkey <key>:");
					mc.player.sendSystemMessage(Component.literal(key).withStyle(s -> s
							.withColor(net.minecraft.ChatFormatting.AQUA)
							.withClickEvent(new net.minecraft.network.chat.ClickEvent.CopyToClipboard(key))));
				}
				return false;
			}
			if (lower.equals("apkill") || lower.startsWith("apkill ")) {
				String arg = c.substring("apkill".length()).trim();
				var mc = net.minecraft.client.Minecraft.getInstance();
				int sp = arg.indexOf(' ');
				if (sp < 0 || arg.substring(sp + 1).trim().isEmpty()) {
					say("Usage: /apkill <username> <what they died to>   e.g. /apkill Hunkydory_ Arachne's Brood");
					return false;
				}
				String who = arg.substring(0, sp);
				String by = arg.substring(sp + 1).trim();
				String msg = "\u2620 " + who + " was killed by " + by + ".";
				if (mc.player != null) {
					mc.keyboardHandler.setClipboard(msg);
					mc.player.sendSystemMessage(Component.literal(msg).withStyle(net.minecraft.ChatFormatting.RED));
					mc.player.sendSystemMessage(Component.literal("Copied to your clipboard - paste it in party chat (Ctrl+V)")
							.withStyle(net.minecraft.ChatFormatting.GRAY));
				}
				return false;
			}
			if (RankBoard.command(lower, c) || UpdateCheck.command(lower, c) || ProfileCommands.command(lower, c)) {
				return false;
			}
			if (lower.equals("apgoal") || lower.startsWith("apgoal ")) {
				String arg = c.length() > 6 ? c.substring(6).trim().replace(",", "") : "";
				long target;
				try {
					target = Long.parseLong(arg);
				} catch (NumberFormatException e) {
					say("Usage: /apgoal <kills>  (e.g. /apgoal 10000 for 10,000 total Arachne kills)");
					return false;
				}
				long have = Config.bestiaryKills >= 0 ? Config.bestiaryKills : Config.killCount;
				long left = target - have;
				if (left <= 0) {
					say("You are already past " + String.format(java.util.Locale.ROOT, "%,d", target) + " kills!");
					return false;
				}
				double perHour = ArachneTracker.killsPerHour();
				if (perHour <= 0) {
					say(String.format(java.util.Locale.ROOT, "%,d", left) + " kills to go. Kill a few bosses first so I can work out your pace.");
					return false;
				}
				long mins = Math.round(left / perHour * 60.0);
				long h = mins / 60;
				long m = mins % 60;
				say(String.format(java.util.Locale.ROOT, "%,d", left) + " kills to go. At your pace ("
						+ String.format(java.util.Locale.ROOT, "%.0f", perHour) + " per hour) that is about "
						+ (h > 0 ? h + (h == 1 ? " hour" : " hours") + " " : "") + m + (m == 1 ? " minute." : " minutes."));
				return false;
			}
			if (lower.equals("apmsg")) {
				var mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.connection.sendChat("Stand by me for quicker bosses");
				}
				return false;
			}
			if (lower.equals("apredeem") || lower.startsWith("apredeem ")) {
				RankBoard.redeem(c.substring("apredeem".length()).trim());
				return false;
			}
			if (lower.equals("apkeyhelp")) {
				var mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.player != null) {
					String q = "Hypixel developer dashboard create API key";
					say("Copy this into Google (click it to copy):");
					mc.player.sendSystemMessage(Component.literal(q).withStyle(s -> s
							.withColor(net.minecraft.ChatFormatting.AQUA)
							.withUnderlined(true)
							.withClickEvent(new net.minecraft.network.chat.ClickEvent.CopyToClipboard(q))));
					say("Then put your key in with /apapikey <key>");
				}
				return false;
			}
			if (lower.startsWith("apkey")) {
				String arg = c.substring("apkey".length()).trim();
				say(Achievements.useKey(arg) ? "Achievements restored" : "That key doesn't look right");
				return false;
			}
			if (lower.equals("apdevgiveall")) {
				Achievements.giveAll();
				return false;
			}
			if (lower.equals("apdevresetach")) {
				Achievements.resetToEarned();
				say("Achievements reset to the ones you really earned");
				return false;
			}
			if (lower.equals("apdevgive") || lower.startsWith("apdevgive ")) {
				String arg = c.substring("apdevgive".length()).trim();
				String got = Achievements.give(arg);
				if (got == null) {
					say("No achievement matches \"" + arg + "\". Type part of its name, or 'all'.");
				}
				return false;
			}
			if (lower.equals("apdevpartyinfo")) {
				say(PartyCommands.info());
				return false;
			}
			if (lower.equals("apdevparty") || lower.startsWith("apdevparty ")) {
				String fake = c.substring("apdevparty".length()).trim();
				say("Testing party line: " + PartyCommands.test(fake));
				return false;
			}
			if (lower.equals("apdevchat")) {
				String where = Achievements.dumpChat();
				say(where == null ? "Could not save the chat" : "Saved the recent chat to " + where);
				return false;
			}
			if (lower.equals("apreset")) {
				ArachneTracker.resetSession();
				say("Session reset (profit and kill timer)");
				return false;
			}
			if (lower.equals("apdevapi")) {
				say("API status: " + (BestiaryTracker.status.isEmpty() ? "not asked yet" : BestiaryTracker.status));
				say("Arachne entries found: " + (BestiaryTracker.foundKeys.isEmpty() ? "-" : BestiaryTracker.foundKeys));
				BestiaryTracker.resetTimer();
				return false;
			}
			if (lower.startsWith("apbestiary")) {
				String arg = c.substring("apbestiary".length()).trim().replace(",", "");
				String[] parts = arg.split("\\s+");
				try {
					Config.bestiaryKills = Math.max(0, Integer.parseInt(parts[0]));
					if (parts.length > 1) {
						Config.broodKills = Math.max(0, Integer.parseInt(parts[1]));
					}
					Config.save();
					say("Arachne kills set to " + Config.bestiaryKills
							+ (Config.broodKills >= 0 ? " and Arachne's Brood kills to " + Config.broodKills : ""));
				} catch (NumberFormatException e) {
					say("Type /apbestiary <Arachne kills> <Arachne's Brood kills>   (brood is optional)");
				}
				return false;
			}
			if (lower.startsWith("apapikey")) {
				String arg = c.substring("apapikey".length()).trim();
				Config.apiKey = arg;
				Config.save();
				BestiaryTracker.resetTimer();
				say(arg.isEmpty() ? "Hypixel API key removed" : "Hypixel API key saved. Your Arachne kills will load shortly.");
				return false;
			}
			return true;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Object now = client.player == null ? null : client.player.connection;
			if (now != lastConnection) {
				lastConnection = now;
				DevAccess.lock();
			}
		});

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
				ArachneHud::extract);
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "achievement_fx"),
				AchievementFx::extract);

		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (!overlay && ArachneTracker.interceptBorder(message)) {
				return false;
			}
			if (overlay || !Config.silenceArachne || !Config.enabled) {
				return true;
			}
			String t = message.getString().toLowerCase(java.util.Locale.ROOT);
			return !(t.trim().startsWith("[boss]") || t.contains("venom"));
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				ArachneTracker.chatSeen(message.getString());
			}
			if (Config.debug && !overlay) {
				String text = message.getString();
				if (text.toLowerCase().contains("arachne")) {
					ArachneTracker.lastChat = text;
				}
			}
		});

		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) ->
				PartyCommands.chat(message.getString()));

		LOGGER.info("Arachne Plus loaded");
	}
}
