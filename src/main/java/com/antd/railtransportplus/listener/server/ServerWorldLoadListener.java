/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import static com.antd.railtransportplus.RailTransportPlus.*;

public class ServerWorldLoadListener implements ServerWorldEvents.Load {

    @Override
    public void onWorldLoad(MinecraftServer server, ServerWorld world) {

        final var worldPath = server.getSavePath(WorldSavePath.ROOT);
        final var worldConfigFile =
                worldPath.resolve("config/rail-transport-plus.properties").toFile();

        // ensure config directory exists
        worldPath.resolve("config").toFile().mkdir();

        // load world config
        try (var fr = new FileReader(worldConfigFile)) {
            final var properties = new Properties();
            properties.load(fr);
            worldConfig = Config.loadConfig(properties);
        } catch (IOException ignored) {
        }

        // copy global config
        if (worldConfig == null) {
            try (var fw = new FileWriter(worldConfigFile)) {
                globalConfig.createProperties()
                        .store(fw, "Copied from global config on:");
                worldConfig = globalConfig;
                LOGGER.info("Copied global config to world.");
            } catch (IOException e) {
                LOGGER.error("Failed to write world properties file.", e);
            }
        }
    }
}
