/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import com.antd.railtransportplus.listener.server.RequestCartTypePacketListener;
import com.antd.railtransportplus.listener.server.ServerEntityLoadListener;
import com.antd.railtransportplus.listener.server.ServerWorldLoadListener;
import com.antd.railtransportplus.listener.server.UseEntityListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entrypoint. */
public class RailTransportPlus implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("RailTransportPlus");

	/**
	 * server -> client: cart visual state update
	 * server <- client: cart visual state request
	 */
	public static final Identifier CART_VISUAL_STATE_PACKET_ID =
			new Identifier("railtransportplus", "cart-visual-state");

	public static final RegistryKey<DamageType> TRAIN_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
			new Identifier("railtransportplus", "train_damage"));

	@Override
	public void onInitialize()  {
		UseEntityCallback.EVENT.register(new UseEntityListener());
		ServerWorldEvents.LOAD.register(new ServerWorldLoadListener());
		ServerEntityEvents.ENTITY_LOAD.register(new ServerEntityLoadListener());
		ServerPlayNetworking.registerGlobalReceiver(CART_VISUAL_STATE_PACKET_ID, new RequestCartTypePacketListener());
	}
}
