package com.smallow;

import com.smallow.config.ConfigManager;
import com.smallow.config.ModConfig;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			if (world.isClientSide()) {
				return true;
			}
			Block block = state.getBlock();
			if(!(block instanceof SignBlock)){
				return true;
			}
			if(!(world.getBlockEntity(pos) instanceof SignBlockEntity sign) || !sign.isWaxed()){
				return true;
			}
			StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(world.getServer());
			//check if sign is a warp point
			for (Warppoint warppoint : serverState.warppoints) {
				if (warppoint.world.equals(world.dimension()) && warppoint.position.equals(pos)) {
					// remove the warppoint
					serverState.warppoints.remove(warppoint);
					break;
				}
			}
			return true;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hitResult.getBlockPos();
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if(!(block instanceof SignBlock)){
				return InteractionResult.PASS;
			}
			if(!(world.getBlockEntity(pos) instanceof SignBlockEntity sign) || !sign.isWaxed()){
				return InteractionResult.PASS;
			}
			StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(world.getServer());
			//check if sign is a warp point
			for (Warppoint warppoint : serverState.warppoints) {
				if (warppoint.world.equals(world.dimension()) && warppoint.position.equals(pos)) {
					if(warppoint.isInactive()){
						// Compact calculation of the wait time
						String waitTime = String.format("%02d:%02d",
								(WAIT_TIME - (int) (System.currentTimeMillis() / 1000 - warppoint.timestamp)) / 60,
								(WAIT_TIME - (int) (System.currentTimeMillis() / 1000 - warppoint.timestamp)) % 60);

						player.displayClientMessage(Component.literal("Warp point is not yet active, wait " + waitTime + " minutes"), false);

						return InteractionResult.SUCCESS;
					}
					openWarpGui((ServerPlayer) player, warppoint);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		LOGGER.info("Warper initialized");
	}

	private MenuType<?> chooseType(StateSaverAndLoader serverState) {
		long activePoints = serverState.warppoints.stream().filter(w -> !w.isInactive()).count();
		if (activePoints-1 <= 9) {
			return MenuType.GENERIC_9x1;
		} else if (activePoints-1 <= 18) {
			return MenuType.GENERIC_9x2;
		} else if (activePoints-1 <= 27) {
			return MenuType.GENERIC_9x3;
		} else if (activePoints-1 <= 36) {
			return MenuType.GENERIC_9x4;
		} else if (activePoints-1 <= 45) {
			return MenuType.GENERIC_9x5;
		} else {
			return MenuType.GENERIC_9x6;
		}
	}

	public void openWarpGui(ServerPlayer player, Warppoint warppoint){
		MinecraftServer server = player.level().getServer();
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(server);
		SimpleGui gui = new SimpleGui(chooseType(serverState), player, false);
		gui.setTitle(Component.literal("Warp Menu"));
		// add items from warppoints list except the current one
		for (Warppoint point : serverState.warppoints) {
			if (point.world.equals(warppoint.world) && point.position.equals(warppoint.position)) {
				continue;
			}
			if (point.isInactive()) {
				continue; // TODO: add but grey out
			}
			ItemStack icon = new ItemStack(point.item);
			icon.set(DataComponents.CUSTOM_NAME, point.name);
			gui.addSlot(icon, (index, type, action, slotGui) -> {
				ServerLevel targetWorld = server.getLevel(point.world);
				if (targetWorld == null || !warpPointExists(server, point)) {
					player.sendSystemMessage(Component.literal("§cSorry, this Warp point does not exist anymore"));
				} else {
					player.teleportTo(targetWorld, point.position.getX() + 0.5, point.position.getY(), point.position.getZ() + 0.5, Collections.emptySet(), player.getYRot(), player.getXRot(), false);
					player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
				}
				gui.close();
			});
		}
		gui.open();
	}

	private boolean warpPointExists(MinecraftServer server, Warppoint warp) {
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(server);
		ServerLevel world = server.getLevel(warp.world);
		if (world != null && world.getBlockState(warp.position).getBlock() instanceof SignBlock)
			return true;
		serverState.warppoints.remove(warp);
		return false;
	}

	private static SignText signText(DyeColor color, Component... lines) {
		SignText text = new SignText();
		for (int i = 0; i < lines.length; i++) {
			text = text.setMessage(i, lines[i]);
		}
		return text.setColor(color).setHasGlowingText(true);
	}

	private static void finishSign(SignBlockEntity sign, SignText front) {
		sign.setText(front, true);
		//clear back text
		sign.setText(new SignText(), false);
		sign.setWaxed(true);
	}

	public static boolean singChangeCancelled(SignText text, boolean front, SignBlockEntity sign) {
		if (!text.getMessage(0, false).getString().trim().equalsIgnoreCase("[warp]") || !front) {
			return false;
		}
		Level world = sign.getLevel();
		if (world == null || world.isClientSide()) {
			return false;
		}
		StateSaverAndLoader serverState = StateSaverAndLoader.getServerState(world.getServer());
		if(serverState.warppoints.size() >= 9*6+1){
			finishSign(sign, signText(DyeColor.RED,
					Component.literal("Warp"),
					Component.literal("§0Too many"),
					Component.literal("§0warp points")));
			return true;
		}
		String name = text.getMessage(1, false).getString().trim();
		if(name.isEmpty()){
			finishSign(sign, signText(DyeColor.RED,
					Component.literal("Warp"),
					Component.literal("§0Please provide"),
					Component.literal("§0a name")));
			return true;
		}
		// check if any warp point in DISTANCE
		for (Warppoint warppoint : serverState.warppoints) {
			if (!warppoint.world.equals(world.dimension())) {
				continue;
			}
			if (warppoint.position.closerThan(sign.getBlockPos(), DISTANCE)) {
				finishSign(sign, signText(DyeColor.RED,
						Component.literal("Warp"),
						Component.literal("§0Too close to"),
						Component.literal("§0another warp"),
						Component.literal("§0point")));
				return true;
			}
		}
		Item item = findAttachedBlock(sign).getBlock().asItem();
		if (item == Items.AIR) {
			LOGGER.info("No block attached to sign");
			return false;
		}
		finishSign(sign, signText(DyeColor.CYAN,
				Component.literal("Warp"),
				Component.literal("§4" + name)));
		serverState.warppoints.add(new Warppoint(world.dimension(), sign.getBlockPos(), Component.literal(name), item));
		return true;
	}

	public static BlockState findAttachedBlock(SignBlockEntity signBlockEntity) {
		Level world = signBlockEntity.getLevel();
		BlockPos pos = signBlockEntity.getBlockPos();

		if (world == null) {
			return Blocks.AIR.defaultBlockState(); // If the world reference is not available
		}

		BlockState signState = signBlockEntity.getBlockState();

		if (signState.getBlock() instanceof WallSignBlock) {
			// Attempt to access the facing direction of the sign
			Direction facing = null;
			if (signState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				facing = signState.getValue(BlockStateProperties.HORIZONTAL_FACING);
			} else if (signState.hasProperty(BlockStateProperties.FACING)) {
				facing = signState.getValue(BlockStateProperties.FACING);
			}

			if (facing != null) {
				BlockPos attachedBlockPos = pos.relative(facing.getOpposite());
				return world.getBlockState(attachedBlockPos);
			}
		} else {
			// For standing signs, check the block directly below
			BlockPos blockBelow = pos.below();
			return world.getBlockState(blockBelow);
		}

		return Blocks.AIR.defaultBlockState();
	}

}
