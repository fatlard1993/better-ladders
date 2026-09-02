package justfatlard.better_ladders;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A coil of ladder that unrolls itself.
 *
 * <p>The stack is the rope. One piece becomes one rung's worth of ladder, dropped from where you
 * threw it until it reaches the ground or you run out, which is the whole of it: no placing rung
 * by rung down a shaft you cannot see the bottom of, and no guessing how deep the shaft was.
 *
 * <p>A block item whose placement is a whole rope rather than one rung. Breaking any of it gives
 * the pieces back, so the coil and what it becomes are the same thing in two shapes.
 */
public class RopeLadderItem extends BlockItem {
	public RopeLadderItem(net.minecraft.world.level.block.Block block, Properties settings) {
		super(block, settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

		Direction clickedFace = context.getClickedFace();
		Direction playerFacing = context.getHorizontalDirection().getOpposite();

		BlockPos start = RopeLadder.startFrom(context.getClickedPos(), clickedFace, playerFacing);
		Direction facing = RopeLadder.facingFor(clickedFace, playerFacing);

		ItemStack stack = context.getItemInHand();
		int placed = RopeLadder.unroll(serverLevel, start, facing, stack.getCount());

		if (placed == 0) {
			player.sendSystemMessage(Component.translatable("better-ladders-justfatlard.rope.no_room"));
			return InteractionResult.FAIL;
		}

		stack.consume(placed, player);
		serverLevel.playSound(null, start, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS,
			1.0F, 0.9F);

		return InteractionResult.SUCCESS;
	}
}
