package dev.murad.shipping.transport;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

/** Persistent directed graph shared by every Network Chart that references its UUID. */
public final class TransportNetwork {
    public record Path(List<TransportNode> nodes, List<TransportEdge> edges, double cost) {
        public Path {
            nodes = List.copyOf(nodes);
            edges = List.copyOf(edges);
        }
    }

    private record QueueEntry(UUID node, double cost) {}

    private final UUID id;
    private String name;
    private final TransportMode mode;
    private long revision;
    private final Map<UUID, TransportNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, TransportEdge>> outgoing = new LinkedHashMap<>();

    public TransportNetwork(UUID id, String name, TransportMode mode) {
        this.id = id;
        this.name = name;
        this.mode = mode;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public TransportMode mode() { return mode; }
    public long revision() { return revision; }
    public List<TransportNode> nodes() { return List.copyOf(nodes.values()); }
    public List<TransportEdge> edges() { return outgoing.values().stream().flatMap(map -> map.values().stream()).toList(); }

    public void rename(String value) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && !trimmed.equals(name)) {
            name = trimmed;
            revision++;
        }
    }

    public void putNode(TransportNode node) {
        nodes.put(node.id(), node);
        outgoing.computeIfAbsent(node.id(), ignored -> new LinkedHashMap<>());
        revision++;
    }

    public boolean removeNode(UUID nodeId) {
        if (nodes.remove(nodeId) == null) return false;
        outgoing.remove(nodeId);
        outgoing.values().forEach(edges -> edges.remove(nodeId));
        revision++;
        return true;
    }

    public void putEdge(TransportEdge edge) {
        if (!nodes.containsKey(edge.from()) || !nodes.containsKey(edge.to())) {
            throw new IllegalArgumentException("Both edge endpoints must exist in the network");
        }
        if (mode == TransportMode.RAIL && edge.railSegment() == null
                || mode == TransportMode.WATER && edge.waterSegment() == null) {
            throw new IllegalArgumentException("Edge mode does not match network mode");
        }
        outgoing.computeIfAbsent(edge.from(), ignored -> new LinkedHashMap<>()).put(edge.to(), edge);
        revision++;
    }

    public boolean removeEdge(UUID from, UUID to) {
        Map<UUID, TransportEdge> edges = outgoing.get(from);
        if (edges == null || edges.remove(to) == null) return false;
        revision++;
        return true;
    }

    public Optional<TransportNode> node(UUID nodeId) { return Optional.ofNullable(nodes.get(nodeId)); }

    public Optional<TransportNode> nodeAt(String dimension, net.minecraft.core.BlockPos position) {
        return nodes.values().stream().filter(node -> node.dimension().equals(dimension)
                && node.position().equals(position)).findFirst();
    }

    public Optional<TransportNode> stationNode(UUID stationId) {
        return nodes.values().stream().filter(node -> node.station().filter(stationId::equals).isPresent()).findFirst();
    }

    public Optional<Path> shortestPath(UUID from, UUID to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) return Optional.empty();
        if (from.equals(to)) return Optional.of(new Path(List.of(nodes.get(from)), List.of(), 0));

        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, TransportEdge> previous = new HashMap<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator
                .comparingDouble(QueueEntry::cost).thenComparing(entry -> entry.node().toString()));
        distances.put(from, 0D);
        queue.add(new QueueEntry(from, 0D));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (current.cost() != distances.getOrDefault(current.node(), Double.POSITIVE_INFINITY)) continue;
            if (current.node().equals(to)) break;
            for (TransportEdge edge : outgoing.getOrDefault(current.node(), Map.of()).values()) {
                double candidate = current.cost() + edge.cost();
                double existing = distances.getOrDefault(edge.to(), Double.POSITIVE_INFINITY);
                if (candidate < existing || candidate == existing
                        && edge.from().toString().compareTo(previous.getOrDefault(edge.to(), edge).from().toString()) < 0) {
                    distances.put(edge.to(), candidate);
                    previous.put(edge.to(), edge);
                    queue.add(new QueueEntry(edge.to(), candidate));
                }
            }
        }
        if (!previous.containsKey(to)) return Optional.empty();

        List<TransportEdge> reverseEdges = new ArrayList<>();
        UUID cursor = to;
        while (!cursor.equals(from)) {
            TransportEdge edge = previous.get(cursor);
            if (edge == null) return Optional.empty();
            reverseEdges.add(edge);
            cursor = edge.from();
        }
        List<TransportEdge> pathEdges = reverseEdges.reversed();
        List<TransportNode> pathNodes = new ArrayList<>();
        pathNodes.add(nodes.get(from));
        pathEdges.forEach(edge -> pathNodes.add(nodes.get(edge.to())));
        return Optional.of(new Path(pathNodes, pathEdges, distances.get(to)));
    }

    /** Builds one closed route through the supplied station IDs in order. First and last must match. */
    public Optional<DispatchRoutePlan> closedPlan(List<UUID> stationIds) {
        if (stationIds.size() < 3 || !stationIds.getFirst().equals(stationIds.getLast())) return Optional.empty();
        List<TransportNode> planNodes = new ArrayList<>();
        List<TransportEdge> planEdges = new ArrayList<>();
        for (int i = 0; i < stationIds.size() - 1; i++) {
            Optional<TransportNode> from = stationNode(stationIds.get(i));
            Optional<TransportNode> to = stationNode(stationIds.get(i + 1));
            if (from.isEmpty() || to.isEmpty()) return Optional.empty();
            Optional<Path> leg = shortestPath(from.get().id(), to.get().id());
            if (leg.isEmpty()) return Optional.empty();
            if (leg.get().edges().isEmpty()) continue;
            if (planNodes.isEmpty()) planNodes.add(leg.get().nodes().getFirst());
            planEdges.addAll(leg.get().edges());
            planNodes.addAll(leg.get().nodes().subList(1, leg.get().nodes().size()));
        }
        if (!planNodes.getFirst().id().equals(planNodes.getLast().id())) return Optional.empty();
        planNodes.removeLast();
        if (planNodes.size() != planEdges.size()) return Optional.empty();
        try {
            return Optional.of(new DispatchRoutePlan(id, revision, name, mode, planNodes, planEdges));
        } catch (IllegalArgumentException incompatibleEdges) {
            return Optional.empty();
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putString("mode", mode.getSerializedName());
        tag.putLong("revision", revision);
        ListTag nodeTags = new ListTag();
        nodes.values().forEach(node -> nodeTags.add(node.save()));
        tag.put("nodes", nodeTags);
        ListTag edgeTags = new ListTag();
        edges().forEach(edge -> edgeTags.add(edge.save()));
        tag.put("edges", edgeTags);
        return tag;
    }

    public static TransportNetwork load(CompoundTag tag) {
        TransportNetwork network = new TransportNetwork(tag.getUUID("id"), tag.getString("name"),
                TransportMode.fromName(tag.getString("mode")));
        ListTag nodeTags = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeTags.size(); i++) {
            TransportNode node = TransportNode.load(nodeTags.getCompound(i));
            network.nodes.put(node.id(), node);
            network.outgoing.put(node.id(), new LinkedHashMap<>());
        }
        ListTag edgeTags = tag.getList("edges", Tag.TAG_COMPOUND);
        for (int i = 0; i < edgeTags.size(); i++) {
            TransportEdge edge = TransportEdge.load(edgeTags.getCompound(i));
            if (network.nodes.containsKey(edge.from()) && network.nodes.containsKey(edge.to())) {
                network.outgoing.computeIfAbsent(edge.from(), ignored -> new LinkedHashMap<>()).put(edge.to(), edge);
            }
        }
        network.revision = tag.getLong("revision");
        return network;
    }
}
