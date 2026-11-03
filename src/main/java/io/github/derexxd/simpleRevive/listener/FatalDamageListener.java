package io.github.derexxd.simpleRevive.listener;

import io.github.derexxd.simpleRevive.manager.DownedManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FatalDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getCause() == EntityDamageEvent.DamageCause.SUICIDE) {
            return;
        }

        if (hasTotem(player)) {
            return;
        }

        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (DownedManager.isDowned(player.getUniqueId())) {
                return;
            }

            if (!DownedManager.canBeDowned(player)) {
                return;
            }

            event.setDamage(0);
            player.setHealth(1.0);
            DownedManager.startDowned(player);
        }
    }

    private static boolean hasTotem(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
}

