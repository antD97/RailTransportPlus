/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.Config;
import com.antd.railtransportplus.RailTransportPlus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.FileWriter;
import java.io.IOException;

public class ServerWorldLoadListener implements ServerWorldEvents.Load {

    @Override
    public void onWorldLoad(MinecraftServer server, ServerWorld world) {

        final var worldPath = server.getSavePath(WorldSavePath.ROOT);
        final var worldConfigFile = worldPath.resolve("rail-transport-plus.properties").toFile();

        // create default
        if (!worldConfigFile.exists()) {
            Config.worldConfig = new Config();
            try (final var fw = new FileWriter(worldConfigFile)) {
                Config.worldConfig.createProperties()
                        .store(fw, "See https://github.com/antD97/RailTransportPlus/wiki for details");
                RailTransportPlus.LOGGER.info("Generated default config.");
            } catch (IOException e) {
                RailTransportPlus.LOGGER.error("Failed to write world config file", e);
                throw new RuntimeException();
            }
        }

        // load
        Config.worldConfig = Config.loadConfig(worldConfigFile);
    }
}
