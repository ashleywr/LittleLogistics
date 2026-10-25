package dev.murad.shipping.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/** Data component stored on a Network Chart. Selection is editor-local; the graph is shared. */
public record NetworkChartReference(UUID networkId, TransportMode mode, Optional<UUID> selectedNode) {
    public static final Codec<NetworkChartReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("network").forGetter(NetworkChartReference::networkId),
            TransportMode.CODEC.fieldOf("mode").forGetter(NetworkChartReference::mode),
            UUIDUtil.CODEC.optionalFieldOf("selected").forGetter(NetworkChartReference::selectedNode)
    ).apply(instance, NetworkChartReference::new));

    public static final StreamCodec<ByteBuf, NetworkChartReference> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NetworkChartReference::networkId,
            TransportMode.STREAM_CODEC, NetworkChartReference::mode,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), NetworkChartReference::selectedNode,
            NetworkChartReference::new);

    public NetworkChartReference select(UUID node) {
        return new NetworkChartReference(networkId, mode, Optional.of(node));
    }

    public NetworkChartReference clearSelection() {
        return new NetworkChartReference(networkId, mode, Optional.empty());
    }
}
