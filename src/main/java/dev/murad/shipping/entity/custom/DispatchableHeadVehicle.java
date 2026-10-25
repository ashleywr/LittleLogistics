package dev.murad.shipping.entity.custom;

import dev.murad.shipping.api.DispatchNavigation;
import dev.murad.shipping.transport.DispatchRoutePlan;

/** Opt-in extension for head vehicles that accept temporary dispatcher-owned itineraries. */
public interface DispatchableHeadVehicle extends HeadVehicle {
    DispatchNavigation getDispatchNavigation();

    boolean applyDispatchRoute(DispatchRoutePlan plan);

    void clearDispatchRoute();
}
