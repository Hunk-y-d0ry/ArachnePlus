package com.arachneplus.mixin;

import com.arachneplus.CpsCounter;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void arachneplus$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		CpsCounter.onButton(info.button(), action);
	}
}
