package com.example.depression;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Хранит «шкалу грусти» каждого игрока в мире, чтобы она переживала перезапуск. */
public class DepressionState extends PersistentState {
	private final Map<UUID, Integer> values = new HashMap<>();

	public int get(UUID id) {
		return values.getOrDefault(id, 0);
	}

	public void set(UUID id, int value) {
		if (get(id) != value) {
			values.put(id, value);
			markDirty();
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound players = new NbtCompound();
		values.forEach((id, v) -> players.putInt(id.toString(), v));
		nbt.put("players", players);
		return nbt;
	}

	public static DepressionState fromNbt(NbtCompound nbt) {
		DepressionState state = new DepressionState();
		NbtCompound players = nbt.getCompound("players");
		for (String key : players.getKeys()) {
			try {
				state.values.put(UUID.fromString(key), players.getInt(key));
			} catch (IllegalArgumentException ignored) {
				// битая запись — пропускаем
			}
		}
		return state;
	}
}
