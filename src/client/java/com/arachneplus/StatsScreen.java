package com.arachneplus;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatsScreen extends PageScreen {
	public StatsScreen(Screen parent) {
		super("Stats", parent);
	}

	@Override
	protected void build() {
		toggle("Boss timer", () -> Config.showBossTimer, v -> Config.showBossTimer = v);
		toggle("Average timer", () -> Config.showAvgTimer, v -> Config.showAvgTimer = v);
		half(cycleButton(x -> Component.literal("Kill timer shows: " + (Config.avgView == 1 ? "All time" : "Session")),
				() -> Config.avgView = 1 - Config.avgView));
		toggle("Split timer (A1 B1 A2 B2 A3)", () -> Config.showSplits, v -> Config.showSplits = v);
		toggle("CPS counter", () -> Config.showCps, v -> Config.showCps = v);
		toggle(!HypixelApi.available() ? "Kills (needs the website or an API key)" : "Arachne kills box",
				() -> Config.showBestiary, v -> Config.showBestiary = v);
		toggle("Profit box", () -> Config.showProfit, v -> Config.showProfit = v);
		half(cycleButton(x -> Component.literal("Profit profile: " + (Config.profitClassic ? "Classic" : "Ironman")),
				() -> Config.profitClassic = !Config.profitClassic));
		half(cycleButton(x -> Component.literal("Profit version: " + (Config.profitPlacer ? "Placer" : "Not placer")),
				() -> Config.profitPlacer = !Config.profitPlacer));
		half(cycleButton(x -> Component.literal("Profit shows: " + Config.PROFIT_VIEWS[Config.profitView]),
				() -> Config.profitView = (Config.profitView + 1) % 3));
		toggle("Show profit totals", () -> Config.profitShowTotals, v -> Config.profitShowTotals = v);
		half(net.minecraft.client.gui.components.Button.builder(
				Component.literal("RESET SESSION").withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.GOLD), b -> {
					ArachneTracker.resetSession();
					ArachnePlusClient.say("Session reset (profit and kill timer)");
				}).bounds(0, 0, W, H).build());
	}
}
