package com.arachneplus.mixin;

import com.arachneplus.ArachneTracker;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	private static boolean hideNext = false;

	@Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"))
	private void arachneplus$particle(ParticleOptions options, double x, double y, double z,
			double xa, double ya, double za, CallbackInfoReturnable<net.minecraft.client.particle.Particle> cir) {
		try {
			var key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
			if (key != null && ClientLevelParticleMixin.live) {
				hideNext = false;
			} else if (key != null) {
				ArachneTracker.particleSeen(key.getPath(), x, y, z, xa, ya, za);
				hideNext = false;
			} else {
				hideNext = false;
			}
		} catch (Throwable t) {
			hideNext = false;
		}
	}

	@Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void arachneplus$hide(net.minecraft.client.particle.Particle particle,
			org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		boolean gyroHide = hideNext;
		hideNext = false;
		if (gyroHide || (com.arachneplus.Config.active() && com.arachneplus.Config.hideParticles)) {
			ci.cancel();
		}
	}
}
