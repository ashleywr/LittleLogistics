package dev.murad.shipping.transport;

import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.LocoRouteSegment;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteNode;
import dev.murad.shipping.util.TugRouteSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Immutable closed itinerary ready to install into one head vehicle. */
public record DispatchRoutePlan(UUID networkId, long networkRevision, String networkName,
                                TransportMode mode, List<TransportNode> nodes,
                                List<TransportEdge> edges) {
    public DispatchRoutePlan {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        if (nodes.size() < 2 || nodes.size() != edges.size()) {
            throw new IllegalArgumentException("A dispatch plan must be a closed route with one edge per node");
        }
        if (mode == TransportMode.RAIL) {
            for (int i = 0; i < edges.size(); i++) {
                LocoRouteSegment current = edges.get(i).railSegment();
                LocoRouteSegment next = edges.get((i + 1) % edges.size()).railSegment();
                if (current == null || next == null
                        || current.getArrivalDirection() != next.getStartIncomingDirection()) {
                    throw new IllegalArgumentException("Rail edges are not directionally continuous at "
                            + nodes.get((i + 1) % nodes.size()).name());
                }
            }
        }
    }

    public LocoRoute toLocoRoute() {
        if (mode != TransportMode.RAIL) throw new IllegalStateException("Not a rail route");
        List<LocoRouteNode> routeNodes = nodes.stream()
                .map(node -> new LocoRouteNode(node.name(), node.position().getX(), node.position().getY(), node.position().getZ()))
                .toList();
        List<LocoRouteSegment> segments = new ArrayList<>();
        for (TransportEdge edge : edges) {
            LocoRouteSegment segment = edge.railSegment();
            if (segment == null) throw new IllegalStateException("Rail plan contains a water edge");
            segments.add(segment);
        }
        return new LocoRoute(networkName, nodes.getFirst().dimension(), routeNodes, segments,
                LocoRoute.State.COMPLETE, -1);
    }

    public TugRoute toTugRoute() {
        if (mode != TransportMode.WATER) throw new IllegalStateException("Not a water route");
        List<TugRouteNode> routeNodes = nodes.stream()
                .map(node -> new TugRouteNode(node.name(), node.position().getX(), node.position().getY(), node.position().getZ()))
                .toList();
        List<TugRouteSegment> segments = new ArrayList<>();
        for (TransportEdge edge : edges) {
            TugRouteSegment segment = edge.waterSegment();
            if (segment == null) throw new IllegalStateException("Water plan contains a rail edge");
            segments.add(segment);
        }
        return new TugRoute(networkName, nodes.getFirst().dimension(), routeNodes, segments);
    }
}
