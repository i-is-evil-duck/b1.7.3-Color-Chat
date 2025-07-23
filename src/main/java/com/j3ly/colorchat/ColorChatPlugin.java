package com.j3ly.colorchat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ColorChatPlugin extends JavaPlugin {

    private final Map<String, Group> groups = new HashMap<>();
    private final Map<String, String> playerGroups = new HashMap<>();
    private final Map<String, ChatColor> playerNameColors = new HashMap<>();
    private final Map<String, ChatColor> playerChatColors = new HashMap<>();

    private ChatColor bracketColor = ChatColor.YELLOW;
    private ChatColor plusColor = ChatColor.GOLD;

    private final PlayerListener chatListener = new PlayerListener() {
        @Override
        public void onPlayerChat(PlayerChatEvent event) {
            Player player = event.getPlayer();
            String name = player.getName().toLowerCase();

            ChatColor nameColor = playerNameColors.getOrDefault(name, ChatColor.WHITE);
            ChatColor chatColor = playerChatColors.getOrDefault(name, nameColor);

            String groupKey = playerGroups.get(name);
            String rankPrefix = "";
            if (groupKey != null && groups.containsKey(groupKey)) {
                Group g = groups.get(groupKey);
                String coloredRank = g.name.replace("+", plusColor + "+" + ChatColor.WHITE);
                rankPrefix = bracketColor + "[" + g.color + coloredRank + bracketColor + "] " + ChatColor.WHITE;
            }

            String fullName = nameColor + player.getName();
            String msg = chatColor + event.getMessage();

            event.setFormat(rankPrefix + fullName + ChatColor.WHITE + ": " + msg);
        }
    };

    @Override
    public void onEnable() {
        createDefaultConfig();
        loadConfig();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_CHAT, chatListener, Priority.Normal, this);
        System.out.println("[ColorChat] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        System.out.println("[ColorChat] Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("colorchat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "/colorchat reload");
            sender.sendMessage(ChatColor.GOLD + "/colorchat role <player> <group>");
            sender.sendMessage(ChatColor.GOLD + "/colorchat name <player> <color>");
            sender.sendMessage(ChatColor.GOLD + "/colorchat chat <player> <color>");
            sender.sendMessage(ChatColor.GOLD + "/colorchat clear <player>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "ColorChat config reloaded.");
                return true;
            case "role":
                if (args.length < 3) return false;
                playerGroups.put(args[1].toLowerCase(), args[2]);
                saveConfigFile();
                sender.sendMessage("Set role of " + args[1] + " to " + args[2]);
                return true;
            case "name":
                if (args.length < 3) return false;
                playerNameColors.put(args[1].toLowerCase(), ChatColor.valueOf(args[2].toUpperCase()));
                saveConfigFile();
                sender.sendMessage("Set name color of " + args[1] + " to " + args[2]);
                return true;
            case "chat":
                if (args.length < 3) return false;
                playerChatColors.put(args[1].toLowerCase(), ChatColor.valueOf(args[2].toUpperCase()));
                saveConfigFile();
                sender.sendMessage("Set chat color of " + args[1] + " to " + args[2]);
                return true;
            case "clear":
                if (args.length < 2) return false;
                String clearName = args[1].toLowerCase();
                playerGroups.remove(clearName);
                playerNameColors.remove(clearName);
                playerChatColors.remove(clearName);
                saveConfigFile();
                sender.sendMessage("Cleared all color and role settings for " + args[1]);
                return true;
        }

        return false;
    }

    private void createDefaultConfig() {
        try {
            File folder = getDataFolder();
            if (!folder.exists()) folder.mkdirs();

            File configFile = new File(folder, "config.yml");
            if (!configFile.exists()) {
                FileWriter writer = new FileWriter(configFile);
                writer.write("chat-colors: {mikey: BLUE}\n");
                writer.write("player-colors: {mikey: BROWN, boy_kisser_owo: LIGHT_PURPLE}\n");
                writer.write("plus-color: GOLD\n");
                writer.write("groups:\n");
                writer.write("  mvp+++: {color: LIGHT_PURPLE, name: MVP+++}\n");
                writer.write("  vip+: {color: WHITE, name: VIP+}\n");
                writer.write("bracket-color: LIGHT_PURPLE\n");
                writer.write("group-members: {timmy: mvp+++, boy_kisser_owo: 'vip+'}\n");
                writer.close();
            }
        } catch (Exception e) {
            System.out.println("Failed to write default config: " + e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) return;

            InputStream input = new FileInputStream(file);
            Yaml yaml = new Yaml();
            Object raw = yaml.load(input);
            input.close();

            if (!(raw instanceof Map)) return;
            Map<?, ?> root = (Map<?, ?>) raw;

            groups.clear();
            playerGroups.clear();
            playerNameColors.clear();
            playerChatColors.clear();

            Map<?, ?> groupsSection = (Map<?, ?>) root.get("groups");
            if (groupsSection != null) {
                for (Map.Entry<?, ?> entry : groupsSection.entrySet()) {
                    String key = entry.getKey().toString();
                    Map<?, ?> data = (Map<?, ?>) entry.getValue();
                    String name = data.get("name").toString();
                    ChatColor color = ChatColor.valueOf(data.get("color").toString().toUpperCase());
                    groups.put(key, new Group(name, color));
                }
            }

            Map<?, ?> membersSection = (Map<?, ?>) root.get("group-members");
            if (membersSection != null) {
                for (Map.Entry<?, ?> entry : membersSection.entrySet()) {
                    playerGroups.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
                }
            }

            Map<?, ?> nameColors = (Map<?, ?>) root.get("player-colors");
            if (nameColors != null) {
                for (Map.Entry<?, ?> entry : nameColors.entrySet()) {
                    playerNameColors.put(entry.getKey().toString().toLowerCase(), ChatColor.valueOf(entry.getValue().toString().toUpperCase()));
                }
            }

            Map<?, ?> chatColors = (Map<?, ?>) root.get("chat-colors");
            if (chatColors != null) {
                for (Map.Entry<?, ?> entry : chatColors.entrySet()) {
                    playerChatColors.put(entry.getKey().toString().toLowerCase(), ChatColor.valueOf(entry.getValue().toString().toUpperCase()));
                }
            }

            if (root.containsKey("bracket-color")) {
                bracketColor = ChatColor.valueOf(root.get("bracket-color").toString().toUpperCase());
            }

            if (root.containsKey("plus-color")) {
                plusColor = ChatColor.valueOf(root.get("plus-color").toString().toUpperCase());
            }

        } catch (Exception e) {
            System.out.println("Failed to load config: " + e.getMessage());
        }
    }

    private void saveConfigFile() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            Yaml yaml = new Yaml();
            Map<String, Object> root = new HashMap<>();

            Map<String, Object> groupData = new HashMap<>();
            for (Map.Entry<String, Group> entry : groups.entrySet()) {
                Map<String, Object> g = new HashMap<>();
                g.put("name", entry.getValue().name);
                g.put("color", entry.getValue().color.name());
                groupData.put(entry.getKey(), g);
            }
            root.put("groups", groupData);
            root.put("group-members", playerGroups);

            Map<String, String> nameColors = new HashMap<>();
            for (Map.Entry<String, ChatColor> entry : playerNameColors.entrySet()) {
                nameColors.put(entry.getKey(), entry.getValue().name());
            }
            root.put("player-colors", nameColors);

            Map<String, String> chatColors = new HashMap<>();
            for (Map.Entry<String, ChatColor> entry : playerChatColors.entrySet()) {
                chatColors.put(entry.getKey(), entry.getValue().name());
            }
            root.put("chat-colors", chatColors);

            root.put("bracket-color", bracketColor.name());
            root.put("plus-color", plusColor.name());

            FileWriter writer = new FileWriter(file);
            yaml.dump(root, writer);
        } catch (Exception e) {
            System.out.println("Failed to save config: " + e.getMessage());
        }
    }

    private static class Group {
        public String name;
        public ChatColor color;

        public Group(String name, ChatColor color) {
            this.name = name;
            this.color = color;
        }
    }
}
