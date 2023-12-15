/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Rail Transport Plus configuration settings. */
public class Config {

    public static Config worldConfig = null;

    private Map<DoubleEntry, Double> doubles;
    private Map<IntEntry, Integer> ints;
    private Map<StringEntry, String> strings;

    private static Set<ConfigEntry> configEntries;

    public static final DoubleEntry MAX_BOOSTED_SPEED = new DoubleEntry(10, Double.MAX_VALUE, 65, "maxBoostedSpeed");
    public static final DoubleEntry TIME_TO_MAX_BOOST = new DoubleEntry(0, Double.MAX_VALUE, 10, "timeToMaxBoost");
    public static final DoubleEntry STANDARD_RAIL_TIME_TO_NO_BOOST = new DoubleEntry(0, Double.MAX_VALUE, 7.5,
            "standardRailTimeToNoBoost");
    public static final DoubleEntry UNPOWERED_RAIL_TIME_TO_NO_BOOST = new DoubleEntry(0, Double.MAX_VALUE, 3.33,
            "unpoweredRailTimeToNoBoost");
    public static final DoubleEntry OFF_RAIL_TIME_TO_NO_BOOST = new DoubleEntry(0, Double.MAX_VALUE, 3.33,
            "offRailTimeToNoBoost");

    public static final IntEntry MAX_CARTS_PER_FURNACE_CART = new IntEntry(0, Integer.MAX_VALUE, 3,
            "maxCartsPerFurnaceCart");
    public static final IntEntry MAX_FURNACE_CARTS_PER_TRAIN = new IntEntry(0, Integer.MAX_VALUE, 4,
            "maxFurnaceCartsPerTrain");

    public static final String TRAIN_CRASH_MODE_NOTHING = "nothing";
    public static final String TRAIN_CRASH_MODE_BREAK = "break";
    public static final String TRAIN_CRASH_MODE_EXPLODE = "explode";
    public static final StringEntry TRAIN_CRASH_MODE = new StringEntry(
            List.of(TRAIN_CRASH_MODE_NOTHING,
                    TRAIN_CRASH_MODE_BREAK,
                    TRAIN_CRASH_MODE_EXPLODE),
            TRAIN_CRASH_MODE_BREAK,
            "trainCrashMode");

    static {
        configEntries = ImmutableSet.<ConfigEntry>builder()
                .add(MAX_BOOSTED_SPEED)
                .add(TIME_TO_MAX_BOOST)
                .add(STANDARD_RAIL_TIME_TO_NO_BOOST)
                .add(UNPOWERED_RAIL_TIME_TO_NO_BOOST)
                .add(OFF_RAIL_TIME_TO_NO_BOOST)
                .add(MAX_CARTS_PER_FURNACE_CART)
                .add(MAX_FURNACE_CARTS_PER_TRAIN)
                .add(TRAIN_CRASH_MODE)
                .build();
    }

    // defaults
    public Config() {
        final var doublesBuilder = ImmutableMap.<DoubleEntry, Double>builder();
        final var intsBuilder = ImmutableMap.<IntEntry, Integer>builder();
        final var stringsBuilder = ImmutableMap.<StringEntry, String>builder();

        for (final var entry : configEntries) {
            if (entry instanceof final DoubleEntry doubleEntry) {
                doublesBuilder.put(doubleEntry, doubleEntry.def);
            } else if (entry instanceof final IntEntry intEntry) {
                intsBuilder.put(intEntry, intEntry.def);
            } else if (entry instanceof final StringEntry stringEntry) {
                stringsBuilder.put(stringEntry, stringEntry.def);
            }
        }

        this.doubles = doublesBuilder.build();
        this.ints = intsBuilder.build();
        this.strings = stringsBuilder.build();

        checkHasAllEntries();
    }

    private Config(Map<DoubleEntry, Double> doubles, Map<IntEntry, Integer> ints, Map<StringEntry, String> strings) {
        this.doubles = doubles;
        this.ints = ints;
        this.strings = strings;

        checkHasAllEntries();
    }

    public double get(DoubleEntry entry) {
        return doubles.get(entry);
    }

    public int get(IntEntry entry) {
        return ints.get(entry);
    }

    public String get(StringEntry entry) {
        return strings.get(entry);
    }

    public Properties createProperties() {
        final var properties = new Properties();

        for (final var key : doubles.keySet()) properties.setProperty(key.propName, doubles.get(key) + "");
        for (final var key : ints.keySet()) properties.setProperty(key.propName, ints.get(key) + "");
        for (final var key : strings.keySet()) properties.setProperty(key.propName, strings.get(key) + "");

        return properties;
    }

