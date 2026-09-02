package justfatlard.better_ladders;

import justfatlard.pandorical.api.BlockRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MOD_ID = "better-ladders-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier ROPE_LADDER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "rope_ladder");

	public static final ResourceKey<Block> ROPE_LADDER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ROPE_LADDER_ID);
	public static final ResourceKey<Item> ROPE_LADDER_KEY = ResourceKey.create(Registries.ITEM, ROPE_LADDER_ID);

	// Rope and slats, not nailed timber: quieter and quicker to cut than a ladder.
	public static final RopeLadderBlock ROPE_LADDER = new RopeLadderBlock(
		BlockBehaviour.Properties.of()
			.strength(0.3F)
			.sound(SoundType.WOOL)
			.noOcclusion()
			.setId(ROPE_LADDER_BLOCK_KEY)
	);

	public static final RopeLadderItem ROPE_LADDER_ITEM = new RopeLadderItem(
		ROPE_LADDER,
		new Item.Properties().setId(ROPE_LADDER_KEY).useBlockDescriptionPrefix()
	);

	@Override
	public void onInitialize() {
		if (PandoricalApi.isAvailable()) {
			PandoricalApi.content().registerBlock(MOD_ID + ":rope_ladder", new BlockRegistration()
				.baseBlock("minecraft:ladder")
				.model(MOD_ID + ":block/rope_ladder"));
			PandoricalApi.content().registerItem(MOD_ID + ":rope_ladder", new ItemRegistration()
				.model(MOD_ID + ":item/rope_ladder"));
			PandoricalApi.content().registerModAssets(MOD_ID);

			// Vanilla's ladder is one flat plane, so a ladder seen edge-on is not there at all.
			// This is the same texture given rails and rungs, served above vanilla's own copy by
			// the synced pack. registerModAssets does not scan the minecraft namespace, so it is
			// handed over by name.
			syncModelOverride("minecraft/models/block/ladder.json");
		}

		Registry.register(BuiltInRegistries.BLOCK, ROPE_LADDER_ID, ROPE_LADDER);
		Registry.register(BuiltInRegistries.ITEM, ROPE_LADDER_ID, ROPE_LADDER_ITEM);

		System.out.println("[" + MOD_ID + "] Loaded (server-side with Pandorical)");
	}

	/** Ship one of our {@code minecraft}-namespace overrides, which registerModAssets skips. */
	private static void syncModelOverride(String path) {
		try (java.io.InputStream in = Main.class.getClassLoader().getResourceAsStream("assets/" + path)) {
			if (in == null) {
				LOGGER.error("[{}] Missing bundled asset {}; ladders stay flat", MOD_ID, path);
				return;
			}
			PandoricalApi.content().registerAsset(path, in.readAllBytes());
		} catch (java.io.IOException e) {
			LOGGER.error("[{}] Could not read bundled asset {}: {}", MOD_ID, path, e.getMessage());
		}
	}
}
