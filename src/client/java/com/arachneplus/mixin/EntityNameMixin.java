package com.arachneplus.mixin;

import com.arachneplus.Config;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityNameMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
	private void arachneplus$hideOwnName(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.active()) {
			return;
		}
		boolean player = entity instanceof Player;
		if ((player && Config.hidePlayerNames) || (!player && Config.hideMobNames)) {
			cir.setReturnValue(false);
		}
	}
}
