package dev.murad.shipping.transport;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-wide persistent transport graphs. */
public final class TransportNetworkData extends SavedData {
    public static final String NAME = "littlelogistics_transport_networks";
    private final Map<UUID, TransportNetwork> networks = new LinkedHashMap<>();

    public static TransportNetworkData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TransportNetworkData::new, TransportNetworkData::load, null), NAME);
    }

    public TransportNetwork create(String name, TransportMode mode) {
        TransportNetwork network = new TransportNetwork(UUID.randomUUID(), name, mode);
        networks.put(network.id(), network);
        setDirty();
        return network;
    }

    public Optional<TransportNetwork> get(UUID id) { return Optional.ofNullable(networks.get(id)); }
    public List<TransportNetwork> all() { return List.copyOf(networks.values()); }

    public boolean remove(UUID id) {
        if (networks.remove(id) == null) return false;
        setDirty();
        return true;
    }

    /** Call after using a network's mutation methods. */
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        networks.values().forEach(network -> list.add(network.save()));
        tag.put("networks", list);
        tag.putInt("version", 1);
        return tag;
    }

    public static TransportNetworkData load(CompoundTag tag, HolderLookup.Provider registries) {
        TransportNetworkData data = new TransportNetworkData();
        ListTag list = tag.getList("networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TransportNetwork network = TransportNetwork.load(list.getCompound(i));
            data.networks.put(network.id(), network);
        }
        return data;
    }
}
