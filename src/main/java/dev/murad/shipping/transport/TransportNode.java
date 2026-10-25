package dev.murad.shipping.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Stable graph node. A station node retains its station UUID even when renamed. */
public record TransportNode(UUID id, String name, String dimension, BlockPos position,
                            @Nullable UUID stationId) {
    public Optional<UUID> station() {
        return Optional.ofNullable(stationId);
    }

    public TransportNode renamed(String newName) {
        return new TransportNode(id, newName, dimension, position, stationId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putString("dimension", dimension);
        tag.putLong("position", position.asLong());
        if (stationId != null) tag.putUUID("station", stationId);
        return tag;
    }

    public static TransportNode load(CompoundTag tag) {
        return new TransportNode(tag.getUUID("id"), tag.getString("name"), tag.getString("dimension"),
                BlockPos.of(tag.getLong("position")), tag.hasUUID("station") ? tag.getUUID("station") : null);
    }
}
