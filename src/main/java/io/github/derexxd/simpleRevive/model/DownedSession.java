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
    private static final float[] REVIVE_PITCHES = {0.80f, 0.95f, 1.10f, 1.25f, 1.40f, 1.55f, 1.70f, 1.85f, 2.00f};
    private static final PotionEffectType JUMP_BOOST_EFFECT = PotionEffectType.getByName("JUMP_BOOST") != null
            ? PotionEffectType.getByName("JUMP_BOOST")
            : PotionEffectType.getByName("JUMP");
    private static final Particle TOTEM_PARTICLE;

    static {
        Particle particle;
        try {
            particle = Particle.valueOf("TOTEM_OF_UNDYING");
        } catch (IllegalArgumentException e) {
            particle = Particle.valueOf("TOTEM");
        }
        TOTEM_PARTICLE = particle;
    }

    private final UUID playerId;
    private final TextDisplay display;
    private int downedTicksRemaining;
    private int revivingTicks;
    private int giveUpCounter;
    private int lastRedSegment = 0;
    private int lastGreenSegment = 0;

    public DownedSession(Player player) {
        this.playerId = player.getUniqueId();
        this.downedTicksRemaining = PluginConfig.downedTime * 10;
        this.revivingTicks = 0;
        this.giveUpCounter = 0;

        this.display = player.getWorld().spawn(player.getLocation().add(0, 1.2, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setViewRange(32.0f);
            d.setShadowed(true);
            d.setPersistent(false);
        });

        applyEffects(player);
        player.sendActionBar(Component.text("Look Down and Hold Sneak to Give Up", NamedTextColor.WHITE));
        updateDisplay(player, false);
    }

    public void tick() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline() || player.isDead()) {
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

        if (player.getLocation().getPitch() >= 89.0f && player.isSneaking()) {
            giveUpCounter += 2;
            if (giveUpCounter >= 8) {
                fail(player);
                return;
            }
        } else {
            if (giveUpCounter > 0) {
                giveUpCounter--;
            }
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
        updateDisplay(player, beingRevived);

        int targetReviveTicks = (int) (PluginConfig.revivingTime * 10);
        if (revivingTicks >= targetReviveTicks) {
            revive(player, reviver);
        } else if (downedTicksRemaining <= 0) {
            fail(player);
        }
    }

    private void updateDisplay(Player player, boolean beingRevived) {
        if (PluginConfig.numeric) {
            double remainingSeconds = downedTicksRemaining / 10.0;
            double progressSeconds = revivingTicks / 10.0;
            Component text = Component.text(String.format("%.1fs ", remainingSeconds), NamedTextColor.RED)
                    .append(Component.text(String.format("%.1fs", progressSeconds), NamedTextColor.GREEN));
            display.text(text);
            return;
        }

        int totalDowned = PluginConfig.downedTime * 10;
        int totalRevive = (int) (PluginConfig.revivingTime * 10);

        int redSkulls = 10 - (int) (((double) downedTicksRemaining / totalDowned) * 10);
        if (redSkulls < 0) {
            redSkulls = 0;
        } else if (redSkulls > 10) {
            redSkulls = 10;
        }

        int greenHearts = (int) (((double) revivingTicks / totalRevive) * 10);
        if (greenHearts < 0) {
            greenHearts = 0;
        } else if (greenHearts > 10) {
            greenHearts = 10;
        }

        if (redSkulls > lastRedSegment) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0f, 1.7f);
            lastRedSegment = redSkulls;
        }

        if (greenHearts > lastGreenSegment && greenHearts >= 1 && greenHearts <= 9) {
            float pitch = REVIVE_PITCHES[greenHearts - 1];
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, pitch);
            lastGreenSegment = greenHearts;
        }

        Component bar = Component.text("[", beingRevived ? NamedTextColor.GREEN : NamedTextColor.RED);

        for (int i = 0; i < greenHearts; i++) {
            bar = bar.append(Component.text("\u2764", NamedTextColor.GREEN));
        }

        int activeSkulls = redSkulls - greenHearts;
        if (activeSkulls > 0) {
            for (int i = 0; i < activeSkulls; i++) {
                bar = bar.append(Component.text("\u2620", beingRevived ? NamedTextColor.DARK_GRAY : NamedTextColor.RED));
            }
        }

        int clocks = 10 - Math.max(redSkulls, greenHearts);
        for (int i = 0; i < clocks; i++) {
            bar = bar.append(Component.text("\u231A", NamedTextColor.GRAY));
        }

        bar = bar.append(Component.text("]", beingRevived ? NamedTextColor.GREEN : NamedTextColor.RED));
        display.text(bar);
    }

    private void revive(Player player, Player reviver) {
        cleanup(player);
        DownedManager.removeDowned(playerId);
        DownedManager.setCooldown(playerId);

        player.getWorld().spawnParticle(TOTEM_PARTICLE, player.getLocation().add(0, 0.5, 0), 15, 0, 0, 0, 0.5);
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
            if (JUMP_BOOST_EFFECT != null) {
                player.removePotionEffect(JUMP_BOOST_EFFECT);
            }
            player.setWalkSpeed(0.2f);
            player.setSwimming(false);
        }
    }

    private void applyEffects(Player player) {
        if (PluginConfig.glow) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        }
        if (JUMP_BOOST_EFFECT != null) {
            player.addPotionEffect(new PotionEffect(JUMP_BOOST_EFFECT, Integer.MAX_VALUE, 200, false, false));
        }
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
