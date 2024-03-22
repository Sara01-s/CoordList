package com.lavy01.coordlist;

import static java.lang.Math.abs;
import static java.lang.Math.clamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.util.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public final class CoordListCommand implements TabExecutor {
	
    private final int MAX_COORDS_PER_PLAYER = 5;
    private final CoordList plugin;
    
	private List<Coord> playerCoordList = new ArrayList<>();
    private CoordTracker coordTracker;
    
    
    public CoordListCommand(final CoordList plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] cmdArgs) {
        
        if (!(sender instanceof final Player player)) {
            this.plugin.logError("Cannot execute this command from console.");
            return true;
        }

        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(this.plugin.NAME + ChatColor.RED + "Saving nether or end coords is not yet supported.");
            return true;
        }
        
        this.playerCoordList = this.plugin.getPlayerCoordList(player.getUniqueId());

        if (cmdArgs.length <= 0) {
            showHelp(player);
            return true;
        }
        
        if (cmdArgs[0].equalsIgnoreCase("view")) {
            if (cmdArgs.length > 1) {
                player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist view");
                return true;
            }

            displayCoordList(player);
            return true;
        }

        if (cmdArgs[0].equalsIgnoreCase("clear")) {
            if (cmdArgs.length > 1) {
                player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist clear");
                return true;
            }

            clearCoordList(player);
            return true;
        }

        if (cmdArgs[0].equalsIgnoreCase("add")) {
            if (cmdArgs.length == 5) { // Player wants to set Custom coords
                var customCoord = getClampedCoords(cmdArgs[2], cmdArgs[3], cmdArgs[4]);
                addCoord(player, cmdArgs[1], new Location(player.getWorld(), customCoord.getX(), customCoord.getY(), customCoord.getZ()));
                return true;
            }

            if (cmdArgs.length <= 1) {
                player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist add <name> [x] [y] [z]");
                return true;
            }

            addCoord(player, cmdArgs[1], player.getLocation());
            return true;
        }

        if (cmdArgs[0].equalsIgnoreCase("rename")) {
            if (cmdArgs.length == 3) {
                renameCoord(player, cmdArgs[1], cmdArgs[2]);
                return true;
            }

            player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist rename <name> <newName>");
            return true;
        }

        if (cmdArgs[0].equalsIgnoreCase("remove")) {
            if (cmdArgs.length <= 1) {
                player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist remove <name>");
                return true;
            }

            removeCoord(player, cmdArgs[1]);
            return true;
        }

        if (cmdArgs[0].equalsIgnoreCase("track")) {
            if (cmdArgs.length == 1) {
                if (!this.coordTracker.isCancelled()) {

                    this.coordTracker.cancel();

                    player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Tracking canceled.");
                    return true;
                }

                player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "Usage: /coordlist track <name>");
                return true;
            }

            startTrackingCoord(player, cmdArgs[1]);
            return true;
        }
        
        return true;
    }

	private void addCoord(final Player player, final String coordName, final Location location) {
        // I use abs(MAX_COORDS) because I've seen things... horrible things...
        if (this.playerCoordList.size() >= abs(MAX_COORDS_PER_PLAYER)) {
            player.sendMessage(plugin.NAME + ChatColor.RED + "Max coords limit reached! (" + MAX_COORDS_PER_PLAYER + "/" + this.playerCoordList.size() + ").");
            return;
        }

        if (!isValidName(player, coordName)) {
            return;
        }

        if (!isValidCoord(player, new Coord(location, coordName))) {
            return;
        }

		this.plugin.addCoord(player.getUniqueId(), new Coord(location, coordName));
		
		player.sendMessage(plugin.NAME + 
				ChatColor.WHITE + "Coord " + ChatColor.YELLOW + coordName + ChatColor.WHITE + " added to your list.");
	}

    private void renameCoord(final Player player, final String coordName, String newCoordName) {
        if (!isValidName(player, newCoordName)) {
            return;
        }

        for (var coord: playerCoordList) {
            if (coord.getName().equals(newCoordName)) {
                player.sendMessage(plugin.NAME + ChatColor.RED + 
                                    "That is the same name.");
                return;
            }
            if (coord.getName().equalsIgnoreCase(coordName)) {
                this.plugin.renameCoord(player.getUniqueId(), coordName, newCoordName);
                player.sendMessage(plugin.NAME + ChatColor.WHITE + 
                                    "Coord " + coordName + " renamed to: " + ChatColor.YELLOW + newCoordName);
                return;
            }
        }
        
        player.sendMessage(plugin.NAME + ChatColor.RED + 
                            "A Coord named \"" + coordName + "\" is not present in your list.");
    }

    private void removeCoord(final Player player, final String coordName) {
        final var coord = this.plugin.getCoordByName(player.getUniqueId(), coordName);

        if (this.plugin.playerHasCoord(player.getUniqueId(), coord)) {
            this.plugin.removeCoord(player.getUniqueId(), coord);
            player.sendMessage(plugin.NAME + ChatColor.WHITE + "Coord " + ChatColor.YELLOW + coordName + ChatColor.WHITE + " removed.");
            return;
        }

        player.sendMessage(plugin.NAME + ChatColor.RED + "Coord not found.");
    }

	
	private void displayCoordList(final Player player) {
		if (this.playerCoordList == null || this.playerCoordList.isEmpty()) {
			player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "CoordList empty.");
			return;
		}
		
		player.sendMessage(Utils.colorize(this.plugin.NAME +
				"&a---&8[&eYour saved coords&8]&a---"));
			
		for (int i = 0; i < this.playerCoordList.size(); i++) {
			player.sendMessage(Utils.colorize (
                "&a" + (i + 1 + "") + "&7" + ": " +
                "&b" + this.playerCoordList.get(i).getName() + " ") +
                ChatColor.GRAY + ">> " + this.playerCoordList.get(i).getFormattedCoords());
		}
	}

    private void clearCoordList(final Player player) {
        if (this.playerCoordList == null || this.playerCoordList.isEmpty()) {
			player.sendMessage(this.plugin.NAME + ChatColor.RED + "CoordList already empty.");
			return;
		}

        this.plugin.clearCoordList(player.getUniqueId());
        player.sendMessage(plugin.NAME + ChatColor.WHITE + "Coordlist cleared.");
	}
    
    private boolean isValidName(final Player player, final String name) {
	    final String validCharacters = "[a-zA-Z0-9_]*";
		
		if (!name.matches(validCharacters)) {
			player.sendMessage(plugin.NAME + 
					ChatColor.RED + "Name can only contain letters or numbers.");
			return false;
		}
        
		if (name.length() >= 16) {
			player.sendMessage(plugin.NAME + 
					ChatColor.RED + "Name cannot exceed 16 characters.");
			return false;
		}

	    return true;
	}

    private Vector getClampedCoords(final String x, final String y, final String z) {
        // Clamps the coords to reasonable values, althought 30_000_000 is considered "out of bounds"
        // this is used because some funny player could input something like 100000000000000000000000000000000000
        final double _x = clamp(Double.parseDouble(x), -30_000_000, 30_000_000);
        final double _y = clamp(Double.parseDouble(y), 0, 300);
        final double _z = clamp(Double.parseDouble(z), -30_000_000, 30_000_000);

        return new Vector(_x, _y, _z);
    }

    private boolean isValidCoord(final Player player, final Coord coord) {
        final double x = abs(coord.getLocation().getX());
        final double y = abs(coord.getLocation().getY());
        final double z = abs(coord.getLocation().getZ());
    
        if (x > 29_999_970 || y > 255 || z > 29_999_970) {
            player.sendMessage(plugin.NAME + ChatColor.RED + "Specified coords out of world bounds.");
            return false;
        }

        if (this.plugin.playerHasCoord(player.getUniqueId(), coord)) {
            player.sendMessage(plugin.NAME + ChatColor.RED + "A coord with name " + ChatColor.YELLOW + coord.getName() + ChatColor.RED + " is already present in your list.");
            return false;
        }
        
        return true;
    }
    	
	private void startTrackingCoord(final Player player, final String targetCoordName) {
        final var targetCoord = this.plugin.getCoordByName(player.getUniqueId(), targetCoordName);
        
        if (!this.plugin.playerHasCoord(player.getUniqueId(), targetCoord)) {
            player.sendMessage(plugin.NAME + ChatColor.RED + "Coord not found.");
            return;
        }

        final String currentPlayerPosFormatted = Coord.getFormattedLocation(player.getLocation());
        
        // Auto-Cancel current tracker if player switches target without cancelling before
        if (this.coordTracker != null) { 
            this.coordTracker.cancel();
        }

        this.coordTracker = new CoordTracker(player, this.plugin, currentPlayerPosFormatted, targetCoord);
        this.coordTracker.runTaskTimerAsynchronously(this.plugin, 0, 1);
	}

    private void showHelp(final Player player) {
		player.sendMessage(Utils.colorize(this.plugin.NAME + "&a---&8[&eHelp&8]&a---"));
        player.sendMessage(ChatColor.YELLOW + "/coordlist add <name>, Add your current coords to your list.");
        player.sendMessage(ChatColor.YELLOW + "/coordlist add <name> [x] [y] [z], Save custom to your list.");
        player.sendMessage(ChatColor.YELLOW + "/coordlist remove <name>, Delete a saved coord from your list.");
        player.sendMessage(ChatColor.YELLOW + "/coordlist view <name>, Displays a list of all your saved coords");
        player.sendMessage(ChatColor.YELLOW + "/coordlist track <name>, Shows where is the indicated saved coord.");
        player.sendMessage(ChatColor.YELLOW + "/coordlist rename <name> <new name>, Change the name of a saved coord without losing the original location.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof final Player player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("help", "add", "remove", "view", "track", "rename", "clear");
        }

        if (args[0].equalsIgnoreCase("rename")) {
            if (args.length == 3) {
                return Arrays.asList("<new name>");
            }

            return this.plugin.getPlayerCoordsNames(player.getUniqueId());
        }

        if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("track")) {
            return this.plugin.getPlayerCoordsNames(player.getUniqueId());
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 2) {
                return Arrays.asList("<name>");
            }
            
            if (args.length == 3) {
                return Arrays.asList("[x]");
            }

            if (args.length == 4) {
                return Arrays.asList("[y]");
            }

            if (args.length == 5) {
                return Arrays.asList("[z]");
            }
        }

        return new ArrayList<>();
    }
}

