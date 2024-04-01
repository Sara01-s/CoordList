package com.lavy01.coordlist;

import static java.lang.Math.max;
import static java.lang.Math.abs;

import java.util.function.Consumer;
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
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class CoordListCommand implements TabExecutor {
	
    private final int MAX_COORDS_PER_PLAYER = 5;
    private final int MAX_COORD_NAME_CHARACTERS = 16;
    private final CoordList plugin;
    
	private List<Coord> playerCoordList = new ArrayList<>();
    private CoordTracker coordTracker;
    
    
    public CoordListCommand(final CoordList plugin) {
        this.plugin = plugin;
    }

    private void msg(final String message, final Player player) {
        player.sendMessage(this.plugin.NAME + ChatColor.WHITE + message);
    }

    private void msgError(final String message, final Player player) {
        player.sendMessage(this.plugin.NAME + ChatColor.RED + message);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof final Player player)) {
            this.plugin.logError("Cannot execute this command from console.");
            return true;
        }
        
        this.playerCoordList = this.plugin.getPlayerCoordList(player.getUniqueId());

        if (args.length <= 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("forceclear")) { // Prevent player for using this directly with permissions
            forceClearCoordList(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("view")) {
            if (args.length > 1) {
                msg("Usage: /coordlist view", player);
                return true;
            }

            displayCoordList(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length > 1) {
                msg("Usage: /coordlist clear", player);
                return true;
            }

            showClearPrompt(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("rename")) {
            if (args.length == 3) {
                renameCoord(player, args[1], args[2]);
                return true;
            }

            msg("Usage: /coordlist rename <name> <new name>", player);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length <= 1) {
                msg("Usage: /coordlist remove <name>", player);
                return true;
            }

            removeCoord(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 5) { // Player wants to set Custom coords
                var customCoord = getClampedCoords(args[2], args[3], args[4]);
                addCoord(player, args[1], new Location(player.getWorld(), customCoord.getX(), customCoord.getY(), customCoord.getZ()));
                return true;
            }

            if (args.length <= 1) {
                msg("Usage: /coordlist add <name> [x] [y] [z]", player);
                return true;
            }

            addCoord(player, args[1], player.getLocation());
            return true;
        }

        if (args[0].equalsIgnoreCase("track")) {
            if (args.length == 1) {
                if (this.coordTracker != null) {
                    if (!this.coordTracker.isCancelled()) {

                        this.coordTracker.cancel();
    
                        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                        msg("Tracking canceled.", player);
                        return true;
                    }
                }

                msg("Usage: /coordlist track <name>", player);
                return true;
            }

            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                msgError("Saving or tracking nether/end coords is not yet supported.", player);
                return true;
            }

            if (args.length == 4) {
                var customCoord = getClampedCoords(args[1], args[2], args[3]);
                var targetLocation = new Location(player.getWorld(), customCoord.getX(), customCoord.getY(), customCoord.getZ());
                
                startTrackingCoord(player, targetLocation);
                return true;
            }

            startTrackingCoord(player, args[1]);
            return true;
        }
        
        return true;
    }



	private void addCoord(final Player player, final String coordName, final Location location) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            msgError("Saving or tracking nether/end coords is not yet supported.", player);
            return;
        }

        // I use max(1, MAX_COORDS) because MAX COORDS must never be <= 0.
        if (this.playerCoordList.size() >= max(1, MAX_COORDS_PER_PLAYER)) {
            msgError("Max coords limit reached! (" + MAX_COORDS_PER_PLAYER + "/" + this.playerCoordList.size() + ").", player);
            return;
        }

        if (!isValidName(player, coordName)) {
            return;
        }

        if (!isValidCoord(player, new Coord(location, coordName))) {
            return;
        }

		this.plugin.addCoord(player.getUniqueId(), new Coord(location, coordName));
		
		msg("Coord " + ChatColor.YELLOW + coordName + ChatColor.WHITE + " added to your list.", player);
	}

    private void renameCoord(final Player player, final String coordName, final String newCoordName) {
        if (!isValidName(player, newCoordName)) {
            return;
        }

        if (coordName.equals(newCoordName)) {
            msgError("That is the same name.", player);
            return;
        }

        var playerId = player.getUniqueId();
        var coordBeingRenamed = this.plugin.getCoordByName(playerId, coordName);

        if (!this.plugin.playerHasCoord(playerId, coordBeingRenamed)) {
            msgError("A Coord named \"" + coordName + "\" is not present in your list.", player);
            return;
        }

        this.plugin.renameCoord(player.getUniqueId(), coordName, newCoordName);
        msg(Utils.colorize("Coord &e" + coordName + " &frenamed to: &e" + newCoordName), player);
    }

    private void removeCoord(final Player player, final String coordName) {
        final var coord = this.plugin.getCoordByName(player.getUniqueId(), coordName);

        if (this.plugin.playerHasCoord(player.getUniqueId(), coord)) {
            this.plugin.removeCoord(player.getUniqueId(), coord);
            msg("Coord " + ChatColor.YELLOW + coordName + ChatColor.WHITE + " removed.", player);
            return;
        }

        msgError("A Coord named \"" + coordName + "\" is not present in your list.", player);
    }

	
	private void displayCoordList(final Player player) {
		if (this.playerCoordList == null || this.playerCoordList.isEmpty()) {
			msg("CoordList empty.", player);
			return;
		}
		
		msg(Utils.colorize("&a---&8[&eYour saved coords&8]&a---"), player);
			
		for (int i = 0; i < this.playerCoordList.size(); i++) {
			player.sendMessage(Utils.colorize (
                "&a" + (i + 1 + "") + "&7" + ": " 
                + "&e" + this.playerCoordList.get(i).getName() + " ") 
                + ChatColor.GRAY + ">> " 
                + this.playerCoordList.get(i).getFormattedCoords()
            );
		}
	}

    private void showClearPrompt(final Player player) {
        if (this.playerCoordList == null || this.playerCoordList.isEmpty()) {
			msgError("CoordList already empty.", player);
			return;
		}

        msgError("WARNING! this command will DELETE ALL your saved coords.", player);

		new SpigotCallback(this.plugin);
		Consumer<Player> clearCoordListCallback = targetPlayer -> {
			forceClearCoordList(targetPlayer);
		};

        final var promptYes = new TextComponent(ChatColor.GRAY + "- " + ChatColor.GREEN + ChatColor.UNDERLINE + "[yes]");
            //promptYes.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/coordlist forceclear"));
            promptYes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                    new ComponentBuilder(ChatColor.GREEN + "Clear your coordlist").create()));

        final var promptNo = new TextComponent(ChatColor.GRAY + "- " + ChatColor.RED + ChatColor.UNDERLINE + "[no]");
            promptNo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                   new ComponentBuilder(ChatColor.RED + "Cancel").create()));

        player.sendMessage(ChatColor.BOLD + "" + ChatColor.GOLD + "Confirm coordlist clear?");
        player.spigot().sendMessage(promptYes);
        player.spigot().sendMessage(promptNo);

		SpigotCallback.createCommand(promptYes, clearCoordListCallback);

	}

    private void forceClearCoordList(final Player player) {
        this.plugin.clearCoordList(player.getUniqueId());
        msg("CoordList cleared.", player);
    }
    
    private boolean isValidName(final Player player, final String coordName) {
	    final String validCharacters = "[a-zA-Z0-9_]*";

		if (!coordName.matches(validCharacters)) {
			msgError("Name can only contain letters or numbers.", player);
			return false;
		}
        
		if (coordName.length() >= MAX_COORD_NAME_CHARACTERS) {
			msgError("Name cannot exceed " + MAX_COORD_NAME_CHARACTERS + " characters.", player);
			return false;
		}

	    return true;
	}

    private Vector getClampedCoords(final String x, final String y, final String z) {
        // Clamps the coords to reasonable values, althought 30_000_000 is considered "out of bounds"
        // this is used because some funny player could input something like 100000000000000000000000000000000000
        final double _x = Utils.clamp(Double.parseDouble(x), -30_000_000, 30_000_000);
        final double _y = Utils.clamp(Double.parseDouble(y), 0, 300);
        final double _z = Utils.clamp(Double.parseDouble(z), -30_000_000, 30_000_000);

        return new Vector(_x, _y, _z);
    }

    private boolean isValidCoord(final Player player, final Coord coord) {
        final double x = abs(coord.getLocation().getX());
        final double y = abs(coord.getLocation().getY());
        final double z = abs(coord.getLocation().getZ());
    
        if (x > 29_999_970 || y > 255 || z > 29_999_970) {
            msgError("Specified coords out of world bounds.", player);
            return false;
        }

        if (this.plugin.playerHasCoord(player.getUniqueId(), coord)) {
            msgError("A coord with name " + ChatColor.YELLOW + coord.getName() + ChatColor.RED + " is already present in your list.", player);
            return false;
        }
        
        return true;
    }
    	
	private void startTrackingCoord(final Player player, final String targetCoordName) {
        final var targetCoord = this.plugin.getCoordByName(player.getUniqueId(), targetCoordName);
        
        if (!this.plugin.playerHasCoord(player.getUniqueId(), targetCoord)) {
            msgError("Coord not found.", player);
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

    private void startTrackingCoord(final Player player, final Location targetLocation) {
        final String currentPlayerPosFormatted = Coord.getFormattedLocation(player.getLocation());
        final var tempTargetCoord = new Coord(targetLocation, "Custom");

        if (!isValidCoord(player, new Coord(targetLocation, tempTargetCoord.getName()))) {
            return;
        }
        
        // Auto-Cancel current tracker if player switches target without cancelling before
        if (this.coordTracker != null) { 
            this.coordTracker.cancel();
        }

        this.coordTracker = new CoordTracker(player, this.plugin, currentPlayerPosFormatted, tempTargetCoord);
        this.coordTracker.runTaskTimerAsynchronously(this.plugin, 0, 1);
	}

    private void showHelp(final Player player) {
		msg(Utils.colorize("&a---&8[&eHelp&8]&a---"), player);
        player.sendMessage(Utils.colorize("&e/coordlist add &6<name>, &7Adds your current coords to your list."));
        player.sendMessage(Utils.colorize("&e/coordlist add &6<name> [x] [y] [z], &7Saves custom coords to your list."));
        player.sendMessage(Utils.colorize("&e/coordlist remove &6<name>, &7Deletes a saved coord from your list."));
        player.sendMessage(Utils.colorize("&e/coordlist view &6<name>, &7Displays a list of all your saved coords"));
        player.sendMessage(Utils.colorize("&e/coordlist track &6<name>, &7Shows where is the indicated saved coord."));
        player.sendMessage(Utils.colorize("&e/coordlist track &6[x] [y] [z], &7Shows where is the indicated custom coord."));
        player.sendMessage(Utils.colorize("&e/coordlist rename &6<name> <new name>, &7Changes the name of a saved coord without losing the original location."));
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

