/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import com.antd.railtransportplus.listener.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class RailTransportPlus implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("RailTransportPlus");

	/**
	 * server -> client: cart type info
	 * server <- client: cart type info request
	 */
	public static final Identifier CART_TYPE_PACKET_ID =
			new Identifier("railtransportplus", "cart-type-update");

	public static Config globalConfig = null;
	public static Config worldConfig = null;

	@Override
	public void onInitialize() {

		final var globalConfigFile = new File("config/rail-transport-plus.properties");

		// load global config
		try (final var fr = new FileReader(globalConfigFile)) {
			final var properties = new Properties();
			properties.load(fr);
			globalConfig = Config.loadConfig(properties);
		} catch (IOException ignored) {
		}

		// default global config
		if (globalConfig == null) {
			globalConfig = Config.DEFAULT;

			try (var fw = new FileWriter(globalConfigFile)) {

				RailTransportPlus.globalConfig.createProperties()
						.store(fw, "Default global config generated on:");

				LOGGER.info("Generated default global config.");

			} catch (IOException e) {
				RailTransportPlus.LOGGER.error("Failed to write global properties file.", e);
			}
		}

		// register listeners
		UseEntityCallback.EVENT.register(new UseEntityListener());
		ServerWorldEvents.LOAD.register(new ServerWorldLoadListener());
		ServerEntityEvents.ENTITY_LOAD.register(new ServerEntityLoadListener());
		ServerPlayNetworking.registerGlobalReceiver(CART_TYPE_PACKET_ID,
				new RequestCartTypePacketListener());

		ClientPlayNetworking.registerGlobalReceiver(CART_TYPE_PACKET_ID,
				new CartTypeUpdatePacketListener());
		ClientEntityEvents.ENTITY_LOAD.register(new ClientEntityLoadListener());
	}
}
