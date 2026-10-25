package dev.murad.shipping.item;

import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.block.dockingstation.DockingStationRegistry;
import dev.murad.shipping.setup.ModDataComponents;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.transport.NetworkChartReference;
import dev.murad.shipping.transport.TransportEdge;
import dev.murad.shipping.transport.TransportMode;
import dev.murad.shipping.transport.TransportNetwork;
import dev.murad.shipping.transport.TransportNetworkData;
import dev.murad.shipping.transport.TransportNode;
import dev.murad.shipping.util.LocoRouteCompiler;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteCompiler;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative editor and shared reference for a directed transport graph. */
public final class NetworkChartItem extends Item {
    private static final double MAX_WATER_EDGE_DISTANCE = 256.0D;

    public NetworkChartItem(Properties properties) {
        super(properties);
    }

    public static Optional<NetworkChartReference> getReference(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModDataComponents.NETWORK_CHART));
    }

    public static boolean matchesMode(ItemStack stack, TransportMode mode) {
        return stack.getItem() instanceof NetworkChartItem
                && getReference(stack).map(reference -> reference.mode() == mode).orElse(false);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        Optional<NetworkChartReference> existing = getReference(stack);
        if (existing.isEmpty()) return createNetwork(level, context, stack);

        TransportNetworkData data = TransportNetworkData.get(level.getServer());
        Optional<TransportNetwork> network = data.get(existing.get().networkId());
        if (network.isEmpty()) {
            message(context.getPlayer(), "This chart references a missing transport network.", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            network.get().rename(customName.getString());
            data.changed();
        }

        Optional<NodeTarget> target = resolveTarget(level, context.getClickedPos(), network.get().mode());
        if (target.isEmpty()) return InteractionResult.PASS;
        TransportNode node;
        Optional<TransportNode> existingNode = network.get().nodeAt(
                level.dimension().location().toString(), target.get().position());
        if (existingNode.isPresent()) {
            node = existingNode.get();
            if (target.get().stationId() != null && node.station().isEmpty()) {
                node = new TransportNode(node.id(), target.get().name(), node.dimension(),
                        node.position(), target.get().stationId());
                network.get().putNode(node);
                data.changed();
            }
        } else {
            node = target.get().toNode(level);
            network.get().putNode(node);
            data.changed();
        }

        Optional<UUID> selected = existing.get().selectedNode();
        boolean wrenchEditing = context.getPlayer() != null
                && context.getPlayer().getOffhandItem().is(ModItems.CONDUCTORS_WRENCH.get());
        if (wrenchEditing && selected.isEmpty() && context.getPlayer().isShiftKeyDown()) {
            network.get().removeNode(node.id());
            data.changed();
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().clearSelection());
            message(context.getPlayer(), "Removed " + node.name() + " and its connected edges.", ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }
        if (selected.isEmpty()) {
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().select(node.id()));
            message(context.getPlayer(), "Selected " + node.name() + ". Select the destination node.", ChatFormatting.AQUA);
            return InteractionResult.SUCCESS;
        }
        if (selected.get().equals(node.id())) {
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().clearSelection());
            message(context.getPlayer(), "Network edge selection cleared.", ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }

        Optional<TransportNode> from = network.get().node(selected.get());
        if (from.isEmpty()) {
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().select(node.id()));
            message(context.getPlayer(), "The old selection no longer exists; selected this node instead.", ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }

        if (wrenchEditing) {
            boolean removed = network.get().removeEdge(from.get().id(), node.id());
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                removed |= network.get().removeEdge(node.id(), from.get().id());
            }
            data.changed();
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().clearSelection());
            message(context.getPlayer(), removed ? "Removed the selected network edge."
                    : "No selected network edge existed.", ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }

        boolean bidirectional = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        boolean success = compileEdges(level, network.get(), from.get(), node, bidirectional, context.getPlayer());
        if (success) {
            data.changed();
            stack.set(ModDataComponents.NETWORK_CHART, existing.get().clearSelection());
        }
        return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private InteractionResult createNetwork(ServerLevel level, UseOnContext context, ItemStack stack) {
        Optional<TransportMode> mode = inferMode(level, context.getClickedPos());
        if (mode.isEmpty()) {
            message(context.getPlayer(), "Start a Network Chart by using it on rail or navigable water.", ChatFormatting.YELLOW);
            return InteractionResult.FAIL;
        }
        Optional<NodeTarget> target = resolveTarget(level, context.getClickedPos(), mode.get());
        if (target.isEmpty()) return InteractionResult.FAIL;

        TransportNetworkData data = TransportNetworkData.get(level.getServer());
        TransportNetwork network = data.create(mode.get() == TransportMode.RAIL ? "Rail Network" : "Water Network", mode.get());
        TransportNode first = target.get().toNode(level);
        network.putNode(first);
        data.changed();
        stack.set(ModDataComponents.NETWORK_CHART,
                new NetworkChartReference(network.id(), mode.get(), Optional.of(first.id())));
        message(context.getPlayer(), "Created " + network.name() + ". Select the destination node.", ChatFormatting.GREEN);
        return InteractionResult.SUCCESS;
    }

    private boolean compileEdges(ServerLevel level, TransportNetwork network, TransportNode from,
                                 TransportNode to, boolean bidirectional, @Nullable Player player) {
        if (!from.dimension().equals(to.dimension())) {
            message(player, "Transport edges cannot cross dimensions.", ChatFormatting.RED);
            return false;
        }
        if (network.mode() == TransportMode.RAIL) {
            var forward = LocoRouteCompiler.compile(level, from.position(), to.position(), null, null);
            if (!forward.success()) {
                message(player, forward.error(), ChatFormatting.RED);
                return false;
            }
            network.putEdge(TransportEdge.rail(from.id(), to.id(), forward.segment()));
            if (bidirectional) {
                var reverse = LocoRouteCompiler.compile(level, to.position(), from.position(), null, null);
                if (!reverse.success()) {
                    network.removeEdge(from.id(), to.id());
                    message(player, reverse.error(), ChatFormatting.RED);
                    return false;
                }
                network.putEdge(TransportEdge.rail(to.id(), from.id(), reverse.segment()));
            }
        } else {
            TugRoute route = new TugRoute(null, List.of(TugRouteNode.fromBlockPos(from.position()),
                    TugRouteNode.fromBlockPos(to.position())));
            var result = TugRouteCompiler.compile(level, route, MAX_WATER_EDGE_DISTANCE);
            if (!result.success() || result.route().getSegments().size() != 2) {
                message(player, result.error() == null ? "Could not compile the water edge." : result.error(), ChatFormatting.RED);
                return false;
            }
            network.putEdge(TransportEdge.water(from.id(), to.id(), result.route().getSegments().get(0)));
            if (bidirectional) {
                network.putEdge(TransportEdge.water(to.id(), from.id(), result.route().getSegments().get(1)));
            }
        }
        message(player, "Added " + (bidirectional ? "bidirectional" : "directed") + " edge from "
                + from.name() + " to " + to.name() + ".", ChatFormatting.GREEN);
        return true;
    }

    private static Optional<TransportMode> inferMode(ServerLevel level, BlockPos clicked) {
        DockingStationBlockEntity dock = DockingStationBlock.getControllerBE(level.getBlockState(clicked), level, clicked);
        if (dock != null) clicked = dock.getVehicleBlockPos();
        if (LocoRouteCompiler.resolveRailWaypoint(level, clicked).isPresent()) return Optional.of(TransportMode.RAIL);
        if (TugRouteCompiler.resolveWaypointAnchor(level, clicked).isPresent()) return Optional.of(TransportMode.WATER);
        return Optional.empty();
    }

    private static Optional<NodeTarget> resolveTarget(ServerLevel level, BlockPos clicked, TransportMode mode) {
        DockingStationBlockEntity dock = DockingStationBlock.getControllerBE(level.getBlockState(clicked), level, clicked);
        if (dock != null && dock.getStationId() != null) {
            DockingStationRegistry.StationData station = DockingStationRegistry.getOrCreate(level).getStation(dock.getStationId());
            String name = station == null ? "Docking Station" : station.name;
            return Optional.of(new NodeTarget(dock.getVehicleBlockPos(), name, dock.getStationId()));
        }
        Optional<BlockPos> position = mode == TransportMode.RAIL
                ? LocoRouteCompiler.resolveRailWaypoint(level, clicked)
                : TugRouteCompiler.resolveWaypointAnchor(level, clicked);
        return position.map(pos -> new NodeTarget(pos, "Waypoint " + pos.toShortString(), null));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        getReference(stack).ifPresentOrElse(reference -> {
            tooltip.add(Component.literal(reference.mode() == TransportMode.RAIL ? "Rail network" : "Water network")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(reference.networkId().toString()).withStyle(ChatFormatting.DARK_GRAY));
            if (reference.selectedNode().isPresent()) {
                tooltip.add(Component.literal("Destination selection active").withStyle(ChatFormatting.AQUA));
            }
        }, () -> tooltip.add(Component.literal("Use on rail or water to create a shared network")
                .withStyle(ChatFormatting.GRAY)));
    }

    private static void message(@Nullable Player player, @Nullable String text, ChatFormatting color) {
        if (player != null && text != null) player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private record NodeTarget(BlockPos position, String name, @Nullable UUID stationId) {
        TransportNode toNode(ServerLevel level) {
            return new TransportNode(UUID.randomUUID(), name, level.dimension().location().toString(),
                    position.immutable(), stationId);
        }
    }
}
