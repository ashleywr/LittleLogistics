package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.barge.ChestBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.item.creative.CreativeCapacitor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityRegistration {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {

        // === Entity capabilities: ITEM_HANDLER ===

        // SteamTugEntity -> fuel item handler
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_TUG.get(),
            (entity, ctx) -> entity instanceof SteamTugEntity tug ? tug.getFuelItemHandler() : null
        );

        // EnergyTugEntity -> energy item handler
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity instanceof EnergyTugEntity tug ? tug.getItemHandler() : null
        );

        // SteamLocomotiveEntity -> fuel item handler
        // Note: STEAM_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_LOCOMOTIVE.get(),
            (entity, ctx) -> entity instanceof SteamLocomotiveEntity locomotive
                ? locomotive.getFuelItemHandler() : null
        );

        // EnergyLocomotiveEntity -> energy item handler
        // Note: ENERGY_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> entity instanceof EnergyLocomotiveEntity locomotive
                ? locomotive.getEnergyItemHandler() : null
        );

        // ChestCarEntity -> chest item handler
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.CHEST_CAR.get(),
            (entity, ctx) -> entity instanceof ChestCarEntity car ? car.getRawHandler() : null
        );

        // BarrelCarEntity -> chest item handler (same class as ChestCarEntity)
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.BARREL_CAR.get(),
            (entity, ctx) -> entity instanceof ChestCarEntity car ? car.getRawHandler() : null
        );

        // ChestBargeEntity -> chest item handler
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.CHEST_BARGE.get(),
            (entity, ctx) -> entity instanceof ChestBargeEntity barge
                ? barge.getRawHandler() : null
        );

        // BarrelBargeEntity -> chest item handler (same class as ChestBargeEntity)
        registerEntitySafely(event,
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.BARREL_BARGE.get(),
            (entity, ctx) -> entity instanceof ChestBargeEntity barge
                ? barge.getRawHandler() : null
        );

        // === Entity capabilities: ENERGY ===

        // EnergyTugEntity -> internal battery
        registerEntitySafely(event,
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity instanceof EnergyTugEntity tug ? tug.getInternalBattery() : null
        );

        // EnergyLocomotiveEntity -> internal battery
        // Note: ENERGY_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        registerEntitySafely(event,
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> entity instanceof EnergyLocomotiveEntity locomotive
                ? locomotive.getInternalBattery() : null
        );

        // === Entity capabilities: FLUID_HANDLER ===

        // FluidTankBargeEntity -> fluid tank
        registerEntitySafely(event,
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_TANK_BARGE.get(),
            (entity, ctx) -> entity instanceof FluidTankBargeEntity barge ? barge.getTank() : null
        );

        // FluidTankCarEntity -> fluid tank
        registerEntitySafely(event,
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_CAR.get(),
            (entity, ctx) -> entity instanceof FluidTankCarEntity car ? car.getTank() : null
        );

        // === BlockEntity capabilities ===

        // Docking station port capabilities — exposed on every side except the inward face
        // that points at the docked vehicle (the vehicle lane, FACING.getOpposite()).
        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            (level, pos, state, blockEntity, direction) -> {
                DockingStationBlockEntity dock = dockAt(level, pos, state, direction);
                return dock == null ? null : dock.getItemHandler();
            },
            ModBlocks.DOCKING_STATION.get()
        );
        event.registerBlock(
            Capabilities.FluidHandler.BLOCK,
            (level, pos, state, blockEntity, direction) -> {
                DockingStationBlockEntity dock = dockAt(level, pos, state, direction);
                return dock == null ? null : dock.getFluidHandler();
            },
            ModBlocks.DOCKING_STATION.get()
        );
        event.registerBlock(
            Capabilities.EnergyStorage.BLOCK,
            (level, pos, state, blockEntity, direction) -> {
                DockingStationBlockEntity dock = dockAt(level, pos, state, direction);
                return dock == null ? null : dock.getEnergyStorage();
            },
            ModBlocks.DOCKING_STATION.get()
        );

        // === Item capabilities ===

        // CreativeCapacitor -> infinite energy
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new CreativeCapacitor.CreativeEnergyStorage(),
            ModItems.CREATIVE_CAPACITOR.get()
        );

        // === Entity capabilities: STALLING ===

        // Head vehicles — tugs
        registerStalling(event, ModEntityTypes.STEAM_TUG.get());
        registerStalling(event, ModEntityTypes.ENERGY_TUG.get());

        // Head vehicles — locomotives (typed as EntityType<AbstractLocomotiveEntity>)
        registerStalling(event, ModEntityTypes.STEAM_LOCOMOTIVE.get());
        registerStalling(event, ModEntityTypes.ENERGY_LOCOMOTIVE.get());

        // Tail vehicles — barges
        registerStalling(event, ModEntityTypes.CHEST_BARGE.get());
        registerStalling(event, ModEntityTypes.BARREL_BARGE.get());
        registerStalling(event, ModEntityTypes.CHUNK_LOADER_BARGE.get());
        registerStalling(event, ModEntityTypes.FISHING_BARGE.get());
        registerStalling(event, ModEntityTypes.FLUID_TANK_BARGE.get());
        registerStalling(event, ModEntityTypes.SEATER_BARGE.get());
        registerStalling(event, ModEntityTypes.VACUUM_BARGE.get());

        // Tail vehicles — wagons
        registerStalling(event, ModEntityTypes.CHEST_CAR.get());
        registerStalling(event, ModEntityTypes.BARREL_CAR.get());
        registerStalling(event, ModEntityTypes.SEATER_CAR.get());
        registerStalling(event, ModEntityTypes.FLUID_CAR.get());
        registerStalling(event, ModEntityTypes.CHUNK_LOADER_CAR.get());
    }

    /**
     * NeoForge multipart entities inherit their parent's {@link EntityType}. Capability lookup
     * therefore invokes providers registered for the vehicle type when another mod scans a
     * {@code PartEntity}. Keeping the provider itself typed to {@link Entity} avoids the bridge
     * cast generated for a concrete vehicle class; each extractor can reject the part with null.
     */
    private static <T, C, E extends Entity> void registerEntitySafely(
            RegisterCapabilitiesEvent event, EntityCapability<T, C> capability,
            EntityType<E> entityType, ICapabilityProvider<Entity, C, T> provider) {
        event.registerEntity(capability, entityType, provider);
    }

    private static <E extends Entity> void registerStalling(
            RegisterCapabilitiesEvent event, EntityType<E> entityType) {
        registerEntitySafely(event, StallingCapability.ENTITY_CAPABILITY, entityType,
            (entity, ctx) -> entity instanceof StallingCapability stalling ? stalling : null);
    }

    private static DockingStationBlockEntity dockAt(Level level, BlockPos pos, BlockState state,
                                                     Direction direction) {
        if (direction != null && direction == state.getValue(DockingStationBlock.FACING).getOpposite()) {
            return null;
        }
        return DockingStationBlock.getControllerBE(state, level, pos);
    }
}
