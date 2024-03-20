package com.lavy01.coordlist;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import net.md_5.bungee.api.ChatColor;

@SerializableAs("Coord")
public class Coord implements ConfigurationSerializable, Cloneable {

    private final Location location;
    private String name;

	
	public Coord(Location location, String name) {
		this.location = location;
		this.name = name;
	}

    public static String getFormattedLocation(Location loc) {
		return ChatColor.translateAlternateColorCodes('&', "&8[&a" 
            + Math.round(loc.getX()) + ", "
            + Math.round(loc.getY()) + ", " 
            + Math.round(loc.getZ()) + "&8]");
	}

	public String getFormattedCoords() {
		return ChatColor.translateAlternateColorCodes('&', "&8[&a" 
            + Math.round(location.getX()) + ", "
            + Math.round(location.getY()) + ", " 
            + Math.round(location.getZ()) + "&8]");
	}

	public String getName() {
		return name;
	}

    public void setName(String newName) {
		this.name = newName;
	}

    public Location getLocation() {
		return this.location;
	}

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', "&8[&a" 
            + Math.round(location.getX()) + ", "
            + Math.round(location.getY()) + ", " 
            + Math.round(location.getZ()) + "&8]");
    }
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name);
		map.put("location", location);
		
		return map;
	}

	 public static Coord deserialize(Map<String, Object> args) {
        String name = "";
        Location location = null;
        
        if (args.containsKey("name")) {
            name = (String) args.get("name");
        }

        if (args.containsKey("location")) {
            location = (Location) args.get("location");
        }
        
        return new Coord(location, name);
	}
}
