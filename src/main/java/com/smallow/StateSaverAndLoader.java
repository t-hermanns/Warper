package com.smallow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StateSaverAndLoader extends SavedData {
    public List<Warppoint> warppoints = new ArrayList<>();

    private static final Codec<StateSaverAndLoader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Warppoint.CODEC.listOf().fieldOf("warppoints").forGetter(state -> state.warppoints)
    ).apply(instance, StateSaverAndLoader::new));

    // Stored in <world>/data/warper/warppoints.dat.
    // SAVED_DATA_COMMAND_STORAGE has no datafixers, so our data is passed through unchanged.
    private static final SavedDataType<StateSaverAndLoader> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Warper.MOD_ID, "warppoints"),
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
        SavedDataStorage storage = server.getDataStorage();
        StateSaverAndLoader state = storage.get(TYPE);
        if (state == null) {
            state = migrateLegacyState(server, storage);
            storage.set(TYPE, state);
        }

        // Always mark dirty so changes to the warppoints list are saved without having to track them.
        state.setDirty();

        return state;
    }

    /**
     * Versions for Minecraft 1.20.x stored the warp points in <world>/data/warper.dat.
     * Load them from there once, so existing warp points survive the update.
     */
    private static StateSaverAndLoader migrateLegacyState(MinecraftServer server, SavedDataStorage storage) {
        Path legacyFile = server.getWorldPath(LevelResource.DATA).resolve(Warper.MOD_ID + ".dat");
        if (!Files.exists(legacyFile)) {
            return new StateSaverAndLoader();
        }
        try {
            CompoundTag tag = storage.readTagFromDisk(legacyFile, DataFixTypes.SAVED_DATA_COMMAND_STORAGE,
                    SharedConstants.getCurrentVersion().dataVersion().version());
            StateSaverAndLoader state = CODEC.parse(NbtOps.INSTANCE, tag.get("data")).getOrThrow();
            Files.move(legacyFile, legacyFile.resolveSibling(Warper.MOD_ID + ".dat.migrated"));
            Warper.LOGGER.info("Migrated {} warp points from {}", state.warppoints.size(), legacyFile);
            return state;
        } catch (Exception e) {
            Warper.LOGGER.error("Failed to migrate warp points from {}", legacyFile, e);
            return new StateSaverAndLoader();
        }
    }
}
