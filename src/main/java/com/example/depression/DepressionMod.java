package com.example.depression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.ServerPlayerEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;

public class DepressionMod implements ModInitializer {
	public static final String MOD_ID = "depression";

	@Override
	public void onInitialize() {
		ModEffects.init();

		ServerTickEvents.END_SERVER_TICK.register(DepressionManager::tick);

		// Нормальный сон сильно помогает.
		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayerEntity player) {
				DepressionManager.onWokeUp(player);
			}
		});

		// После смерти начинаем с чистого листа.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			DepressionManager.reset(newPlayer));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			DepressionCommand.register(dispatcher));
	}
}
