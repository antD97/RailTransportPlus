/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener;

import com.antd.railtransportplus.Config;
import com.antd.railtransportplus.RailTransportPlus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

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
            RailTransportPlus.worldConfig = Config.loadConfig(properties);
        } catch (IOException ignored) {
        }

        // copy global config
        if (RailTransportPlus.worldConfig == null) {
            try (var fw = new FileWriter(worldConfigFile)) {
                RailTransportPlus.globalConfig.createProperties()
                        .store(fw, "Copied from global config on:");
                RailTransportPlus.LOGGER.info("Copied global config to world.");
            } catch (IOException e) {
                RailTransportPlus.LOGGER.error("Failed to write world properties file.", e);
            }
        }
    }
}
