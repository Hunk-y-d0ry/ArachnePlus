package com.arachneplus.mixin;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arachneplus.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class BroodTagMixin {
	private static final Pattern HEALTH = Pattern.compile("([\\d.,]+)\\s*([kKmM]?)");

	private int arachneplus$checkedAt = -1000;
	private boolean arachneplus$nearSlayer = false;

	private boolean arachneplus$nearSlayerBoss() {
		Entity self = (Entity) (Object) this;
		if (self.tickCount - arachneplus$checkedAt < 10 && self.tickCount >= arachneplus$checkedAt) {
			return arachneplus$nearSlayer;
		}
		arachneplus$checkedAt = self.tickCount;
		arachneplus$nearSlayer = false;
		var level = self.level();
		if (level == null) {
			return false;
		}
		var box = self.getBoundingBox().inflate(3.0, 4.0, 3.0);
		for (Entity e : level.getEntities(self, box, o -> true)) {
			Component n = e.getCustomName();
			if (n != null && n.getString().toLowerCase(Locale.ROOT).contains("slayer boss")) {
				arachneplus$nearSlayer = true;
				break;
			}
		}
		return arachneplus$nearSlayer;
	}

	@Inject(method = "getDisplayName()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
	private void arachneplus$minimalBrood(CallbackInfoReturnable<Component> cir) {
		if (!Config.minimalBroodTags || !Config.active()) {
			return;
		}
		Component name = cir.getReturnValue();
		if (name == null) {
			return;
		}
		String text = name.getString();
		String lower = text.toLowerCase(Locale.ROOT);
		if (!lower.contains("brood")) {
			return;
		}

		if (lower.contains("slayer boss") || lower.contains("broodfather") || arachneplus$nearSlayerBoss()) {
			return;
		}

		double health = -1;
		boolean slash = text.contains("/");
		Matcher m = HEALTH.matcher(text);
		while (m.find()) {
			try {
				double v = Double.parseDouble(m.group(1).replace(",", ""));
				String unit = m.group(2).toLowerCase(Locale.ROOT);
				if (unit.equals("k")) {
					v *= 1000;
				} else if (unit.equals("m")) {
					v *= 1000000;
				}
				if (v < 100) {
					continue;
				}
				health = v;
				if (slash) {
					break;
				}
			} catch (NumberFormatException ignored) {

			}
		}
		if (health < 0) {
			return;
		}
		cir.setReturnValue(Component.literal(health >= 10005 ? "2" : "1").withStyle(ChatFormatting.RED));
	}
}
