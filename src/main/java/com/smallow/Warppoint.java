package com.smallow;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;


public class Warppoint {

    public final RegistryKey<World> world;
    public final BlockPos position;
    public final Text name;
    public final Item item;
    public final long timestamp;

    public Warppoint(RegistryKey<World> world, BlockPos position, Text name, Item item) {
        this.world = world;
        this.position = position;
        this.name = name;
        this.item = item;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public boolean isInactive() {
        return System.currentTimeMillis() / 1000 - this.timestamp < Warper.WAIT_TIME;
    }

    private Warppoint(RegistryKey<World> world, BlockPos position, Text name, Item item, long timestamp) {
        this.world = world;
        this.position = position;
        this.name = name;
        this.item = item;
        this.timestamp = timestamp;
    }

    public static NbtElement writeNbt(List<Warppoint> warppoints) {
        NbtList list = new NbtList();
        for (Warppoint warppoint : warppoints) {
            NbtCompound nbt = new NbtCompound();

            // Serialize world
            nbt.putString("World", warppoint.world.getValue().toString());

            // Serialize position
            nbt.putInt("PosX", warppoint.position.getX());
            nbt.putInt("PosY", warppoint.position.getY());
            nbt.putInt("PosZ", warppoint.position.getZ());

            // Serialize name
            nbt.putString("Name", warppoint.name.getString());

            // Serialize item by its registry name
            Identifier itemId = Registries.ITEM.getId(warppoint.item);
            nbt.putString("Item", itemId.toString());

            // Serialize timestamp
            nbt.putLong("Timestamp", warppoint.timestamp);

            // Add this warppoint's NBT to the list
            list.add(nbt);
        }
        return list;
    }

    public static List<Warppoint> readNbt(NbtList warppoints) {
        List<Warppoint> list = new ArrayList<>();
        for (NbtElement nbt : warppoints) {
            NbtCompound compound = (NbtCompound) nbt;

            // Deserialize world
            RegistryKey<World> world = RegistryKey.of(RegistryKeys.WORLD, new Identifier(compound.getString("World")));

            // Deserialize position
            BlockPos position = new BlockPos(
                    compound.getInt("PosX"),
                    compound.getInt("PosY"),
                    compound.getInt("PosZ")
            );

            // Deserialize name
            Text name = Text.of(compound.getString("Name"));

            // Deserialize item by its registry name
            Item item = Registries.ITEM.get(new Identifier(compound.getString("Item")));

            // Deserialize timestamp
            long timestamp = compound.getLong("Timestamp");

            // Add this warppoint to the list
            list.add(new Warppoint(world, position, name, item, timestamp));
        }
        return list;
    }
}
