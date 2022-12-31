/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import java.util.Properties;

/** Rail Transport Plus configuration settings. */
public class Config {

    /** Default configuration settings. */
    public final static Config DEFAULT = new Config(40, 3, 4);

    /** Boosted furnace cart max speed. */
    public final double maxBoostedSpeed;
    /** Max number of carts a single furnace cart can pull. */
    public final int maxCartsPerFurnaceCart;
    /** Max number of furnace carts in a train. */
    public final int maxFurnaceCartsPerTrain;

    private Config(
            double maxBoostedSpeed,
            int maxCartsPerFurnaceCart,
            int maxFurnaceCartsPerTrain
    ) {
        this.maxBoostedSpeed = maxBoostedSpeed;
        this.maxCartsPerFurnaceCart = maxCartsPerFurnaceCart;
        this.maxFurnaceCartsPerTrain = maxFurnaceCartsPerTrain;
    }

    /** Creates properties from this config. */
    public Properties createProperties() {
        final var properties = new Properties();

        properties.setProperty("maxBoostedSpeed", maxBoostedSpeed + "");
        properties.setProperty("maxCartsPerFurnaceCart", maxCartsPerFurnaceCart + "");
        properties.setProperty("maxFurnaceCartsPerTrain", maxFurnaceCartsPerTrain + "");

        return properties;
    }

    /** Loads config from a properties. Returns null if  */
    public static Config loadConfig(Properties properties) {
        try {
            return new Config(
                    Double.parseDouble(properties.getProperty("maxBoostedSpeed")),
                    Integer.parseInt(properties.getProperty("maxCartsPerFurnaceCart")),
                    Integer.parseInt(properties.getProperty("maxFurnaceCartsPerTrain"))
            );
        } catch (NullPointerException | NumberFormatException ignored) {
            return null;
        }
    }
}
