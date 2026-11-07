package com.arachneplus.mixin;

import com.arachneplus.Config;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingNameMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
	private void arachneplus$hideNames(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.active()) {
			return;
		}
		boolean player = entity instanceof Player;
		if ((player && Config.hidePlayerNames) || (!player && Config.hideMobNames)) {
			cir.setReturnValue(false);
		}
	}
}
