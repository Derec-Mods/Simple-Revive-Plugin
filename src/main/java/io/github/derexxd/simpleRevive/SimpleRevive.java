package io.github.derexxd.simpleRevive;

import io.github.derexxd.simpleRevive.config.PluginConfig;
import io.github.derexxd.simpleRevive.listener.FatalDamageListener;
import io.github.derexxd.simpleRevive.listener.PlayerRestrictionListener;
import io.github.derexxd.simpleRevive.manager.DownedManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class SimpleRevive extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        PluginConfig.load(getConfig());

        getServer().getPluginManager().registerEvents(new FatalDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRestrictionListener(), this);

        PluginCommand command = getCommand("simplerevive");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        Bukkit.getScheduler().runTaskTimer(this, DownedManager::tickAll, 2L, 2L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DownedManager.cleanupAll();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("simplerevive.admin")) {
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            reloadConfig();
            PluginConfig.load(getConfig());
            sender.sendMessage(Component.text("SimpleRevive configuration reloaded.", NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /simplerevive reload", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}
