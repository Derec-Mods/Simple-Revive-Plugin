package io.github.derexxd.simpleRevive.manager;

import io.github.derexxd.simpleRevive.config.PluginConfig;
import io.github.derexxd.simpleRevive.model.DownedSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DownedManager {
    private static final Map<UUID, DownedSession> sessions = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static boolean isDowned(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public static boolean canBeDowned(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return false;
        }
        if (isDowned(player.getUniqueId())) {
            return false;
        }

        Long expireTime = cooldowns.get(player.getUniqueId());
        if (expireTime != null && System.currentTimeMillis() < expireTime) {
            player.sendMessage(Component.text("You died too quickly to get Downed again", NamedTextColor.DARK_GRAY));
            return false;
        }

        return true;
    }

    public static void startDowned(Player player) {
        if (!PluginConfig.keepinv && !Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY))) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
        }
        sessions.put(player.getUniqueId(), new DownedSession(player));
        Bukkit.broadcast(Component.text(player.getName() + " has been downed", NamedTextColor.GRAY));
    }

    public static void removeDowned(UUID uuid) {
        DownedSession session = sessions.remove(uuid);
        if (session != null) {
            Player player = Bukkit.getPlayer(uuid);
            session.cleanup(player);
        }
    }

    public static void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis() + (long) (PluginConfig.cooldown * 1000L));
    }

    public static void tickAll() {
        if (sessions.isEmpty()) {
            return;
        }
        for (DownedSession session : sessions.values().toArray(new DownedSession[0])) {
            session.tick();
        }
    }

    public static void cleanupAll() {
        for (Map.Entry<UUID, DownedSession> entry : sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            entry.getValue().cleanup(player);
        }
        sessions.clear();
        cooldowns.clear();
    }
}
