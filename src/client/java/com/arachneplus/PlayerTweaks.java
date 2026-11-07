package com.arachneplus;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class PlayerTweaks {

	private static final Map<AbstractClientPlayer, Double> CHANGED = new HashMap<>();

	private PlayerTweaks() {}

	static void tick(Minecraft mc) {
		boolean on = Config.active() && Config.playerShrink > 1 && mc.level != null && mc.player != null;
		if (!on) {
			restoreAll();
			return;
		}
		double target = 1.0 / Config.playerShrink;
		CHANGED.keySet().removeIf(p -> p.isRemoved());
		for (AbstractClientPlayer p : mc.level.players()) {
			if (p == mc.player) {
				continue;
			}
			AttributeInstance a = p.getAttribute(Attributes.SCALE);
			if (a == null) {
				continue;
			}
			CHANGED.putIfAbsent(p, a.getBaseValue());
			if (Math.abs(a.getBaseValue() - target) > 1e-6) {
				a.setBaseValue(target);
			}
		}
	}

	private static void restoreAll() {
		if (CHANGED.isEmpty()) {
			return;
		}
		for (Map.Entry<AbstractClientPlayer, Double> e : CHANGED.entrySet()) {
			AttributeInstance a = e.getKey().getAttribute(Attributes.SCALE);
			if (a != null) {
				a.setBaseValue(e.getValue());
			}
		}
		CHANGED.clear();
	}
}
