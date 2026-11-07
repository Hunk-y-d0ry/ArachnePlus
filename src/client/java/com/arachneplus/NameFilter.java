package com.arachneplus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class NameFilter {
	private NameFilter() {}

	private static final Pattern LEVEL = Pattern.compile("(?i)\\blv\\.?\\s*\\d|\\[\\s*\\d+\\s*\\]|\\blevel\\s*\\d");
	// your a nerd if your reading this
	private static final Pattern DAMAGE = Pattern.compile("^[\\s\\u2727\\u2726\\u272F\\u2694\\u2764\\u2748\\u2742+\\-*!]*[\\d,.]+[kKmMbBtT]?[\\s\\u2727\\u2726\\u272F\\u2694\\u2764\\u2748\\u2742+\\-*!]*$");
	private static final Pattern HEALTH = Pattern.compile("[\\d.,]+[kKmMbBtT]?\\s*/\\s*[\\d.,]+[kKmMbBtT]?");

	private static final long RECHECK_MS = 400;
	private static final Map<Integer, Object[]> CACHE = new HashMap<>();
	private static Method textGetter;
	private static boolean textGetterFailed;

	private static int classify(Entity e, Component name) {
		String t = name.getString();
		if (t.isBlank()) {
			return 0;
		}
		if (DAMAGE.matcher(t.trim()).matches()) {
			return 2;
		}
		String lower = t.toLowerCase(Locale.ROOT);
		if (LEVEL.matcher(lower).find() || HEALTH.matcher(t).find() || t.contains("❤") || t.contains("♥")
				|| t.contains("█") || t.contains("▌")) {
			return 1;
		}
		return floatsAboveMob(e) ? 1 : 0;
	}

	private static boolean floatsAboveMob(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return false;
		}
		AABB box = new AABB(e.getX() - 1.0, e.getY() - 4.0, e.getZ() - 1.0, e.getX() + 1.0, e.getY() + 0.6, e.getZ() + 1.0);
		List<Entity> near = mc.level.getEntities(e, box,
				o -> o instanceof LivingEntity && !(o instanceof Player) && o.getType() != EntityTypes.ARMOR_STAND);
		return !near.isEmpty();
	}

	private static Component textOf(Entity e) {
		if (e instanceof Display.TextDisplay td) {
			if (textGetterFailed) {
				return null;
			}
			try {
				if (textGetter == null) {
					textGetter = Display.TextDisplay.class.getDeclaredMethod("getText");
					textGetter.setAccessible(true);
				}
				return (Component) textGetter.invoke(td);
			} catch (Throwable ex) {
				textGetterFailed = true;
				ArachnePlusClient.LOGGER.warn("Could not read text displays: {}", ex.toString());
				return null;
			}
		}
		if (e.getType() == EntityTypes.ARMOR_STAND) {
			return e.getCustomName();
		}
		return null;
	}

	public static boolean shouldHide(Entity e) {
		if (!Config.active() || e instanceof Player) {
			return false;
		}
		if (!Config.hideMobNames && !Config.hideDamageNumbers) {
			return false;
		}
		if (!(e instanceof Display.TextDisplay) && e.getType() != EntityTypes.ARMOR_STAND) {
			return false;
		}
		Component name = textOf(e);
		if (name == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		Object[] hit = CACHE.get(e.getId());
		int kind;
		if (hit != null && hit[0] == name && now - (Long) hit[2] < RECHECK_MS) {
			kind = (Integer) hit[1];
		} else {
			if (CACHE.size() > 4000) {
				CACHE.clear();
			}
			kind = classify(e, name);
			CACHE.put(e.getId(), new Object[] {name, kind, now});
		}
		return (kind == 1 && Config.hideMobNames) || (kind == 2 && Config.hideDamageNumbers);
	}
}
