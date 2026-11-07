package com.arachneplus.mixin;

import com.arachneplus.ArachneTracker;
import com.arachneplus.Config;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelParticleMixin {

	public static volatile boolean live = false;
	private static boolean warned = false;

	@Inject(method = "doAddParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
	private void arachneplus$seeParticle(ParticleOptions options, boolean overrideLimiter, boolean alwaysShow,
			double x, double y, double z, double xa, double ya, double za, CallbackInfo ci) {
		try {
			live = true;
			var key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
			if (key == null) {
				return;
			}
			String path = key.getPath();
			ArachneTracker.particleSeen(path, x, y, z, xa, ya, za);
			if (Config.active() && Config.hideParticles) {
				ci.cancel();
			}
		} catch (Throwable t) {

			if (!warned) {
				warned = true;
				com.arachneplus.ArachnePlusClient.LOGGER.warn("Particle hook problem: {}", t.toString());
			}
		}
	}
}
