package justfatlard.better_ladders;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rope ladder, which is a ladder that hangs rather than one that is nailed on.
 *
 * <p>A {@link LadderBlock} on purpose: climbing, facing, waterlogging, the shape you walk into and
 * the placement rules are all a ladder's, and this mod has already taught that class to stand
 * without a wall - which a hanging rope needs more than anything else does.
 *
 * <p>What is its own is the cut. Take out a rung and everything under it comes down, because the
 * only thing holding the rest up was that rung.
 */
public class RopeLadderBlock extends LadderBlock {
	/** As deep as one cut reaches. A rope longer than this is not a rope, it is a bug. */
	private static final int MAX_FALL = 512;

	public RopeLadderBlock(Properties settings) {
		super(settings);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
			boolean movedByPiston) {
		cutBelow(level, pos);

		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	/**
	 * Drop everything hanging from this spot.
	 *
	 * <p>Gathered first and destroyed from the bottom up, which is what keeps this from being
	 * recursive: by the time each one is removed the rope under it is already gone, so its own
	 * cut finds nothing and stops. Removing top-down instead would have each removal call this
	 * again, one frame deeper per rung, and a rope down a deep shaft is a lot of rungs.
	 */
	private static void cutBelow(ServerLevel level, BlockPos pos) {
		List<BlockPos> hanging = new ArrayList<>();

		BlockPos.MutableBlockPos below = pos.mutable().move(net.minecraft.core.Direction.DOWN);
		while (hanging.size() < MAX_FALL && level.getBlockState(below).getBlock() instanceof RopeLadderBlock) {
			hanging.add(below.immutable());
			below.move(net.minecraft.core.Direction.DOWN);
		}

		for (int i = hanging.size() - 1; i >= 0; i--) {
			// Dropped rather than deleted: the rope is still rope, it is just on the floor now.
			level.destroyBlock(hanging.get(i), true);
		}
	}
}
