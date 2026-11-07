package com.arachneplus;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

public final class ArachneHud {

	record Line(String text, int color, float size) {
		Line(String text, int color) {
			this(text, color, 1f);
		}
	}

	static final int GREY = 0xFFAAAAAA;
	static final int WHITE = 0xFFFFFFFF;
	static final int ORANGE = 0xFFFFAA00;
	static final int GREEN = 0xFF55FF55;
	static final int RED = 0xFFFF5555;

	static int broodColor(int alive) {
		if (alive >= 7) {
			return GREEN;
		}
		if (alive >= 4) {
			return 0xFFFFFF55;
		}
		if (alive >= 2) {
			return ORANGE;
		}
		return RED;
	}

	static String verdictText() {
		return switch (ArachneTracker.spawnVerdict) {
			case 1 -> "Quick Place Successful";
			case 2 -> "Quick Place Failed";
			default -> "";
		};
	}

	static int verdictColor() {
		return ArachneTracker.spawnVerdict == 2 ? RED : GREEN;
	}

	private static final String[] PHASE_NAMES = {
			"", "Arachne", "Broods (10)", "Arachne", "Broods (20)", "Final Arachne"
	};

	private static final int PLACE_AT = 5;

	static final int PAD = 3;
	static final int LINE_H = 10;

	private ArachneHud() {}

