package me.eaz.galacticvotes.util;

import org.bukkit.ChatColor;

public class MessageUtil {

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String colorPlayer(String message, String playerName) {
        return color(message.replace("%player%", playerName));
    }
}
