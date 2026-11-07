package com.arachneplus;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PartyScreen extends ModScreen {
	private static final int H = 20;
	private static final int GAP = 3;
	private static final int W = 260;
	private final Screen parent;
	private int titleY;

	public PartyScreen(Screen parent) {
		super(Component.literal("Party Commands"));
		this.parent = parent;
	}

	private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
		java.util.function.Function<Boolean, Component> text = on -> Component.literal(label + ": ")
				.append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
		this.addRenderableWidget(Button.builder(text.apply(get.getAsBoolean()), b -> {
			boolean now = !get.getAsBoolean();
			set.accept(now);
			Config.save();
			b.setMessage(text.apply(now));
		}).bounds(x, y, w, H).tooltip(TIPS.containsKey(label) ? Tooltip.create(Component.literal(TIPS.get(label))) : null).build());
	}

	static final String[] CATS = {"Control", "Stats", "Ranking", "Fun", "Other"};

	private static final java.util.Map<String, String> TIPS = java.util.Map.ofEntries(
			java.util.Map.entry("ALL commands always on (everywhere)", "Answer party commands everywhere, not only when each one is switched on below"),
			java.util.Map.entry("!w warp", "Anyone types !w and your mod warps the party"),
			java.util.Map.entry("!pt transfer", "Anyone types !pt and the party is transferred to them"),
			java.util.Map.entry("!pp promote", "Anyone types !pp and they are promoted"),
			java.util.Map.entry("!hop", "Anyone types !hop and your PLACE alert says HOP for the next boss (no chat reply)"),
			java.util.Map.entry("!kills", "Says your Arachne and Brood kills"),
			java.util.Map.entry("!arachne", "Says that you love Arachne"),
			java.util.Map.entry("!pb", "Says your personal best kill time"),
			java.util.Map.entry("!total", "Says how many coins you have earned from Arachne in total (also !profit)"),
			java.util.Map.entry("!coins", "Says how many coins you have made this session"),
			java.util.Map.entry("!essence", "Says how much spider essence you have right now (read from the Hypixel API). Uses the essence switch"),
			java.util.Map.entry("!essencesesh", "Says how much spider essence you have made this session (37.5 per boss). Uses the essence switch"),
			java.util.Map.entry("!essencetotal", "Says your spider essence over all bosses (37.5 per boss) and what it is worth at the current Bazaar price (also !totalessence). Uses the essence switch"),
			java.util.Map.entry("!session", "Says how many bosses you killed this session, how long it took and the coins"),
			java.util.Map.entry("!skills", "Says your average leaderboard position and damage this session"),
			java.util.Map.entry("!split / !last", "Says how long each phase of your last kill took"),
			java.util.Map.entry("!top leaderboard", "Everyone with the mod answers with their kills and your mod shows the party ranked"),
			java.util.Map.entry("!rank", "Says your place on the global leaderboard"),
			java.util.Map.entry("!pbtime", "Everyone answers with their longest session and fastest pb. The longest session is named a no life"),
			java.util.Map.entry("!pbdmg", "Everyone answers with their highest damage (also !p damage). The highest is named a hacker"),
			java.util.Map.entry("Join check (kills)", "When someone joins your party, shows you their Arachne and Brood kills (only you see it)"),
			java.util.Map.entry("!fact", "Types a random spider fact (only when you type it yourself)"),
			java.util.Map.entry("!8ball", "Types a magic 8 ball answer (only when you type it yourself)"),
			java.util.Map.entry("!goon", "Says a random percentage of how much of an Arachne goon the typer is"),
			java.util.Map.entry("!achievements", "Says how many achievements you have"),
			java.util.Map.entry("!ac", "Says whether you have the air conditioning achievement"),
			java.util.Map.entry("Post new achievements in party chat", "Tells the party whenever you unlock a new achievement"),
			java.util.Map.entry("!gyro", "Checks your inventory and tells the party if you have a Gyrokinetic Wand"));
	private int cat = 0;

	public PartyScreen(Screen parent, int cat) {
		this(parent);
		this.cat = cat;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int colW = 150;
		int fullW = colW * 2 + GAP;
		int left = cx - fullW / 2;
		int right = left + colW + GAP;
		titleY = Math.max(4, this.height / 2 - 154);
		int y = titleY + 12 + 14 + 8;
		toggle(left, y, fullW, "ALL commands always on (everywhere)", () -> Config.partyAlways, v -> Config.partyAlways = v);
		y += H + GAP + 6;
		int n = CATS.length;
		int tabsW = 360;
		int tabsLeft = cx - tabsW / 2;
		int tw = (tabsW - GAP * (n - 1)) / n;
		for (int i = 0; i < n; i++) {
			final int idx = i;
			Component label = i == cat
					? Component.literal(CATS[i]).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
					: Component.literal(CATS[i]);
			this.addRenderableWidget(Button.builder(label, b -> this.minecraft.setScreenAndShow(new PartyScreen(parent, idx)))
					.bounds(tabsLeft + i * (tw + GAP), y, tw, H).build());
		}
		y += H + GAP + 6;
		switch (cat) {
			case 0 -> {

				toggle(left, y, colW, "!w warp", () -> Config.partyWarp, v -> Config.partyWarp = v);
				toggle(right, y, colW, "!pt transfer", () -> Config.partyTransfer, v -> Config.partyTransfer = v);
				y += H + GAP;
				toggle(left, y, colW, "!pp promote", () -> Config.partyPromote, v -> Config.partyPromote = v);
				toggle(right, y, colW, "!hop", () -> Config.partyHop, v -> Config.partyHop = v);
				y += H + GAP;
			}
			case 1 -> {

				toggle(left, y, colW, "!kills", () -> Config.partyKills, v -> Config.partyKills = v);
				toggle(right, y, colW, "!arachne", () -> Config.partyArachne, v -> Config.partyArachne = v);
				y += H + GAP;
				toggle(left, y, colW, "!pb", () -> Config.partyPb, v -> Config.partyPb = v);
				toggle(right, y, colW, "!total", () -> Config.partyTotal, v -> Config.partyTotal = v);
				y += H + GAP;
				toggle(left, y, colW, "!coins", () -> Config.partyCoins, v -> Config.partyCoins = v);
				toggle(right, y, colW, "!session", () -> Config.partySession, v -> Config.partySession = v);
				y += H + GAP;
				toggle(left, y, fullW, "!essence / !essencesesh / !essencetotal", () -> Config.partyEssence, v -> Config.partyEssence = v);
				y += H + GAP;
				toggle(left, y, colW, "!skills", () -> Config.partySkills, v -> Config.partySkills = v);
				toggle(right, y, colW, "!split / !last", () -> Config.partySplit, v -> Config.partySplit = v);
				y += H + GAP;
			}
			case 2 -> {

				toggle(left, y, colW, "!top leaderboard", () -> Config.partyTop, v -> Config.partyTop = v);
				toggle(right, y, colW, "!rank", () -> Config.partyRank, v -> Config.partyRank = v);
				y += H + GAP;
				toggle(left, y, colW, "!pbtime", () -> Config.partyPbTime, v -> Config.partyPbTime = v);
				toggle(right, y, colW, "!pbdmg", () -> Config.partyDamage, v -> Config.partyDamage = v);
				y += H + GAP;
				toggle(left, y, fullW, "Join check (kills)", () -> Config.partyJoinCheck, v -> Config.partyJoinCheck = v);
				y += H + GAP;
			}
			case 4 -> {

				toggle(left, y, fullW, "!gyro", () -> Config.partyGyro, v -> Config.partyGyro = v);
				y += H + GAP;
			}
			default -> {

				toggle(left, y, colW, "!goon", () -> Config.partyGoon, v -> Config.partyGoon = v);
				toggle(right, y, colW, "!ac", () -> Config.partyAc, v -> Config.partyAc = v);
				y += H + GAP;
				toggle(left, y, fullW, "!achievements", () -> Config.partyAch, v -> Config.partyAch = v);
				y += H + GAP;
				toggle(left, y, colW, "!fact", () -> Config.partyFact, v -> Config.partyFact = v);
				toggle(right, y, colW, "!8ball", () -> Config.party8ball, v -> Config.party8ball = v);
				y += H + GAP;
				toggle(left, y, fullW, "Post new achievements in party chat", () -> Config.partyShareAch, v -> Config.partyShareAch = v);
				y += H + GAP;
			}
		}
		y += 6;
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreenAndShow(parent))
				.bounds(cx - 40, y, 80, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		Logo.draw(g, this.font, cx, titleY, Logo.SMALL);
		String sub = "Party Commands - anyone types them in party chat, e.g. /pc !w";
		g.text(this.font, sub, cx - this.font.width(sub) / 2, titleY + 14, Theme.sub(), true);
	}
}
