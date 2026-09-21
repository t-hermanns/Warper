package com.smallow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;


public class Warppoint {

    // Field names match the NBT layout written by older versions, so old save data can still be read.
    public static final Codec<Warppoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("World").forGetter(w -> w.world),
            Codec.INT.fieldOf("PosX").forGetter(w -> w.position.getX()),
            Codec.INT.fieldOf("PosY").forGetter(w -> w.position.getY()),
            Codec.INT.fieldOf("PosZ").forGetter(w -> w.position.getZ()),
            Codec.STRING.<Component>xmap(Component::literal, Component::getString).fieldOf("Name").forGetter(w -> w.name),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("Item").forGetter(w -> w.item),
            Codec.LONG.fieldOf("Timestamp").forGetter(w -> w.timestamp)
    ).apply(instance, (world, x, y, z, name, item, timestamp) ->
            new Warppoint(world, new BlockPos(x, y, z), name, item, timestamp)));

    public final ResourceKey<Level> world;
    public final BlockPos position;
    public final Component name;
    public final Item item;
    public final long timestamp;

    public Warppoint(ResourceKey<Level> world, BlockPos position, Component name, Item item) {
        this(world, position, name, item, System.currentTimeMillis() / 1000);
    }

    private Warppoint(ResourceKey<Level> world, BlockPos position, Component name, Item item, long timestamp) {
        this.world = world;
        this.position = position;
        this.name = name;
        this.item = item;
        this.timestamp = timestamp;
    }

    public boolean isInactive() {
        return System.currentTimeMillis() / 1000 - this.timestamp < Warper.WAIT_TIME;
    }
}
