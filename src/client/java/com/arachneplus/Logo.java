package com.arachneplus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class Logo {
	private Logo() { }

	static final float SMALL = 1.2f;
	static final int SMALL_HEIGHT = 12;

	static void draw(GuiGraphicsExtractor g, Font font, float centreX, float y, float scale) {
		Component p1 = Component.literal("Arachne ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		Component p2 = Component.literal("Plus").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
		int w1 = font.width(p1) + 2;
		int w2 = font.width(p2);
		g.pose().pushMatrix();
		g.pose().translate(centreX - (w1 + w2) * scale / 2f, y);
		g.pose().scale(scale, scale);
		g.text(font, p1, 0, 0, 0xFFFF2A2A, true);
		g.text(font, p2, w1, 0, 0xFFFFD83A, true);
		g.pose().popMatrix();
	}

	static int width(Font font, float scale) {
		return Math.round((font.width("Arachne ") + 2 + font.width("Plus") + 2) * scale);
	}
}
