package io.github.derexxd.simpleRevive.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    public static int downedTime;
    public static double revivingTime;
    public static int cooldown;
    public static int hunger;
    public static boolean block;
    public static boolean entity;
    public static boolean glow;
    public static boolean keepinv;
    public static boolean crawl;
    public static boolean numeric;

    public static void load(FileConfiguration config) {
        downedTime = config.getInt("downed_time", 45);
        revivingTime = config.getDouble("reviving_time", 4.5);
        cooldown = config.getInt("cooldown", 1);
        hunger = config.getInt("hunger", 6);
        block = config.getBoolean("block", true);
        entity = config.getBoolean("entity", true);
        glow = config.getBoolean("glow", false);
        keepinv = config.getBoolean("keepinv", true);
        crawl = config.getBoolean("crawl", true);
        numeric = config.getBoolean("numeric", false);
    }
}