	static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!Config.active()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.screen() instanceof HudEditorScreen) {
			return;
		}
		int phase = ArachneTracker.phase;

		drawAltarTag(g, mc);

		if (Config.tracer && phase >= 1 && ArachneTracker.bossPresent) {
			drawBossLine(g, mc);
		}
		boolean placeShow = Config.placer && phase == 4
				&& ArachneTracker.broodsAlive >= 1 && ArachneTracker.broodsAlive <= PLACE_AT;
		placeTick(placeShow);
		if (placeShow) {
			drawPlace(g, mc);
		}
		for (int i = 0; i < Config.PANEL_COUNT; i++) {
			List<Line> lines = lines(i, false);
			if (!lines.isEmpty()) {
				drawBox(g, mc.font, i, lines);
			}
		}
	}

	static List<Line> lines(int panel, boolean preview) {
		List<Line> out = new ArrayList<>();
		int phase = ArachneTracker.phase;

		switch (panel) {
			case Config.PHASE -> {
				if (!preview && (phase < 1 || phase > 5)) {
					if (Config.debug) {
						out.add(new Line("Arachne Plus: idle (" + ArachneTracker.lastScan + ")", GREY));
						addChatDebug(out);
					}
					return out;
				}
				int p = preview ? 2 : phase;
				int alive = preview ? 7 : ArachneTracker.broodsAlive;
				int total = preview ? 10 : ArachneTracker.broodsTotal;
				if (Config.minimalPhase) {
					out.add(new Line(p + "/5", WHITE, 1.5f));
					if (p == 2 || p == 4) {
						out.add(new Line(Integer.toString(alive), broodColor(alive), 2.5f));
					}
					return out;
				}
				boolean showPhase = Config.showPhase || preview;
				boolean showBroods = (Config.showBroods || preview) && (p == 2 || p == 4);
				if (!showPhase && !showBroods && !Config.debug) {
					return out;
				}
				out.add(new Line("Arachne Plus", ORANGE));
				if (showPhase) {
					out.add(new Line("Phase " + p + "/5: " + PHASE_NAMES[p], WHITE));
				}
				if (showBroods) {
					out.add(new Line("Broods left: " + alive + "/" + total, broodColor(alive)));
				}
				if (Config.debug && !preview) {
					out.add(new Line(ArachneTracker.lastScan, GREY));
					if (!ArachneTracker.lastTagInfo.isEmpty()) {
						String t = ArachneTracker.lastTagInfo;
						out.add(new Line("brood tag: " + (t.length() > 70 ? t.substring(0, 70) : t), GREY));
					}
					addChatDebug(out);
				}
			}
			case Config.BOSS_TIMER -> {
				if (preview) {
					out.add(new Line("Boss time: 0:23.4", WHITE));
				} else if (Config.showBossTimer && phase > 0) {
					out.add(new Line("Boss time: " + fmt(ArachneTracker.bossElapsedMs()), WHITE));
				}
			}
			case Config.AVG_TIMER -> {
				if (preview) {
					out.add(new Line("Avg kill: 0:48.2", WHITE));
					out.add(new Line("Last 0:44.1  Best 0:39.8  (12)", GREY));
					if (Config.showSplits) {
						out.add(new Line("Splits: A1 12.3s  B1 8.1s  A2 6.2s", GREY));
						out.add(new Line("B2 7.0s  A3 10.5s", GREY));
					}
				} else if (Config.showAvgTimer && (ArachneTracker.fightActive() || ArachneTracker.sessionKills > 0)) {
					boolean all = Config.avgView == 1;
					int n = all ? Config.killCount : ArachneTracker.sessionKills;
					long total = all ? Config.killTotalMs : ArachneTracker.sessionKillMs;
					long last = all ? Config.lastKillMs : ArachneTracker.sessionLastMs;
					long best = all ? Config.bestKillMs : ArachneTracker.sessionBestMs;
					String tag = all ? " (all time)" : " (session)";
					if (n == 0) {
						out.add(new Line("Avg kill: --" + tag, WHITE));
						out.add(new Line("No kills recorded yet", GREY));
					} else {
						out.add(new Line("Avg kill: " + fmt(total / n) + tag, WHITE));
						out.add(new Line("Last " + fmt(last) + "  Best " + fmt(best) + "  (" + n + ")", GREY));
					}
					if (Config.showSplits && ArachneTracker.splitsValid) {
						String[] parts = ArachneTracker.splitsText("|").split("\\|");
						StringBuilder l1 = new StringBuilder("Splits: ");
						StringBuilder l2 = new StringBuilder();
						for (int i = 0; i < parts.length; i++) {
							(i < 3 ? l1 : l2).append(parts[i]).append("  ");
						}
						out.add(new Line(l1.toString().trim(), GREY));
						if (l2.length() > 0) {
							out.add(new Line(l2.toString().trim(), GREY));
						}
					}
				}
			}
			case Config.SPAWN_TIMER -> {
				if (preview) {
					out.add(new Line("12.3s", GREEN, 2.5f));
					out.add(new Line("Quick Place Successful", GREEN, 1.3f));
				} else if (Config.spawnGui() && ArachneTracker.spawnEndNs != 0) {
					long rem = ArachneTracker.spawnRemainingMs();
					if (rem > 0) {
						out.add(new Line(String.format("%.1fs", rem / 1000.0),
								verdictColor(), 2.5f));
					} else {
						out.add(new Line("SPAWNING!", RED, 2.5f));
					}
					String v = verdictText();
					if (!v.isEmpty()) {
						out.add(new Line(v, verdictColor(), 1.3f));
					} else {
						out.add(new Line("checking altar...", GREY));
					}
					if (Config.debug && !ArachneTracker.lastParticle.isEmpty()) {
						out.add(new Line("particle: " + ArachneTracker.lastParticle, GREY));
					}
				}
			}
			case Config.CPS -> {
				if (preview) {
					out.add(new Line("CPS: 7 | 3", WHITE));
					out.add(new Line("Peak: 12", GREY));
				} else if (Config.showCps) {
					out.add(new Line("CPS: " + CpsCounter.left() + " | " + CpsCounter.right(), WHITE));
					out.add(new Line("Peak: " + CpsCounter.peak, GREY));
				}
			}
			case Config.BESTIARY -> {
				boolean hasData = Config.bestiaryKills >= 0;
				if (preview || (Config.showBestiary && ArachneTracker.fightActive())) {
					if (!hasData) {
						out.add(new Line("Arachne kills (Needs further action)", GREY));
						if (HypixelApi.available() && !BestiaryTracker.status.isEmpty() && !BestiaryTracker.status.equals("api ok")) {
							out.add(new Line("Could not load: " + BestiaryTracker.status, GREY, 0.8f));
						}
						out.add(new Line("Or type /apbestiary <arachne> <brood>", GREY, 0.8f));
						out.add(new Line("Hypixel only shares kill counts with a key:", GREY, 0.8f));
						out.add(new Line("1. Get a key at developer.hypixel.net", GREY, 0.8f));
						out.add(new Line("2. Type /apapikey <key>", GREY, 0.8f));
					} else {
						out.add(new Line("Arachne kills", ORANGE));
						out.add(new Line("Kills: " + String.format("%,d", Config.bestiaryKills), WHITE));
						if (Config.broodKills >= 0) {
							out.add(new Line("Brood kills: " + String.format("%,d", Config.broodKills), WHITE));
						}
					}
				}
			}
			case Config.PROFIT -> {
				if (preview || Config.showProfit) {
					long per = Config.profitPerKill();
					int view = Config.profitShowTotals ? Config.profitView : 0;
					out.add(new Line("Arachne profit - " + Config.PROFIT_VIEWS[view] + " ("
							// fix this later
							+ (Config.profitClassic ? "Classic" : "Ironman")
							+ (Config.profitPlacer ? ", placer" : "") + ")", ORANGE));
					double kph = preview ? 40 : ArachneTracker.killsPerHour();
					double essPrice = EssenceCalc.price();
					if (Config.profitClassic) {
						out.add(new Line(essPrice > 0
								? String.format(java.util.Locale.ROOT, "Essence: 37.5/boss = %,d coins at Bazaar", Math.round(EssenceCalc.PER_KILL * essPrice))
								: "Essence: 37.5/boss (Bazaar price loading)", GREY));
					}
					if (view == 0) {
						if (kph < 0) {
							out.add(new Line("Forecast: need 2 kills", GREY));
						} else {
							out.add(new Line(String.format("Pace: %.1f kills/hr", kph), WHITE));
							out.add(new Line(coins(Math.round(per * kph)) + " /hr", GREEN, 1.3f));
						}
					} else if (view == 1) {
						int kills = preview ? 5000 : Config.bestiaryKills;
						if (kills < 0) {
							out.add(new Line("Bestiary kills unknown", GREY));
							out.add(new Line("Type /apbestiary <kills> or add an API key", GREY, 0.8f));
						} else {
							out.add(new Line("Bestiary kills: " + String.format("%,d", kills), WHITE));
							out.add(new Line(coins(per * kills), GREEN, 1.3f));
						}
					} else {
						int kills = preview ? 12 : ArachneTracker.sessionKills;
						long ms = preview ? 1_080_000L : (kills == 0 ? 0 : System.currentTimeMillis() - ArachneTracker.sessionFirstKillMs);
						long s = ms / 1000;
						out.add(new Line(String.format("Session: %d:%02d:%02d  (%d kills)", s / 3600, (s / 60) % 60, s % 60, kills), WHITE));
						out.add(new Line(coins(per * kills), GREEN, 1.3f));
						if (kph >= 0) {
							out.add(new Line(coins(Math.round(per * kph)) + " /hr pace", GREY));
						}
					}
				}
			}
			default -> { }
		}
		return out;
	}

	private static String coins(long n) {
		return String.format("%,d", n) + " coins";
	}

	private static void addChatDebug(List<Line> out) {
		if (!Location.lastLocation.isEmpty()) {
			String loc = Location.lastLocation;
			out.add(new Line("loc: " + (loc.length() > 50 ? loc.substring(0, 50) : loc)
					+ (Location.inSanctuary ? " [sanctuary]" : ""), GREY));
		}
		if (!ArachneTracker.lastChat.isEmpty()) {
			String c = ArachneTracker.lastChat;
			out.add(new Line("chat: " + (c.length() > 60 ? c.substring(0, 60) : c), GREY));
		}
	}

	// why am I doing this, this is killing me
	private static int lineH(Line l) {
		return Math.round(LINE_H * l.size());
	}

	private static Style styleOf(int panel) {
		Style st = Style.EMPTY;
		if (Config.bold[panel]) {
			st = st.withBold(true);
		}
		if (Config.italic[panel]) {
			st = st.withItalic(true);
		}
		if (Config.strike[panel]) {
			st = st.withStrikethrough(true);
		}
		return st;
	}

	private static float rawWidth(net.minecraft.client.gui.Font font, String text, int panel) {
		Style st = styleOf(panel);
		int spacing = Config.letterSpacing[panel];
		if (spacing <= 0) {
			return font.width(Component.literal(text).withStyle(st));
		}
		float w = 0;
		for (int i = 0; i < text.length(); i++) {
			w += font.width(Component.literal(String.valueOf(text.charAt(i))).withStyle(st));
			if (i < text.length() - 1) {
				w += spacing;
			}
		}
		return w;
	}

	private static void drawStyled(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, String text, int color, int panel) {
		Style st = styleOf(panel);
		int spacing = Config.letterSpacing[panel];
		if (spacing <= 0) {
			g.text(font, Component.literal(text).withStyle(st), 0, 0, color, true);
			return;
		}
		float x = 0;
		for (int i = 0; i < text.length(); i++) {
			Component ch = Component.literal(String.valueOf(text.charAt(i))).withStyle(st);
			g.text(font, ch, Math.round(x), 0, color, true);
			x += font.width(ch) + spacing;
		}
	}

	private static int textWidth(net.minecraft.client.gui.Font font, List<Line> lines, int panel) {
		int w = 0;
		for (Line l : lines) {
			w = Math.max(w, Math.round(rawWidth(font, l.text(), panel) * l.size()));
		}
		return w;
	}

	private static int textHeight(List<Line> lines) {
		int h = 0;
		for (Line l : lines) {
			h += lineH(l);
		}
		return h;
	}

	static int[] bounds(net.minecraft.client.gui.Font font, List<Line> lines, int panel) {
		if (panel == Config.PLACE) {
			return placeBounds();
		}
		float s = Config.scale[panel];
		int x = Config.posX[panel];
		int y = Config.posY[panel];
		int w = textWidth(font, lines, panel);
		int h = textHeight(lines);
		float left = Config.centered[panel] ? x - (w / 2f + PAD) * s : x - PAD * s;
		return new int[] {
				Math.round(left), Math.round(y - PAD * s),
				Math.round(left + (w + 2 * PAD) * s), Math.round(y + (h + PAD - 2) * s)};
	}

	static void drawBox(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int panel, List<Line> lines) {
		if (panel == Config.PLACE) {
			PlaceStyles.Style st = Config.placeStyle();
			float s = placeScale(st);
			int[] pb = placeBounds();
			drawPlaceAt(g, st, pb[0], pb[1], s);
			return;
		}
		float s = Config.scale[panel];
		int w = textWidth(font, lines, panel);
		int h = textHeight(lines);
		boolean mid = Config.centered[panel];
		g.pose().pushMatrix();
		g.pose().translate((float) Config.posX[panel] - (mid ? w * s / 2f : 0f), (float) Config.posY[panel]);
		g.pose().scale(s, s);
		if (Config.background[panel]) {
			g.fill(-PAD, -PAD, w + PAD, h + PAD - 2, 0x90000000);
		}
		int y = 0;
		for (Line l : lines) {
			float lw = rawWidth(font, l.text(), panel) * l.size();
			g.pose().pushMatrix();
			g.pose().translate(mid ? (w - lw) / 2f : 0f, (float) y);
			g.pose().scale(l.size(), l.size());
			int col = Config.textColor[panel] != 0 ? Config.textColor[panel] : l.color();
			drawStyled(g, font, l.text(), col, panel);
			g.pose().popMatrix();
			y += lineH(l);
		}
		g.pose().popMatrix();
	}

	static String fmt(long ms) {
		long m = ms / 60000;
		long s = (ms / 1000) % 60;
		long tenths = (ms / 100) % 10;
		return m + ":" + String.format("%02d", s) + "." + tenths;
	}

	static int ratioColor(float ratio) {
		float r = Math.min(1f, Math.max(0f, ratio));
		int red = Math.min(255, Math.round(510f * (1f - r)));
		int green = Math.min(255, Math.round(510f * r));
		return 0xFF000000 | (red << 16) | (green << 8) | 0x20;
	}

	private static double[] project(Minecraft mc, double tx, double ty, double tz) {
		var cam = mc.gameRenderer.mainCamera();
		var pos = cam.position();
		var f = cam.forwardVector();
		var u = cam.upVector();
		var l = cam.leftVector();
		double dx = tx - pos.x;
		double dy = ty - pos.y;
		double dz = tz - pos.z;

		double depth = dx * f.x() + dy * f.y() + dz * f.z();
		double right = -(dx * l.x() + dy * l.y() + dz * l.z());
		double up = dx * u.x() + dy * u.y() + dz * u.z();

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		double tanHalf = Math.tan(Math.toRadians(cam.getFov()) / 2.0);
		double aspect = w / (double) h;

		double sx;
		double sy;
		boolean on = depth > 0.05;
		if (on) {
			sx = w / 2.0 + (right / depth) / (tanHalf * aspect) * (w / 2.0);
			sy = h / 2.0 - (up / depth) / tanHalf * (h / 2.0);
			on = sx >= 0 && sx <= w && sy >= 0 && sy <= h;
		} else {
			sx = 0;
			sy = 0;
		}
		if (!on) {

			double vx = depth > 0.05 ? right : -right;
			double vy = depth > 0.05 ? -up : up;
			if (depth <= 0.05 && Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) {
				vy = 1;
			}
			double len = Math.max(Math.abs(vx) / (w / 2.0 - 40), Math.abs(vy) / (h / 2.0 - 40));
			if (len < 1e-6) {
				len = 1;
			}
			sx = w / 2.0 + vx / len;
			sy = h / 2.0 + vy / len;
		}
		return new double[] {sx, sy, depth, on ? 1 : 0};
	}

	static final double HOLO_X = -282.5, HOLO_Y = 50.5, HOLO_Z = -178.5;

	private static void drawAltarTag(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Config.spawnHolo() || ArachneTracker.spawnEndNs == 0) {
			return;
		}
		double[] centre = project(mc, HOLO_X, HOLO_Y, HOLO_Z);
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		if (centre[3] <= 0.5 || centre[0] < 0 || centre[0] > sw || centre[1] < 0 || centre[1] > sh) {
			return;
		}
		long totalMs = ArachneTracker.spawnTotalMs();
		long rem = ArachneTracker.spawnRemainingMs();
		int color = rem > 0 ? verdictColor() : RED;

		float s = Config.holoSize / 100f;
		if (Config.holoScaleWithDistance) {
			double depth = Math.max(0.5, centre[2]);
			s *= (float) Math.max(0.4, Math.min(3.0, 8.0 / depth));
		}

		var font = mc.font;
		String top = rem > 0 ? String.format("%.1fs", rem / 1000.0) : "SPAWNING!";
		String sub = ArachneTracker.spawnVerdict == 0 ? "checking altar..." : verdictText();
		boolean mini = Config.holoMinimal;
		float big = mini ? 1.5f : 2.0f;
		int topW = Math.round(font.width(top) * big);
		int boxW = (mini ? topW : Math.max(topW, font.width(sub))) + 10;
		int boxH = mini ? Math.round(LINE_H * big) + 6 : Math.round(LINE_H * big) + LINE_H + 8;

		g.pose().pushMatrix();
		g.pose().translate((float) centre[0], (float) centre[1]);
		g.pose().scale(s, s);
		int bx = -boxW / 2;
		int by = -boxH / 2;
		if (Config.holoBackground) {
			g.fill(bx, by, bx + boxW, by + boxH, 0xB0000000);
		}
		g.pose().pushMatrix();
		g.pose().translate((float) (bx + (boxW - topW) / 2), (float) (by + 3));
		g.pose().scale(big, big);
		g.text(font, top, 0, 0, color, true);
		g.pose().popMatrix();
		if (!mini) {
			g.text(font, sub, bx + (boxW - font.width(sub)) / 2, by + 3 + Math.round(LINE_H * big) + 1, color, true);
			float ratio = rem > 0 ? Math.min(1f, rem / (float) totalMs) : 0f;
			g.fill(bx, by + boxH - 3, bx + Math.round(boxW * ratio), by + boxH - 1, color);
		}
		g.pose().popMatrix();
	}

	private static final String[][] PLACE_GLYPHS = {
			{"11110", "10001", "10001", "11110", "10000", "10000", "10000"},
			{"10000", "10000", "10000", "10000", "10000", "10000", "11111"},
			{"01110", "10001", "10001", "11111", "10001", "10001", "10001"},
			{"01110", "10001", "10000", "10000", "10000", "10001", "01110"},
			{"11111", "10000", "10000", "11110", "10000", "10000", "11111"},
	};

	private static final String[][] HOP_GLYPHS = {
			{"10001", "10001", "10001", "11111", "10001", "10001", "10001"},
			{"01110", "10001", "10001", "10001", "10001", "10001", "01110"},
			{"11110", "10001", "10001", "11110", "10000", "10000", "10000"},
	};

	private static String[][] glyphs = PLACE_GLYPHS;

	private static final String[] BANG = {"00100", "00100", "00100", "00100", "00100", "00000", "00100"};
	private static boolean placeWasOn = false;
	private static PlaceStyles.Style placeActive = null;

	private static int cellOf(PlaceStyles.Style st) {
		return st.font() == 1 ? 3 : 6;
	}

	private static String placeText(PlaceStyles.Style st) {
		return (glyphs == HOP_GLYPHS ? "HOP" : "PLACE") + "!".repeat(st.bangs());
	}

	private static int textScale() {
		return 4;
	}

	private static Component placeComp(PlaceStyles.Style st) {
		return Component.literal(placeText(st)).withStyle(net.minecraft.ChatFormatting.BOLD);
	}

	private static int placeWidth(PlaceStyles.Style st) {
		if (st.font() == 2) {
			return Minecraft.getInstance().font.width(placeComp(st)) * textScale();
		}
		int n = glyphs.length + st.bangs();
		int cell = cellOf(st);
		return n * 5 * cell + (n - 1) * cell;
	}

	private static int placeHeight(PlaceStyles.Style st) {
		if (st.font() == 2) {
			return 9 * textScale();
		}
		return glyphs[0].length * cellOf(st) + 2;
	}

	static void resolvePlace() {
		if (Config.posX[Config.PLACE] < 0 || Config.posY[Config.PLACE] < 0) {
			var win = Minecraft.getInstance().getWindow();
			if (Config.posX[Config.PLACE] < 0) {
				Config.posX[Config.PLACE] = win.getGuiScaledWidth() / 2;
			}
			if (Config.posY[Config.PLACE] < 0) {
				Config.posY[Config.PLACE] = win.getGuiScaledHeight() / 4;
			}
		}
	}

	private static float placeScale(PlaceStyles.Style st) {
		return Config.scale[Config.PLACE] * st.size() / 100f;
	}

	private static int[] placeBounds() {
		resolvePlace();
		PlaceStyles.Style st = Config.placeStyle();
		float s = placeScale(st);
		float w = placeWidth(st) * s;
		float h = placeHeight(st) * s;
		float left = Config.centered[Config.PLACE] ? Config.posX[Config.PLACE] - w / 2f : Config.posX[Config.PLACE];
		int top = Config.posY[Config.PLACE];
		return new int[] {Math.round(left), top, Math.round(left + w), Math.round(top + h)};
	}

	private static void placeTick(boolean showing) {
		if (showing && !placeWasOn) {
			placeActive = Config.placeRandom ? PlaceStyles.random() : null;
		}
		placeWasOn = showing;
	}

	private static void drawPlace(GuiGraphicsExtractor g, Minecraft mc) {
		resolvePlace();
		PlaceStyles.Style st = placeActive != null ? placeActive : Config.placeStyle();
		boolean hop = ArachneTracker.hopOn();
		if (hop) {
			st = new PlaceStyles.Style(0xFFFFFFFF, 0xFFFFFFFF, 0, 0, 1, 100);
		}
		glyphs = hop ? HOP_GLYPHS : PLACE_GLYPHS;
		try {
			float s = placeScale(st);
			float w = placeWidth(st) * s;
			float left = Config.centered[Config.PLACE] ? Config.posX[Config.PLACE] - w / 2f : Config.posX[Config.PLACE];
			drawPlaceAt(g, st, left, Config.posY[Config.PLACE], s);
		} finally {
			glyphs = PLACE_GLYPHS;
		}
	}

	static void drawPlacePreview(GuiGraphicsExtractor g, PlaceStyles.Style st, float left, float top, float s) {
		drawPlaceAt(g, st, left, top, s);
	}

	static float previewWidth(PlaceStyles.Style st, float s) {
		return placeWidth(st) * s;
	}

	private static void drawPlaceAt(GuiGraphicsExtractor g, PlaceStyles.Style st, float left, float top, float s) {
		boolean flash = (System.currentTimeMillis() / 250) % 2 == 0;
		int color = flash ? st.a() : st.b();
		g.pose().pushMatrix();
		g.pose().translate(left, top);
		g.pose().scale(s, s);
		if (st.font() == 2) {
			g.pose().scale(textScale(), textScale());
			Component comp = placeComp(st);
			if (st.shadow() != 0) {
				g.text(Minecraft.getInstance().font, comp, 1, 1, st.shadow(), false);
			}
			g.text(Minecraft.getInstance().font, comp, 0, 0, color, false);
			g.pose().popMatrix();
			return;
		}
		int cell = cellOf(st);
		int letterW = 5 * cell;
		int gap = cell;
		int n = glyphs.length + st.bangs();
		int shadowOff = cell >= 6 ? 2 : 1;
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0 && st.shadow() == 0) {
				continue;
			}
			int off = pass == 0 ? shadowOff : 0;
			int c = pass == 0 ? st.shadow() : color;
			for (int i = 0; i < n; i++) {
				int lx = i * (letterW + gap) + off;
				String[] rows = i < glyphs.length ? glyphs[i] : BANG;
				for (int r = 0; r < rows.length; r++) {
					for (int col = 0; col < 5; col++) {
						if (rows[r].charAt(col) == '1') {
							int px = lx + col * cell;
							int py = r * cell + off;
							g.fill(px, py, px + cell, py + cell, c);
						}
					}
				}
			}
		}
		g.pose().popMatrix();
	}

	private static void drawBossLine(GuiGraphicsExtractor g, Minecraft mc) {
		double tx;
		double ty;
		double tz;
		Entity body = ArachneTracker.bossBody;
		if (body != null && !body.isRemoved()) {
			tx = body.getX();
			ty = body.getY() + body.getBbHeight() * 0.5;
			tz = body.getZ();
		} else {
			tx = ArachneTracker.bossTagX;
			ty = ArachneTracker.bossTagY;
			tz = ArachneTracker.bossTagZ;
		}
		double[] end = project(mc, tx, ty, tz);
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		double ex = end[0];
		double ey = end[1];

		double sx = w / 2.0;
		double sy = h / 2.0;
		double len = Math.hypot(ex - sx, ey - sy);
		int t = Config.lineThickness;
		int lo = t / 2;
		int hi = t - lo;
		int style = Config.lineStyle;
		int period = style == 1 ? 14 + t * 2 : Math.max(4, t * 3);
		int onLen = style == 1 ? 8 + t : t;
		int steps = Math.max(1, (int) Math.ceil(len));
		for (int i = 0; i <= steps; i++) {
			double f = i / (double) steps;
			if (style != 0 && ((int) (f * len)) % period >= onLen) {
				continue;
			}
			int px = (int) Math.round(sx + (ex - sx) * f);
			int py = (int) Math.round(sy + (ey - sy) * f);
			g.fill(px - lo, py - lo, px + hi, py + hi, Config.lineColor);
		}

		int mx = (int) Math.round(ex);
		int my = (int) Math.round(ey);
		int labelColor = Config.lineColor;
		if (Config.dotColor != 0) {
			int half = 3 + t;
			g.fill(mx - half, my - half, mx + half, my + half, Config.dotColor);
			labelColor = Config.dotColor;
		}

		var player = mc.player;
		double dx = tx - player.getX();
		double dy = ty - player.getEyeY();
		double dz = tz - player.getZ();
		long dist = Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
		String label = "Arachne " + dist + "m";
		var font = mc.font;
		g.text(font, label, mx - font.width(label) / 2, my + 7 + t, labelColor, true);
	}
}
