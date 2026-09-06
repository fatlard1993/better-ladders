package justfatlard.better_ladders;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Unrolling a rope ladder down whatever is under it.
 *
 * <p>Not all at once. A rope thrown off a ledge takes time to reach the bottom, and one that
 * appeared whole in the same instant it left your hand would not read as thrown at all. So the
 * first rung lands with the click and the rest follow it down tick by tick, gathering speed the
 * way anything falling does, each rung rattling as it lands and the whole thing thudding when it
 * meets the floor. Decided here on the server, so a vanilla client sees and hears all of it.
 */
public final class RopeLadder {
	private RopeLadder() {}

	/** As far as one throw reaches. The same ceiling as one cut, for the same reason. */
	private static final int MAX_DROP = 512;

	/** Ticks between rungs at the top of a drop; it quickens from here. */
	private static final int SLOWEST = 3;
	/** Rungs that go down at each pace before the rope quickens to the next. */
	private static final int RUNGS_PER_PACE = 4;

	/** Every rope still on its way down, across all levels. */
	private static final List<Drop> falling = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(RopeLadder::tick);

		// A rope half-way down when the server stops would lose its tail with the tick that never
		// comes, so every tail goes down now, the way it would have before it had an animation.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Drop drop : falling) {
				drop.finishNow();
			}
			falling.clear();
		});
	}

	private static void tick(ServerLevel level) {
		falling.removeIf(drop -> drop.level == level && drop.tick());
	}

	/**
	 * Throw a rope down from here.
	 *
	 * <p>The drop is measured before anything moves - how far it can go before something stops it,
	 * or the pieces run out - because that number is what the throw costs, and the cost has to be
	 * known while the pieces are still in the player's hand. The first rung lands at once; the rest
	 * are on their way.
	 *
	 * @return how many rungs will go down, which is also how many pieces to spend
	 */
	public static int throwDown(ServerLevel level, BlockPos from, Direction facing, int pieces) {
		int run = measure(level, from, pieces);
		if (run == 0) return 0;

		level.playSound(null, from, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

		Drop drop = new Drop(level, from, facing, run);
		if (!drop.tick()) falling.add(drop);
		return run;
	}

	/**
	 * How many rungs can go down from here.
	 *
	 * <p>Stops at the first space it cannot have - ground, a chest, anybody's build - rather than
	 * pushing past it, so a rope thrown down a shaft lands on the floor of the shaft instead of
	 * disappearing into whatever is below it.
	 */
	private static int measure(ServerLevel level, BlockPos from, int pieces) {
		int limit = Math.min(pieces, MAX_DROP);
		BlockPos.MutableBlockPos at = from.mutable();
		int free = 0;

		while (free < limit && at.getY() >= level.getMinY()) {
			if (!level.getBlockState(at).canBeReplaced()) break;
			free++;
			at.move(Direction.DOWN);
		}
		return free;
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

	/** One rope on its way down. */
	private static final class Drop {
		final ServerLevel level;
		private final BlockState ladder;
		/** Where the next rung goes. */
		private final BlockPos.MutableBlockPos at;
		private final int run;
		private int placed = 0;
		private int wait = 0;

		Drop(ServerLevel level, BlockPos from, Direction facing, int run) {
			this.level = level;
			this.ladder = Main.ROPE_LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
			this.at = from.mutable();
			this.run = run;
		}

		/** @return whether the rope is down, one way or another */
		boolean tick() {
			if (wait-- > 0) return false;

			// The world was measured when the rope left the hand and has had ticks to change since.
			// A rung with nothing above it was cut mid-fall; a space that has filled is a floor
			// that was not there. Either way the rope stops here and the rest is handed back.
			boolean cut = placed > 0 && !(level.getBlockState(at.above()).getBlock() instanceof RopeLadderBlock);
			BlockState existing = level.getBlockState(at);
			if (cut || !existing.canBeReplaced()) {
				handBack(cut ? at : at.above());
				if (!cut) land(at);
				return true;
			}

			boolean wet = existing.getFluidState().getType() == Fluids.WATER;
			level.setBlock(at, ladder.setValue(LadderBlock.WATERLOGGED, wet), Block.UPDATE_ALL);
			placed++;

			// Each rung lands a little lower in pitch than the last: the ear hears it going down.
			float pitch = 1.2F - 0.4F * placed / run;
			level.playSound(null, at, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS, 0.4F, pitch);

			if (placed >= run) {
				if (!level.getBlockState(at.below()).canBeReplaced()) land(at.below());
				return true;
			}

			at.move(Direction.DOWN);
			wait = Math.max(1, SLOWEST - placed / RUNGS_PER_PACE) - 1;
			return false;
		}

		/** The rest of the rope, all at once and in silence. For when there are no more ticks. */
		void finishNow() {
			while (placed < run) {
				BlockState existing = level.getBlockState(at);
				if (!existing.canBeReplaced()) {
					handBack(at.above());
					return;
				}
				boolean wet = existing.getFluidState().getType() == Fluids.WATER;
				level.setBlock(at, ladder.setValue(LadderBlock.WATERLOGGED, wet), Block.UPDATE_ALL);
				placed++;
				at.move(Direction.DOWN);
			}
		}

		/** The bottom rung meeting the floor: a thud, and dust off whatever it landed on. */
		private void land(BlockPos floor) {
			BlockPos rung = floor.above();
			level.playSound(null, rung, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(floor)),
				rung.getX() + 0.5, rung.getY() + 0.1, rung.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.0);
		}

		/** Pieces that were spent on rungs that never went down, returned where the rope ended. */
		private void handBack(BlockPos where) {
			int unplaced = run - placed;
			if (unplaced <= 0) return;
			Block.popResource(level, where, new ItemStack(Main.ROPE_LADDER_ITEM, unplaced));
			placed = run;
		}
	}
}
