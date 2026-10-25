package dev.murad.shipping.api;

import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry for optional dock integrations. Registration is normally performed during mod setup. */
public final class DockingStationServices {
    private static final List<DockingStationService> SERVICES = new CopyOnWriteArrayList<>();

    private DockingStationServices() {}

    public static void register(DockingStationService service) {
        if (!SERVICES.contains(service)) SERVICES.add(service);
    }

    public static void unregister(DockingStationService service) {
        SERVICES.remove(service);
    }

    public static void vehicleDocked(ServerLevel level, DockingStationBlockEntity station, Entity vehicle) {
        SERVICES.forEach(service -> service.onVehicleDocked(level, station, vehicle));
    }

    public static void tick(ServerLevel level, DockingStationBlockEntity station) {
        SERVICES.forEach(service -> service.tick(level, station));
    }

    public static void vehicleDeparted(ServerLevel level, DockingStationBlockEntity station, Entity vehicle) {
        SERVICES.forEach(service -> service.onVehicleDeparted(level, station, vehicle));
    }
}
