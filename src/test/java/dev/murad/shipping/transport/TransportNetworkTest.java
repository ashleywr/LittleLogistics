package dev.murad.shipping.transport;

import dev.murad.shipping.api.DispatchNavigation;
import dev.murad.shipping.api.DockingDirective;
import dev.murad.shipping.util.LocoRouteSegment;
import dev.murad.shipping.util.LocoRouteStep;
import dev.murad.shipping.util.TugRoutePoint;
import dev.murad.shipping.util.TugRouteSegment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransportNetworkTest {
    @Test
    void twoStationOutAndBackPlanSupportsDynamicDispatch() {
        TransportNetwork network = new TransportNetwork(UUID.randomUUID(), "Canal", TransportMode.WATER);
        TransportNode current = station("Current", 0);
        TransportNode destination = station("Destination", 4);
        network.putNode(current);
        network.putNode(destination);
        network.putEdge(TransportEdge.water(current.id(), destination.id(),
                new TugRouteSegment(List.of(new TugRoutePoint(current.position()),
                        new TugRoutePoint(destination.position())))));
        network.putEdge(TransportEdge.water(destination.id(), current.id(),
                new TugRouteSegment(List.of(new TugRoutePoint(destination.position()),
                        new TugRoutePoint(current.position())))));

        DispatchRoutePlan plan = network.closedPlan(List.of(
                current.stationId(), destination.stationId(), current.stationId())).orElseThrow();
        assertEquals(2, plan.nodes().size());
        assertEquals(2, plan.edges().size());
        assertTrue(plan.toTugRoute().isComplete());
    }

    @Test
    void shortestPathAndClosedPlanAreDirectedAndDeterministic() {
        TransportNetwork network = new TransportNetwork(UUID.randomUUID(), "Harbor", TransportMode.RAIL);
        TransportNode depot = station("Depot", 0);
        TransportNode branch = waypoint("Branch", 1);
        TransportNode source = station("Source", 2);
        TransportNode destination = station("Destination", 3);
        List.of(depot, branch, source, destination).forEach(network::putNode);

        network.putEdge(edge(depot, branch, 1));
        network.putEdge(edge(branch, source, 1));
        network.putEdge(edge(source, destination, 2));
        network.putEdge(edge(destination, depot, 1));
        network.putEdge(edge(depot, source, 5));

        TransportNetwork.Path path = network.shortestPath(depot.id(), source.id()).orElseThrow();
        assertEquals(List.of(depot.id(), branch.id(), source.id()),
                path.nodes().stream().map(TransportNode::id).toList());
        assertEquals(2, path.cost());

        DispatchRoutePlan plan = network.closedPlan(List.of(
                depot.stationId(), source.stationId(), destination.stationId(), depot.stationId())).orElseThrow();
        assertEquals(plan.nodes().size(), plan.edges().size());
        assertTrue(plan.toLocoRoute().isUsable());
    }

    @Test
    void graphRoundTripPreservesRevisionAndPaths() {
        TransportNetwork network = new TransportNetwork(UUID.randomUUID(), "Canal", TransportMode.RAIL);
        TransportNode first = station("A", 0);
        TransportNode second = station("B", 1);
        network.putNode(first);
        network.putNode(second);
        network.putEdge(edge(first, second, 1));
        long revision = network.revision();

        TransportNetwork decoded = TransportNetwork.load(network.save());
        assertEquals(revision, decoded.revision());
        assertTrue(decoded.shortestPath(first.id(), second.id()).isPresent());
        assertFalse(decoded.shortestPath(second.id(), first.id()).isPresent());
    }

    @Test
    void closedRailPlanRejectsDirectionallyDiscontinuousEdges() {
        TransportNetwork network = new TransportNetwork(UUID.randomUUID(), "Broken", TransportMode.RAIL);
        TransportNode first = station("A", 0);
        TransportNode second = station("B", 1);
        network.putNode(first);
        network.putNode(second);
        network.putEdge(TransportEdge.rail(first.id(), second.id(), new LocoRouteSegment(List.of(
                new LocoRouteStep(first.position(), Direction.EAST, Direction.EAST)), Direction.EAST)));
        network.putEdge(TransportEdge.rail(second.id(), first.id(), new LocoRouteSegment(List.of(
                new LocoRouteStep(second.position(), Direction.WEST, Direction.WEST)), Direction.WEST)));

        assertTrue(network.closedPlan(List.of(first.stationId(), first.stationId(),
                second.stationId(), first.stationId())).isEmpty());
    }

    @Test
    void dispatchLeasePersistsAndPassesNonTargets() {
        DispatchNavigation navigation = new DispatchNavigation();
        ResourceLocation controller = ResourceLocation.fromNamespaceAndPath("test", "dispatcher");
        UUID network = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        assertTrue(navigation.claim(controller, network));
        assertTrue(navigation.setTarget(controller, target, true));
        assertEquals(DockingDirective.HOLD, navigation.directiveFor(target));
        assertEquals(DockingDirective.PASS, navigation.directiveFor(UUID.randomUUID()));

        CompoundTag saved = navigation.save();
        DispatchNavigation restored = new DispatchNavigation();
        restored.load(saved);
        assertEquals(DockingDirective.HOLD, restored.directiveFor(target));
        assertFalse(restored.release(ResourceLocation.fromNamespaceAndPath("test", "other")));
        assertTrue(restored.release(controller));
    }

    private static TransportNode station(String name, int x) {
        return new TransportNode(UUID.randomUUID(), name, "minecraft:overworld",
                new BlockPos(x, 64, 0), UUID.randomUUID());
    }

    private static TransportNode waypoint(String name, int x) {
        return new TransportNode(UUID.randomUUID(), name, "minecraft:overworld",
                new BlockPos(x, 64, 0), null);
    }

    private static TransportEdge edge(TransportNode from, TransportNode to, int steps) {
        return TransportEdge.rail(from.id(), to.id(), new LocoRouteSegment(
                java.util.stream.IntStream.range(0, steps)
                        .mapToObj(i -> new LocoRouteStep(new BlockPos(from.position().getX() + i, 64, 0),
                                Direction.EAST, Direction.EAST)).toList(), Direction.EAST));
    }
}