    public static Config loadConfig(File file) {
        final var properties = new Properties();
        try (var fr = new FileReader(file)) {
            properties.load(fr);
        } catch (IOException e) {
            RailTransportPlus.LOGGER.error("Failed to load world config file", e);
            throw new RuntimeException();
        }

        final var doubles = ImmutableMap.<DoubleEntry, Double>builder();
        final var ints = ImmutableMap.<IntEntry, Integer>builder();
        final var strings = ImmutableMap.<StringEntry, String>builder();

        boolean rewriteConfig = false;
        for (final var entry : configEntries) {
            final var prop = properties.getProperty(entry.getPropName());

            if (prop != null) {
                Object value = null;

                try {
                    // load double entry
                    if (entry instanceof DoubleEntry doubleEntry) {
                        value = Double.parseDouble(prop);
                        if ((double) value < doubleEntry.min || (double) value > doubleEntry.max) {
                            throw new IllegalArgumentException();
                        }
                        doubles.put(doubleEntry, (double) value);
                    }

                    // load int entry
                    else if (entry instanceof IntEntry intEntry) {
                        value = Integer.parseInt(prop);
                        if ((int) value < intEntry.min || (int) value > intEntry.max) {
                            throw new IllegalArgumentException();
                        }
                        ints.put(intEntry, (int) value);
                    }

                    // load string entry
                    else if (entry instanceof StringEntry stringEntry) {
                        value = prop;
                        if (!stringEntry.acceptedValues.contains((String) value)) {
                            throw new IllegalArgumentException();
                        }
                        strings.put(stringEntry, (String) value);
                    }

                    // unexpected config entry type
                    else {
                        RailTransportPlus.LOGGER.error("Unexpected config entry type");
                        throw new RuntimeException();
                    }

                }
                // failed to parse number string
                catch (NumberFormatException e) {
                    RailTransportPlus.LOGGER.warn("Unable to load config entry \"" + entry.getPropName()
                    + "\"; using default value: " + entry.getDefault());

                    loadDefault(entry, doubles, ints, strings);
                }
                // illegal config entry value
                catch (IllegalArgumentException e) {
                    RailTransportPlus.LOGGER.warn("Illegal config value for entry \"" + entry.getPropName()
                            + "\"; using default value: " + entry.getDefault());

                    loadDefault(entry, doubles, ints, strings);
                }
            }
            // property not found
            else {
                RailTransportPlus.LOGGER.warn("Config entry \"" + entry.getPropName()
                        + "\" not found; writing default value to file: " + entry.getDefault());

                properties.setProperty(entry.getPropName(), entry.getDefault());
                loadDefault(entry, doubles, ints, strings);
                rewriteConfig = true;
            }
        }

        // rewrite the config with the missing properties
        if (rewriteConfig) {
            try (final var fw = new FileWriter(file)) {
                properties.store(fw, "See https://github.com/antD97/RailTransportPlus/wiki for details");
            } catch(IOException e) {
                RailTransportPlus.LOGGER.error("Failed to write world config file", e);
                throw new RuntimeException();
            }
        }

        return new Config(doubles.build(), ints.build(), strings.build());
    }

    private void checkHasAllEntries() {
        if (this.doubles.size() + this.ints.size() + this.strings.size() != configEntries.size()) {
            final var missingEntries = configEntries.stream()
                    .filter(entry -> !this.doubles.containsKey(entry))
                    .filter(entry -> !this.ints.containsKey(entry))
                    .filter(entry -> !this.strings.containsKey(entry))
                    .map(ConfigEntry::getPropName)
                    .toList();

            RailTransportPlus.LOGGER.error("Missing config entries: " + String.join(", ", missingEntries));
            throw new RuntimeException();
        }
    }

    private static void loadDefault(ConfigEntry entry, ImmutableMap.Builder<DoubleEntry, Double> doubles,
                                    ImmutableMap.Builder<IntEntry, Integer> ints,
                                    ImmutableMap.Builder<StringEntry, String> strings) {

        if (entry instanceof DoubleEntry) doubles.put((DoubleEntry) entry, Double.parseDouble(entry.getDefault()));
        else if (entry instanceof IntEntry) ints.put((IntEntry) entry, Integer.parseInt(entry.getDefault()));
        else if (entry instanceof StringEntry) strings.put((StringEntry) entry, entry.getDefault());
        // unexpected entry type
        else {
            RailTransportPlus.LOGGER.error("Unexpected config entry type");
            throw new RuntimeException();
        }
    }

    private interface ConfigEntry {
        String getPropName();
        String getDefault();
    }

    private record DoubleEntry(double min, double max, double def, String propName) implements ConfigEntry {
        @Override
        public String getPropName() {
            return propName;
        }
        @Override
        public String getDefault() {
            return def + "";
        }
    }

    private record IntEntry(int min, int max, int def, String propName) implements ConfigEntry {
        @Override
        public String getPropName() {
            return propName;
        }
        @Override
        public String getDefault() {
            return def + "";
        }
    }

    private record StringEntry(List<String> acceptedValues, String def, String propName) implements ConfigEntry {
        @Override
        public String getPropName() { return propName; }
        @Override
        public String getDefault() {
            return def;
        }
    }
}
