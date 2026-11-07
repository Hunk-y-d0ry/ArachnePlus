package com.arachneplus;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class PageScreen extends ModScreen {
	protected static final int W = 150;
	protected static final int H = 20;
	protected static final int GAP = 4;

	protected final Screen parent;
	private int slot;
	private int nextY;

	private static final int HEADER = 34;
	private int originY;
	private final java.util.List<net.minecraft.client.gui.components.AbstractWidget> placed = new java.util.ArrayList<>();

	protected PageScreen(String title, Screen parent) {
		super(Component.literal(title));
		this.parent = parent;
	}

	protected abstract void build();

	@Override
	protected void init() {
		slot = 0;
		nextY = 0;
		placed.clear();
		build();
		if (slot % 2 == 1) {
			nextY += H + GAP;
		}
		Button back = Button.builder(Component.literal("Back"), b -> this.onClose())
				.bounds(this.width / 2 - 40, nextY + 6, 80, H).build();
		this.addRenderableWidget(back);
		placed.add(back);
		int total = HEADER + nextY + 6 + H;
		originY = Math.max(4, (this.height - total) / 2);
		for (var w : placed) {
			w.setY(originY + HEADER + w.getY());
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}

	private int left() {
		return this.width / 2 - W - GAP / 2;
	}

	protected void half(Button b) {
		b.setPosition(left() + (slot % 2) * (W + GAP), nextY);
		b.setWidth(W);
		this.addRenderableWidget(b);
		placed.add(b);
		slot++;
		if (slot % 2 == 0) {
			nextY += H + GAP;
		}
	}

	protected void full(Button b) {
		if (slot % 2 == 1) {
			slot++;
			nextY += H + GAP;
		}
		b.setPosition(left(), nextY);
		b.setWidth(W * 2 + GAP);
		this.addRenderableWidget(b);
		placed.add(b);
		slot += 2;
		nextY += H + GAP;
	}

	protected void fullWidget(net.minecraft.client.gui.components.AbstractWidget w) {
		if (slot % 2 == 1) {
			slot++;
			nextY += H + GAP;
		}
		w.setPosition(left(), nextY);
		w.setWidth(W * 2 + GAP);
		this.addRenderableWidget(w);
		placed.add(w);
		slot += 2;
		nextY += H + GAP;
	}

	protected static Component onOff(String label, boolean on) {
		return Component.literal(label + ": ")
				.append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
	}

	protected Button toggleButton(String label, BooleanSupplier get, Consumer<Boolean> set) {
		return Button.builder(onOff(label, get.getAsBoolean()), b -> {
			boolean now = !get.getAsBoolean();
			set.accept(now);
			Config.save();
			b.setMessage(onOff(label, now));
		}).bounds(0, 0, W, H).build();
	}

	protected void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
		half(toggleButton(label, get, set));
	}

	protected Button cycleButton(Function<Object, Component> text, Runnable action) {
		return Button.builder(text.apply(null), b -> {
			action.run();
			Config.save();
			b.setMessage(text.apply(null));
		}).bounds(0, 0, W, H).build();
	}

	protected void opens(String label, java.util.function.Supplier<Screen> screen) {
		half(Button.builder(Component.literal(label), b -> this.minecraft.setScreenAndShow(screen.get()))
				.bounds(0, 0, W, H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		String t = this.title.getString();
		Logo.draw(g, this.font, this.width / 2f, originY, Logo.SMALL);
		g.text(this.font, t, this.width / 2 - this.font.width(t) / 2, originY + 17, Theme.gold(), true);
	}
}
