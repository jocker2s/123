package com.example.depression;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class DepressionManager {
	public static final int MAX = 2400;
	public static final int LEVEL_1 = 600;
	public static final int LEVEL_2 = 1200;
	public static final int LEVEL_3 = 1800;

	private static final int SOCIAL_RADIUS = 16;
	private static final int PET_RADIUS = 8;
	private static final int SLEEPLESS_TICKS = 72000; // 3 игровых суток без сна

	private DepressionManager() {}

	public static DepressionState state(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager()
			.getOrCreate(DepressionState::fromNbt, DepressionState::new, "depression");
	}

	public static int level(int value) {
		if (value >= LEVEL_3) return 3;
		if (value >= LEVEL_2) return 2;
		if (value >= LEVEL_1) return 1;
		return 0;
	}

	/** Вызывается каждый тик сервера, работает раз в секунду. */
	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) return;

		DepressionState state = state(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isCreative() || player.isSpectator() || player.isSleeping()) continue;

			UUID id = player.getUuid();
			int old = state.get(id);
			int now = clamp(old + delta(player));
			state.set(id, now);
			apply(player, old, now);
		}
	}

	/** Что происходит с настроением за одну секунду. Плюс — хуже, минус — лучше. */
	private static int delta(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = player.getBlockPos();
		int change = 0;

		// Что давит.
		if (world.getLightLevel(pos) <= 4) change += 1;
		if (player.getStatHandler().getStat(
			Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST)) > SLEEPLESS_TICKS) change += 1;
		if (player.getHealth() <= 6.0f) change += 1;

		// Что поддерживает.
		boolean players = !world.getPlayers(p -> p != player && !p.isSpectator()
			&& p.squaredDistanceTo(player) < SOCIAL_RADIUS * SOCIAL_RADIUS).isEmpty();
		boolean pet = !world.getEntitiesByClass(TameableEntity.class,
			player.getBoundingBox().expand(PET_RADIUS), t -> t.isOwner(player)).isEmpty();
		boolean villager = !world.getEntitiesByClass(VillagerEntity.class,
			player.getBoundingBox().expand(PET_RADIUS), v -> true).isEmpty();
		boolean sunny = world.getDimension().hasSkyLight() && world.isDay()
			&& !world.isRaining() && world.isSkyVisible(pos);

		if (!players && !pet && !villager) change += 1; // одиночество
		if (players) change -= 2;
		if (pet) change -= 2;
		if (villager) change -= 1;
		if (sunny) change -= 2;

		return change;
	}

	private static void apply(ServerPlayerEntity player, int oldValue, int newValue) {
		int lvl = level(newValue);
		if (lvl > 0) {
			player.addStatusEffect(new StatusEffectInstance(
				ModEffects.DEPRESSION, 100, lvl - 1, false, false, true));
		} else if (player.hasStatusEffect(ModEffects.DEPRESSION)) {
			player.removeStatusEffect(ModEffects.DEPRESSION);
		}

		int oldLvl = level(oldValue);
		if (lvl > oldLvl) {
			player.sendMessage(Text.translatable("message.depression.worse." + lvl), true);
		} else if (lvl < oldLvl) {
			player.sendMessage(Text.translatable("message.depression.better." + lvl), true);
		}
	}

	public static void onWokeUp(ServerPlayerEntity player) {
		DepressionState state = state(player.getServer());
		int old = state.get(player.getUuid());
		int now = clamp(old - 900);
		state.set(player.getUuid(), now);
		apply(player, old, now);
	}

	public static void reset(ServerPlayerEntity player) {
		state(player.getServer()).set(player.getUuid(), 0);
		player.removeStatusEffect(ModEffects.DEPRESSION);
	}

	public static int clamp(int value) {
		return Math.max(0, Math.min(MAX, value));
	}
}
