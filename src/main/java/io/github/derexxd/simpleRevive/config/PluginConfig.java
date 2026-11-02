package io.github.derexxd.simpleRevive.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PluginConfig {
    public static double durationSeconds;
    public static double reviveDurationSeconds;
    public static double cooldownSeconds;
    public static int hungerCost;
    public static double reviveRadius;
    public static double speedMultiplier;
    public static boolean enableCrawling;
    public static boolean restrictBlockInteraction;
    public static boolean restrictEntityInteraction;
    public static boolean glowing;

    public static String displayMode;
    public static String numericFormat;
    public static double heightOffset;
    public static List<Float> revivePitches;

    public static boolean enableGiveUp;
    public static double giveUpHoldSeconds;
    public static boolean killOnDisconnect;
    public static boolean requireNearbyTeammates;

    private static FileConfiguration config;

    public static void load(FileConfiguration fileConfig) {
        config = fileConfig;
        durationSeconds = config.getDouble("downed.duration_seconds", 45.0);
        reviveDurationSeconds = config.getDouble("downed.revive_duration_seconds", 4.5);
        cooldownSeconds = config.getDouble("downed.cooldown_seconds", 1.0);
        hungerCost = config.getInt("downed.hunger_cost", 6);
        reviveRadius = config.getDouble("downed.revive_radius", 3.0);
        speedMultiplier = config.getDouble("downed.speed_multiplier", 0.3);
        enableCrawling = config.getBoolean("downed.enable_crawling", true);
        restrictBlockInteraction = config.getBoolean("downed.restrict_block_interaction", true);
        restrictEntityInteraction = config.getBoolean("downed.restrict_entity_interaction", true);
        glowing = config.getBoolean("downed.glowing", false);

        displayMode = config.getString("display.mode", "ICONS");
        numericFormat = config.getString("display.numeric_format", "<red>{time_remaining}s</red> <green>{revive_progress}s</green>");
        heightOffset = config.getDouble("display.height_offset", 1.2);
        revivePitches = config.getFloatList("display.revive_pitches");
        if (revivePitches.isEmpty()) {
            revivePitches = List.of(0.80f, 0.95f, 1.10f, 1.25f, 1.40f, 1.55f, 1.70f, 1.85f, 2.00f);
        }

        enableGiveUp = config.getBoolean("mechanics.enable_give_up", true);
        giveUpHoldSeconds = config.getDouble("mechanics.give_up_hold_seconds", 0.8);
        killOnDisconnect = config.getBoolean("mechanics.kill_on_disconnect", true);
        requireNearbyTeammates = config.getBoolean("mechanics.require_nearby_teammates", true);
    }

    public static Component getMessage(String key) {
        if (config == null) {
            return Component.empty();
        }
        String prefix = config.getString("messages.prefix", "");
        String raw = config.getString("messages." + key, "");
        return MiniMessage.miniMessage().deserialize(prefix + raw);
    }

    public static Component getRawMessage(String key) {
        if (config == null) {
            return Component.empty();
        }
        String raw = config.getString("messages." + key, "");
        return MiniMessage.miniMessage().deserialize(raw);
    }
}

