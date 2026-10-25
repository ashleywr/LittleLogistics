package dev.murad.shipping.api;

import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * General integration hook for automation attached to a Little Logistics docking station.
 * Implementations are called only on the logical server.
 */
public interface DockingStationService {
    default void onVehicleDocked(ServerLevel level, DockingStationBlockEntity station, Entity vehicle) {}

    default void tick(ServerLevel level, DockingStationBlockEntity station) {}

    default void onVehicleDeparted(ServerLevel level, DockingStationBlockEntity station, Entity vehicle) {}
}
