package com.j3ly.colorchat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

public class ColorChatPlugin extends JavaPlugin {

    private final Map<String, ChatColor> playerColors = new HashMap<String, ChatColor>();

    private final PlayerListener playerListener = new PlayerListener() {
        @Override
        public void onPlayerChat(PlayerChatEvent event) {
            Player player = event.getPlayer();
            String name = player.getName().toLowerCase();

            ChatColor color = playerColors.getOrDefault(name, ChatColor.WHITE);

            // Apply color to their chat message
            event.setMessage(color + event.getMessage() + ChatColor.WHITE);
        }
    };

    @Override
    public void onEnable() {
        createDefaultConfig();
        loadPlayerColors();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);

        System.out.println("[ColorChat] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[ColorChat] Disabled");
    }

    private void createDefaultConfig() {
        try {
            File folder = getDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File configFile = new File(folder, "config.yml");
            if (!configFile.exists()) {
                FileWriter writer = new FileWriter(configFile);
                writer.write("chat-colors:\n");
                writer.write("  timmy: AQUA\n");
                writer.write("  ducky: GREEN\n");
                writer.close();
            }
        } catch (Exception e) {
            System.out.println("[ColorChat] Error creating default config: " + e.getMessage());
        }
    }

    private void loadPlayerColors() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) return;

            InputStream input = new FileInputStream(configFile);
            Yaml yaml = new Yaml();
            Object rawData = yaml.load(input);
            input.close();

            if (!(rawData instanceof Map)) {
                System.out.println("[ColorChat] Invalid config format.");
                return;
            }

            Map<?, ?> root = (Map<?, ?>) rawData;
            Object colorsObj = root.get("chat-colors");

            if (colorsObj instanceof Map<?, ?>) {
                Map<?, ?> colorMap = (Map<?, ?>) colorsObj;

                for (Map.Entry<?, ?> entry : colorMap.entrySet()) {
                    String playerName = entry.getKey().toString().toLowerCase();
                    String colorName = entry.getValue().toString().toUpperCase();

                    try {
                        ChatColor color = ChatColor.valueOf(colorName);
                        playerColors.put(playerName, color);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("[ColorChat] Invalid color for " + playerName + ": " + colorName);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[ColorChat] Failed to load config: " + e.getMessage());
        }
    }
}
