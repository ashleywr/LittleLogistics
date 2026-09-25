package dev.murad.shipping.item;

import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class WrenchItem extends Item {
    private static final Map<RailShape, RailShape> nextShapes = Map.ofEntries(
            Map.entry(RailShape.EAST_WEST, RailShape.NORTH_SOUTH),
            Map.entry(RailShape.NORTH_SOUTH, RailShape.NORTH_EAST),
            Map.entry(RailShape.NORTH_EAST, RailShape.NORTH_WEST),
            Map.entry(RailShape.NORTH_WEST, RailShape.SOUTH_WEST),
            Map.entry(RailShape.SOUTH_WEST, RailShape.SOUTH_EAST),
            Map.entry(RailShape.SOUTH_EAST, RailShape.EAST_WEST)
    );

    private Component wrenchInfo = Component.translatable("item.littlelogistics.conductors_wrench.description");

    public WrenchItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        tooltip.add(wrenchInfo);
    }


    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        var level = pContext.getLevel();
        var clickedPos = pContext.getClickedPos();
        var state = level.getBlockState(clickedPos);
        Player player = pContext.getPlayer();

        if (player != null && player.isShiftKeyDown() && state.getBlock() instanceof DockingStationBlock) {
            BlockPos controllerPos = state.getValue(DockingStationBlock.PART)
                    .controllerPos(clickedPos, state.getValue(DockingStationBlock.FACING));
            if (!level.getBlockState(controllerPos).is(state.getBlock())) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.destroyBlock(controllerPos, !player.getAbilities().instabuild, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if(state.is(Blocks.RAIL)){
            var shape = state.getValue(RailBlock.SHAPE);
            if(shape.isAscending()){
                return InteractionResult.PASS;
            }
            if(!level.isClientSide()){
                level.setBlock(clickedPos,
                        state.setValue(RailBlock.SHAPE, nextShapes.getOrDefault(shape, shape)), 2);
            }
            return InteractionResult.SUCCESS;
        } else {
            return super.useOn(pContext);
        }
    }
}
