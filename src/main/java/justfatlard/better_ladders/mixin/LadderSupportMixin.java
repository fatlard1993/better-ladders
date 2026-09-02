package justfatlard.better_ladders.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A ladder holds itself up.
 *
 * <p>Vanilla wants a solid face behind every rung, which is what turns a climb down a shaft into a
 * building job: you cannot put a ladder where there is nothing to nail it to, so you first build a
 * wall you did not want in order to hang a ladder on it, in a hole you are about to leave.
 *
 * <p>Two halves, and both are needed. Survival is what stops a placed ladder popping off when the
 * wall behind it goes; placement is what lets one go up in the first place, because vanilla decides
 * there is nowhere to put it and hands back nothing at all.
 */
@Mixin(LadderBlock.class)
public class LadderSupportMixin {

	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void betterLadders$standAlone(BlockState state, LevelReader level, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}

	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void betterLadders$faceThePlacer(BlockPlaceContext context,
			CallbackInfoReturnable<BlockState> cir) {
		// Only when vanilla found nowhere to hang it. A ladder that does have a wall behind it
		// should still take the wall's orientation, not the one the player happens to be facing.
		if (cir.getReturnValue() != null) return;

		LadderBlock self = (LadderBlock) (Object) this;
		cir.setReturnValue(self.defaultBlockState()
			.setValue(LadderBlock.FACING, context.getHorizontalDirection().getOpposite())
			.setValue(LadderBlock.WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).getType()
					== net.minecraft.world.level.material.Fluids.WATER));
	}
}
