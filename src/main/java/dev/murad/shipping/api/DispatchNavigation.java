package dev.murad.shipping.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Persistent exclusive-control lease used by demand-driven vehicle dispatchers. */
public final class DispatchNavigation {
    private static final String CONTROLLER = "controller";
    private static final String NETWORK = "network";
    private static final String TARGET = "target";
    private static final String HOLD = "hold";

    @Nullable private ResourceLocation controller;
    @Nullable private UUID networkId;
    @Nullable private UUID targetStationId;
    private boolean holdAtTarget;

    public boolean claim(ResourceLocation requestedController, UUID requestedNetwork) {
        if (controller != null && !controller.equals(requestedController)) return false;
        controller = requestedController;
        networkId = requestedNetwork;
        return true;
    }

    public boolean setTarget(ResourceLocation requestingController, UUID stationId, boolean hold) {
        if (!isClaimedBy(requestingController)) return false;
        targetStationId = stationId;
        holdAtTarget = hold;
        return true;
    }

    public boolean clearTarget(ResourceLocation requestingController) {
        if (!isClaimedBy(requestingController)) return false;
        targetStationId = null;
        holdAtTarget = false;
        return true;
    }

    public boolean release(ResourceLocation requestingController) {
        if (!isClaimedBy(requestingController)) return false;
        controller = null;
        networkId = null;
        targetStationId = null;
        holdAtTarget = false;
        return true;
    }

    public boolean isClaimed() {
        return controller != null;
    }

    public boolean isClaimedBy(ResourceLocation value) {
        return value.equals(controller);
    }

    public Optional<ResourceLocation> controller() {
        return Optional.ofNullable(controller);
    }

    public Optional<UUID> networkId() {
        return Optional.ofNullable(networkId);
    }

    public Optional<UUID> targetStationId() {
        return Optional.ofNullable(targetStationId);
    }

    public DockingDirective directiveFor(@Nullable UUID stationId) {
        if (controller == null || targetStationId == null || stationId == null) return DockingDirective.DEFAULT;
        if (!targetStationId.equals(stationId)) return DockingDirective.PASS;
        return holdAtTarget ? DockingDirective.HOLD : DockingDirective.STOP;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (controller != null) tag.putString(CONTROLLER, controller.toString());
        if (networkId != null) tag.putUUID(NETWORK, networkId);
        if (targetStationId != null) tag.putUUID(TARGET, targetStationId);
        tag.putBoolean(HOLD, holdAtTarget);
        return tag;
    }

    public void load(CompoundTag tag) {
        controller = tag.contains(CONTROLLER) ? ResourceLocation.tryParse(tag.getString(CONTROLLER)) : null;
        networkId = tag.hasUUID(NETWORK) ? tag.getUUID(NETWORK) : null;
        targetStationId = tag.hasUUID(TARGET) ? tag.getUUID(TARGET) : null;
        holdAtTarget = tag.getBoolean(HOLD);
        if (controller == null) {
            networkId = null;
            targetStationId = null;
            holdAtTarget = false;
        }
    }
}
