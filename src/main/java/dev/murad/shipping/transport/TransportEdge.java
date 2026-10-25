package dev.murad.shipping.transport;

import dev.murad.shipping.util.LocoRouteSegment;
import dev.murad.shipping.util.TugRouteSegment;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/** A directed, precompiled edge. Exactly one mode-specific segment is present. */
public final class TransportEdge {
    private final UUID from;
    private final UUID to;
    private final double cost;
    @Nullable private final LocoRouteSegment railSegment;
    @Nullable private final TugRouteSegment waterSegment;

    private TransportEdge(UUID from, UUID to, double cost, @Nullable LocoRouteSegment railSegment,
                          @Nullable TugRouteSegment waterSegment) {
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.railSegment = railSegment == null ? null : railSegment.copy();
        this.waterSegment = waterSegment == null ? null : waterSegment.copy();
    }

    public static TransportEdge rail(UUID from, UUID to, LocoRouteSegment segment) {
        return new TransportEdge(from, to, Math.max(1, segment.getSteps().size()), segment, null);
    }

    public static TransportEdge water(UUID from, UUID to, TugRouteSegment segment) {
        return new TransportEdge(from, to, Math.max(1, segment.getPoints().size()), null, segment);
    }

    public UUID from() { return from; }
    public UUID to() { return to; }
    public double cost() { return cost; }
    public @Nullable LocoRouteSegment railSegment() { return railSegment == null ? null : railSegment.copy(); }
    public @Nullable TugRouteSegment waterSegment() { return waterSegment == null ? null : waterSegment.copy(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("from", from);
        tag.putUUID("to", to);
        tag.putDouble("cost", cost);
        if (railSegment != null) tag.put("rail", railSegment.toNBT());
        if (waterSegment != null) tag.put("water", waterSegment.toNBT());
        return tag;
    }

    public static TransportEdge load(CompoundTag tag) {
        LocoRouteSegment rail = tag.contains("rail") ? LocoRouteSegment.fromNBT(tag.getCompound("rail")) : null;
        TugRouteSegment water = tag.contains("water") ? TugRouteSegment.fromNBT(tag.getCompound("water")) : null;
        double fallbackCost = rail != null ? rail.getSteps().size() : water != null ? water.getPoints().size() : 1;
        return new TransportEdge(tag.getUUID("from"), tag.getUUID("to"),
                tag.contains("cost") ? tag.getDouble("cost") : Math.max(1, fallbackCost), rail, water);
    }
}
