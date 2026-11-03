package io.github.derexxd.simpleRevive;

import io.github.derexxd.simpleRevive.config.PluginConfig;
import io.github.derexxd.simpleRevive.listener.FatalDamageListener;
import io.github.derexxd.simpleRevive.listener.PlayerRestrictionListener;
import io.github.derexxd.simpleRevive.manager.DownedManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleRevive extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        PluginConfig.load(getConfig());

        getServer().getPluginManager().registerEvents(new FatalDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRestrictionListener(), this);

        Bukkit.getScheduler().runTaskTimer(this, DownedManager::tickAll, 2L, 2L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DownedManager.cleanupAll();
    }
}
