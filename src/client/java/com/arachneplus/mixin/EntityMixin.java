package com.arachneplus.mixin;

import com.arachneplus.ArachneTracker;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "getTeamColor()I", at = @At("RETURN"), cancellable = true)
	private void arachneplus$runicColour(CallbackInfoReturnable<Integer> cir) {
		if (ArachneTracker.isRunic((Entity) (Object) this)) {
			cir.setReturnValue(0xB000FF);
		}
	}
}
