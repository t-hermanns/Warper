package com.smallow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public class StateSaverAndLoader extends SavedData {
    public List<Warppoint> warppoints = new ArrayList<>();

    private static final Codec<StateSaverAndLoader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Warppoint.CODEC.listOf().fieldOf("warppoints").forGetter(state -> state.warppoints)
    ).apply(instance, StateSaverAndLoader::new));

    // Stored in <world>/data/warper.dat, the same file and layout older versions used.
    // SAVED_DATA_COMMAND_STORAGE has no datafixers, so our data is passed through unchanged.
    private static final SavedDataType<StateSaverAndLoader> TYPE = new SavedDataType<>(
            Warper.MOD_ID,
            StateSaverAndLoader::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public StateSaverAndLoader() {
    }

    private StateSaverAndLoader(List<Warppoint> warppoints) {
        this.warppoints = new ArrayList<>(warppoints);
    }

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        StateSaverAndLoader state = server.overworld().getDataStorage().computeIfAbsent(TYPE);

        // Always mark dirty so changes to the warppoints list are saved without having to track them.
        state.setDirty();

        return state;
    }
}
