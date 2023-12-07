/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import java.util.Properties;

/** Rail Transport Plus configuration settings. */
public class Config {

    /** Boosted furnace cart max speed. */
    public final double maxBoostedSpeed;
    /** Time it takes for a fueled furnace minecart on a powered rail to reach maximum boost (in seconds). */
    public final double timeToMaxBoost;
    /**
     * Time it takes for a max boost furnace minecart to reach zero boost while on a standard minecart rail (in
     * seconds).
     */
    public final double standardRailTimeToNoBoost;
    /**
     * Time it takes for a max boost furnace minecart to reach zero boost while on an unpowered minecart rail (in
     * seconds).
     */
    public final double unpoweredRailTimeToNoBoost;

    /** Max number of carts a single furnace cart can pull. */
    public final int maxCartsPerFurnaceCart;
    /** Max number of furnace carts in a train. */
    public final int maxFurnaceCartsPerTrain;

    public Config() {
        this.maxBoostedSpeed = 65;
        this.timeToMaxBoost = 10;
        this.standardRailTimeToNoBoost = 7.5;
        this.unpoweredRailTimeToNoBoost = 3.33;
        this.maxCartsPerFurnaceCart = 3;
        this.maxFurnaceCartsPerTrain = 4;
    }

    private Config(
            double maxBoostedSpeed,
            double timeToMaxBoost,
            double standardRailTimeToNoBoost,
            double unpoweredRailTimeToNoBoost,
            int maxCartsPerFurnaceCart,
            int maxFurnaceCartsPerTrain
    ) {
        this.maxBoostedSpeed = maxBoostedSpeed;
        this.timeToMaxBoost = timeToMaxBoost;
        this.standardRailTimeToNoBoost = standardRailTimeToNoBoost;
        this.unpoweredRailTimeToNoBoost = unpoweredRailTimeToNoBoost;
        this.maxCartsPerFurnaceCart = maxCartsPerFurnaceCart;
        this.maxFurnaceCartsPerTrain = maxFurnaceCartsPerTrain;
    }

    /** Creates properties from this config. */
    public Properties createProperties() {
        final var properties = new Properties();

        properties.setProperty("maxBoostedSpeed", maxBoostedSpeed + "");
        properties.setProperty("timeToMaxBoost", timeToMaxBoost + "");
        properties.setProperty("standardRailTimeToNoBoost", standardRailTimeToNoBoost + "");
        properties.setProperty("unpoweredRailTimeToNoBoost", unpoweredRailTimeToNoBoost + "");
        properties.setProperty("maxCartsPerFurnaceCart", maxCartsPerFurnaceCart + "");
        properties.setProperty("maxFurnaceCartsPerTrain", maxFurnaceCartsPerTrain + "");

        return properties;
    }

    /** Loads config from a properties. Returns null if  */
    public static Config loadConfig(Properties properties) {
        try {
            return new Config(
                    Double.parseDouble(properties.getProperty("maxBoostedSpeed")),
                    Double.parseDouble(properties.getProperty("timeToMaxBoost")),
                    Double.parseDouble(properties.getProperty("standardRailTimeToNoBoost")),
                    Double.parseDouble(properties.getProperty("unpoweredRailTimeToNoBoost")),
                    Integer.parseInt(properties.getProperty("maxCartsPerFurnaceCart")),
                    Integer.parseInt(properties.getProperty("maxFurnaceCartsPerTrain"))
            );
        } catch (NullPointerException | NumberFormatException ignored) {
            return null;
        }
    }
}
