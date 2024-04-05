// CoordList Minecraft PLugin
// by Sara San Martín 2024

/* TO DO:
    Playtest more.

    Admin commands (op):
        - /coordlist spy <player> (displays a player's list).
        - /coordlist clear <player> (forcefully deletes all saved coordinates of a player).
    
    Future considerations (Maybe I won't do it, but it would be great additions for the plugin):
        - Add support for all dimensions
        - Abstract the command logic to divide and make the CoordListCommand.java file more readable.
        - Implement permissions logic.
        - Make configurable variables, such as:
            - Max coords per player.
                - Admin command to increase player's coords capacity (this can be used as reward, progression milestone or rank advantage).
            - CoordTracker update period.
            - Display of CoordTracker arrow, because maybe you want to make the player figure out the direction for themselfs to add difficulty or "vanillaness".
            - Add sign input prompt to add coords name, rename, remove etc...
            - Add GUI.
            - Add the option to encrypt coords.yml data.
 */

package com.lavy01.coordlist;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoordList extends JavaPlugin {

	public final String NAME = Utils.colorize("&8[&aCoordList&8] ");
	private CoordsDatabase coordsDataBase;
	
	
	@Override
	public void onLoad() {
		ConfigurationSerialization.registerClass(Coord.class, "Coord");
	}
	
	@Override
	public void onEnable() {
		this.coordsDataBase = new CoordsDatabase(this);
	
		getCommand("coordlist").setExecutor(new CoordListCommand(this));
		this.coordsDataBase.loadCoords();
	
        new SpigotCallback(this); // Allows to send java function callbacks as Minecraft commmands

		log("Plugin enabled.");
	}
	
	@Override
	public void onDisable() {
		this.coordsDataBase.saveAllCoords();
		Bukkit.getScheduler().cancelTasks(this);
		log("Plugin disabled.");
	}

    public void addCoord(final UUID owner, final Coord coord) {
        this.coordsDataBase.addCoord(owner, coord);
    }

    public void renameCoord(final UUID owner, final String coordName, final String newCoordName) {
        getCoordByName(owner, coordName).setName(newCoordName);
    }

    public void removeCoord(final UUID owner, final Coord coord) {
        this.coordsDataBase.removeCoord(owner, coord);
    }

    public void clearCoordList(final UUID owner) {
        this.coordsDataBase.clearCoordList(owner);
    }

    public boolean playerHasCoord(final UUID owner, final Coord coord) {
        return this.coordsDataBase.playerHasCoord(owner, coord);
    }

    public List<Coord> getPlayerCoordList(final UUID playerId) {
        return this.coordsDataBase.getPlayerCoords(playerId);
    }

    public Coord getCoordByName(final UUID owner, final String coordName) {
        for (var coord: this.getPlayerCoordList(owner)) {
            if (coord == null) {
                continue;
            }

            if (coord.getName().equalsIgnoreCase(coordName)) {
                return coord;
            }
        }

        return null;
    }

    public List<String> getPlayerCoordsNames(UUID owner) {
        var coordNames = new ArrayList<String>();

        for (var coord: this.coordsDataBase.getPlayerCoords(owner)) {
            coordNames.add(coord.getName());
        }
        
        return coordNames;
    }

    public void log(final String message) {
        this.getLogger().info(message);
    }
    
    public void logWarn(final String message) {
        this.getLogger().warning(message);
    }

    public void logError(final String message) {
        this.getLogger().severe(message);
    }

}
