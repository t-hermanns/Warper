package com.smallow;

import com.smallow.config.ConfigManager;
import com.smallow.config.ModConfig;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SimpleGuiBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Collections;

public class Warper implements ModInitializer {

	public static final String MOD_ID = "warper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ModConfig CONFIG = ConfigManager.loadConfig();
	public static final int WAIT_TIME = CONFIG.WAIT_TIME;
	public static final int DISTANCE = CONFIG.DISTANCE;

	@Override
	public void onInitialize() {

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			if (world.isClient) {
				return true;
			}
			Block block = state.getBlock();
			if(!(block instanceof AbstractSignBlock)){
				return true;
			}
			SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(pos);
			if(sign == null || !sign.isWaxed()){
				return true;
			}
			StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(world.getServer());
			//check if sign is a warp point
			for (Warppoint warppoint : serverState.warppoints) {
				if (warppoint.position.equals(pos)) {
					// remove the warppoint
					serverState.warppoints.remove(warppoint);
					break;
				}
			}
			return true;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) {
				return ActionResult.PASS;
			}
			BlockPos pos = hitResult.getBlockPos();
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if(!(block instanceof AbstractSignBlock)){
				return ActionResult.PASS;
			}
			SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(pos);
			if(sign == null || !sign.isWaxed()){
				return ActionResult.PASS;
			}
			StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(world.getServer());
			//check if sign is a warp point
			for (Warppoint warppoint : serverState.warppoints) {
				if (warppoint.position.equals(pos)) {
					if(warppoint.isInactive()){
						// Compact calculation of the wait time
						String waitTime = String.format("%02d:%02d",
								(WAIT_TIME - (int) (System.currentTimeMillis() / 1000 - warppoint.timestamp)) / 60,
								(WAIT_TIME - (int) (System.currentTimeMillis() / 1000 - warppoint.timestamp)) % 60);

						player.sendMessage(Text.literal("Warp point is not yet active, wait " + waitTime + " minutes"), false);

						return ActionResult.SUCCESS;
					}
					openWarpGui(player, warppoint, sign);
					return ActionResult.SUCCESS;
				}
			}
			return ActionResult.PASS;
		});

		LOGGER.info("Warper initialized");
	}

	private ScreenHandlerType<?> chooseType(StateSaverAndLoader serverState) {
		long activePoints = serverState.warppoints.stream().filter(w -> !w.isInactive()).count();
		if (activePoints-1 <= 9) {
			return ScreenHandlerType.GENERIC_9X1;
		} else if (activePoints-1 <= 18) {
			return ScreenHandlerType.GENERIC_9X2;
		} else if (activePoints-1 <= 27) {
			return ScreenHandlerType.GENERIC_9X3;
		} else if (activePoints-1 <= 36) {
			return ScreenHandlerType.GENERIC_9X4;
		} else if (activePoints-1 <= 45) {
			return ScreenHandlerType.GENERIC_9X5;
		} else if (activePoints-1 <= 54) {
			return ScreenHandlerType.GENERIC_9X6;
		} else {
			return ScreenHandlerType.GENERIC_9X6;
		}
	}

	public void openWarpGui(PlayerEntity player, Warppoint warppoint, SignBlockEntity sign){
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(player.getServer());
		ScreenHandlerType<?> handlertype = chooseType(serverState);
		SimpleGuiBuilder builder = new SimpleGuiBuilder(handlertype, false);
		builder.setTitle(Text.literal("Warp Menu"));
		updateWarpPoints(player.getServer());
		// add items from warppoints list except the current one
		for (Warppoint point : serverState.warppoints) {
			if (point.position.equals(warppoint.position)) {
				continue;
			}
			if (point.isInactive()) {
				continue; // TODO: add but grey out
			}
			builder.addSlot(new ItemStack(point.item).setCustomName(point.name),  (index, type, action, gui) -> {
				player.teleport(player.getServer().getWorld(point.world), point.position.getX()+0.5, point.position.getY(), point.position.getZ()+0.5, Collections.emptySet(), player.getYaw(), player.getPitch());
				player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
				((ServerPlayerEntity) player).closeHandledScreen();
			});
		}
		SimpleGui gui = builder.build((ServerPlayerEntity) player);
		gui.open();
	}

	private void updateWarpPoints(MinecraftServer server) {
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(server);
		// remove warppoints if there is no sign at the location
		serverState.warppoints.removeIf(warppoint ->
				!(server.getWorld(warppoint.world).getBlockState(warppoint.position).getBlock() instanceof AbstractSignBlock)
		);
	}

	public static boolean singChangeCancelled(SignText text, boolean front, SignBlockEntity sign) {
		if (!text.getMessage(0, false).getString().trim().equalsIgnoreCase("[warp]") || !front) {
			return false;
		}
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(sign.getWorld().getServer());
		if(serverState.warppoints.size() >= 9*6+1){
			sign.setText(new SignText().
					withMessage(0, Text.literal("Warp")).
					withMessage(1, Text.literal("§0Too many")).
					withMessage(2, Text.literal("§0warp points")).
					withColor(DyeColor.RED).withGlowing(true), true);
			//clear back text
			sign.setText(new SignText(), false);
			sign.setWaxed(true);
			return true;
		}
		if(text.getMessage(1, false).getLiteralString().isEmpty()){
			sign.setText(new SignText().
					withMessage(0, Text.literal("Warp")).
					withMessage(1, Text.literal("§0Please provide")).
					withMessage(2, Text.literal("§0a name")).
					withColor(DyeColor.RED).withGlowing(true), true);
			//clear back text
			sign.setText(new SignText(), false);
			sign.setWaxed(true);
			return true;
		}
		// check if any warp point in DISTANCE
		for (Warppoint warppoint : serverState.warppoints) {
			if (warppoint.world != sign.getWorld().getRegistryKey()) {
				continue;
			}
			if (warppoint.position.isWithinDistance(sign.getPos(), DISTANCE)) {
				sign.setText(new SignText().
						withMessage(0, Text.literal("Warp")).
						withMessage(1, Text.literal("§0Too close to")).
						withMessage(2, Text.literal("§0another warp")).
						withMessage(3, Text.literal("§0point")).
						withColor(DyeColor.RED).withGlowing(true), true);
				//clear back text
				sign.setText(new SignText(), false);
				sign.setWaxed(true);
				return true;
			}
		}
		Item item = findAttachedBlock(sign).getBlock().asItem();
		if (item == Items.AIR) {
			LOGGER.info("No block attached to sign");
			return false;
		}
		sign.setText(new SignText().
				withMessage(0, Text.literal("Warp")).
				withMessage(1, Text.literal("§4" + (text.getMessage(1, false).getLiteralString()))).
				withColor(DyeColor.CYAN).withGlowing(true), true);
		//clear back text
		sign.setText(new SignText(), false);
		sign.setWaxed(true);
		serverState.warppoints.add(new Warppoint(sign.getWorld().getRegistryKey(), sign.getPos(), text.getMessage(1, false), item));
		return true;
	}

	public static BlockState findAttachedBlock(SignBlockEntity signBlockEntity) {
		World world = signBlockEntity.getWorld();
		BlockPos pos = signBlockEntity.getPos();

		if (world == null) {
			return Blocks.AIR.getDefaultState(); // If the world reference is not available
		}

		BlockState signState = signBlockEntity.getCachedState();

		if (signState.getBlock() instanceof WallSignBlock) {
			// Attempt to access the facing direction of the sign
			Direction facing = null;
			if (signState.contains(Properties.HORIZONTAL_FACING)) {
				facing = signState.get(Properties.HORIZONTAL_FACING);
			} else if (signState.contains(Properties.FACING)) {
				facing = signState.get(Properties.FACING);
			}

			if (facing != null) {
				BlockPos attachedBlockPos = pos.offset(facing.getOpposite());
				return world.getBlockState(attachedBlockPos);
			}
		} else {
			// For standing signs, check the block directly below
			BlockPos blockBelow = pos.down();
			return world.getBlockState(blockBelow);
		}

		return Blocks.AIR.getDefaultState();
	}

}