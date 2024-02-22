package com.refinedmods.refinedstorage.item;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.node.ICoverable;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeGraph;
import com.refinedmods.refinedstorage.apiimpl.network.node.CrafterNetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.node.cover.Cover;
import com.refinedmods.refinedstorage.util.NetworkUtils;
import com.refinedmods.refinedstorage.util.LevelUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WrenchItem extends Item {
    public WrenchItem() {
        super(new Item.Properties().tab(RS.CREATIVE_MODE_TAB).stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) {
            return InteractionResult.CONSUME;
        }

        INetworkNode node = NetworkUtils.getNodeFromBlockEntity(ctx.getLevel().getBlockEntity(ctx.getClickedPos()));
        INetwork network = NetworkUtils.getNetworkFromNode(node);
        if (network != null && !network.getSecurityManager().hasPermission(Permission.BUILD, ctx.getPlayer())) {
            LevelUtils.sendNoPermissionMessage(ctx.getPlayer());

            return InteractionResult.FAIL;
        }
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());

        if (node instanceof ICoverable && ((ICoverable) node).getCoverManager().hasCover(ctx.getClickedFace())) {
            Cover cover = ((ICoverable) node).getCoverManager().removeCover(ctx.getClickedFace());
            if (cover != null) {
                ItemStack stack1 = cover.getType().createStack();
                CoverItem.setItem(stack1, cover.getStack());
                ItemHandlerHelper.giveItemToPlayer(ctx.getPlayer(), stack1);
                ctx.getLevel().sendBlockUpdated(ctx.getClickedPos(), state, state, 3);
                ctx.getLevel().updateNeighborsAt(ctx.getClickedPos(), ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock());
                return InteractionResult.SUCCESS;
            }
        }

        ctx.getLevel().setBlockAndUpdate(ctx.getClickedPos(), state.rotate(ctx.getLevel(), ctx.getClickedPos(), Rotation.CLOCKWISE_90));
        if (node instanceof ICoverable || node instanceof CrafterNetworkNode) {
            node.markDirty();
            network = node.getNetwork();
            if (network != null) {
                network.getNodeGraph().invalidate(Action.PERFORM, node.getNetwork().getLevel(), node.getNetwork().getPosition());
            }
            for (Direction facing : Direction.values()) {
                node = NetworkUtils.getNodeFromBlockEntity(ctx.getLevel().getBlockEntity(ctx.getClickedPos().relative(facing)));

                if (node != null && node.getNetwork() != null && !node.getNetwork().equals(network)) {
                    node.getNetwork().getNodeGraph().invalidate(Action.PERFORM, node.getNetwork().getLevel(), node.getNetwork().getPosition());
                }
            }
        }

        return InteractionResult.CONSUME;
    }
}
