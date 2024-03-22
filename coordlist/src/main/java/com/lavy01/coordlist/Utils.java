package com.lavy01.coordlist;

import net.md_5.bungee.api.ChatColor;

public final class Utils {

    // Used intead of ChatColor.translateAlternateColorCodesOmgWhyIsThisFunctionNameSoLong()
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
