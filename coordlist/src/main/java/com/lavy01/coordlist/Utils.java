package com.lavy01.coordlist;

import net.md_5.bungee.api.ChatColor;

public final class Utils {

    // Used intead of ChatColor.translateAlternateColorCodesOmgWhyIsThisFunctionNameSoLong()
    public static String colorize(final String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

	public static double clamp(final double value, final double min, final double max) {
		return Math.max(min, Math.min(max, value));
	}
}
