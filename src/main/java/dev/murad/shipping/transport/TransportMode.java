package dev.murad.shipping.transport;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.StringRepresentable;

public enum TransportMode implements StringRepresentable {
    RAIL("rail"),
    WATER("water");

    private final String serializedName;

    public static final Codec<TransportMode> CODEC = StringRepresentable.fromEnum(TransportMode::values);
    public static final StreamCodec<ByteBuf, TransportMode> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(TransportMode::fromName, TransportMode::getSerializedName);

    TransportMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static TransportMode fromName(String name) {
        for (TransportMode mode : values()) {
            if (mode.serializedName.equals(name)) return mode;
        }
        return RAIL;
    }
}
