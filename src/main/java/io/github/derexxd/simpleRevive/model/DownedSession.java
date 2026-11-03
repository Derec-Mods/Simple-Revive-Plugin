package io.github.derexxd.simpleRevive.model;

import io.github.derexxd.simpleRevive.config.PluginConfig;
import io.github.derexxd.simpleRevive.manager.DownedManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class DownedSession {
    private final UUID playerId;
    private final TextDisplay display;
    private int downedTicksRemaining;
    private int revivingTicks;

    public DownedSession(Player player) {
        this.playerId = player.getUniqueId();
        this.downedTicksRemaining = PluginConfig.downedTime * 10;
        this.revivingTicks = 0;

        this.display = player.getWorld().spawn(player.getLocation().add(0, 1.2, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setViewRange(32.0f);
            d.setShadowed(true);
        });

        applyEffects(player);
        updateDisplay(false);
    }

    public void tick() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            DownedManager.removeDowned(playerId);
            cleanup(player);
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            DownedManager.removeDowned(playerId);
            cleanup(player);
            return;
        }

        if (PluginConfig.crawl) {
            player.setSwimming(true);
        }

        Player reviver = findNearestReviver(player);
        boolean beingRevived = reviver != null;

        if (beingRevived) {
            revivingTicks += 2;
        } else {
            if (revivingTicks > 0) {
                revivingTicks--;
            }
            downedTicksRemaining--;
        }

        display.teleport(player.getLocation().add(0, 1.2, 0));
        updateDisplay(beingRevived);

        int targetReviveTicks = (int) (PluginConfig.revivingTime * 10);
        if (revivingTicks >= targetReviveTicks) {
            revive(player, reviver);
        } else if (downedTicksRemaining <= 0) {
            fail(player);
        }
    }

    private void updateDisplay(boolean beingRevived) {
        Component text;
        if (beingRevived) {
            double prog = revivingTicks / 10.0;
            text = Component.text("❤ Reviving: " + String.format("%.1f", prog) + "s", NamedTextColor.GREEN);
        } else {
            double remaining = downedTicksRemaining / 10.0;
            text = Component.text("☠ Bleeding out: " + String.format("%.1f", remaining) + "s", NamedTextColor.RED);
        }
        display.text(text);
    }

    private void revive(Player player, Player reviver) {
        cleanup(player);
        DownedManager.removeDowned(playerId);
        DownedManager.setCooldown(playerId);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 0.5, 0), 15, 0, 0, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        if (reviver != null && PluginConfig.hunger > 0) {
            reviver.setFoodLevel(Math.max(0, reviver.getFoodLevel() - PluginConfig.hunger));
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + PluginConfig.hunger));
        }

        Bukkit.broadcast(Component.text(player.getName() + " was revived by " + (reviver != null ? reviver.getName() : "a teammate"), NamedTextColor.GRAY));
    }

    private void fail(Player player) {
        cleanup(player);
        DownedManager.removeDowned(playerId);
        Bukkit.broadcast(Component.text(player.getName() + " could not be revived", NamedTextColor.GRAY));
        player.setHealth(0.0);
    }

    public void cleanup(Player player) {
        if (display != null && display.isValid()) {
            display.remove();
        }
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.GLOWING);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.setWalkSpeed(0.2f);
            player.setSwimming(false);
        }
    }

    private void applyEffects(Player player) {
        if (PluginConfig.glow) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 200, false, false));
        player.setWalkSpeed(0.06f);
    }

    private Player findNearestReviver(Player downed) {
        Player closest = null;
        double closestDist = 9.0;
        for (Player p : downed.getWorld().getPlayers()) {
            if (p.equals(downed)) {
                continue;
            }
            if (p.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (DownedManager.isDowned(p.getUniqueId())) {
                continue;
            }
            if (!p.isSneaking()) {
                continue;
            }
            if (p.getFoodLevel() < PluginConfig.hunger) {
                continue;
            }
            double dist = p.getLocation().distanceSquared(downed.getLocation());
            if (dist <= closestDist) {
                closest = p;
                closestDist = dist;
            }
        }
        return closest;
    }
}

