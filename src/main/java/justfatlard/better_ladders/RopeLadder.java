package justfatlard.better_ladders;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Unrolling a rope ladder down whatever is under it. */
public final class RopeLadder {
	private RopeLadder() {}

	/**
	 * Drop ladders from here downwards, one per piece, until something stops them.
	 *
	 * <p>Stops at the first space it cannot have - ground, a chest, anybody's build - rather than
	 * pushing past it, so a rope thrown down a shaft lands on the floor of the shaft instead of
	 * disappearing into whatever is below it.
	 *
	 * @return how many went down, which is also how many pieces to spend
	 */
	public static int unroll(ServerLevel level, BlockPos from, Direction facing, int pieces) {
		BlockState ladder = Main.ROPE_LADDER.defaultBlockState()
			.setValue(LadderBlock.FACING, facing);

		BlockPos.MutableBlockPos at = from.mutable();
		int placed = 0;

		while (placed < pieces && at.getY() >= level.getMinY()) {
			BlockState existing = level.getBlockState(at);
			if (!existing.canBeReplaced()) break;

			boolean wet = existing.getFluidState().getType() == Fluids.WATER;
			level.setBlock(at, ladder.setValue(LadderBlock.WATERLOGGED, wet), Block.UPDATE_ALL);

			placed++;
			at.move(Direction.DOWN);
		}
		return placed;
	}

	/**
	 * Which way the rungs should face, and where the top one goes.
	 *
	 * <p>Off a wall the answer is the wall: the ladder hangs on the face you clicked, the way one
	 * placed by hand would. Off the top of a block there is no wall to read, so it goes over the
	 * edge you are looking across - which is the thing anybody standing at a cliff holding a rope
	 * is about to do anyway.
	 */
	public static BlockPos startFrom(BlockPos clicked, Direction clickedFace, Direction playerFacing) {
		return clickedFace.getAxis().isHorizontal()
			? clicked.relative(clickedFace)
			: clicked.relative(playerFacing);
	}

	/** A ladder faces away from whatever holds it, which is the block it was hung off. */
	public static Direction facingFor(Direction clickedFace, Direction playerFacing) {
		return clickedFace.getAxis().isHorizontal() ? clickedFace : playerFacing;
	}
}
