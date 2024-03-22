package com.lavy01.coordlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class CoordsDatabase {

    public final HashMap<UUID, List<Coord>> playerCoords = new HashMap<>();
    private final String PLAYERS_DATA_ENTRY = "playerIds.";
    private final CoordList plugin;
    
    public CustomConfigFile<CoordList> configFile;

    
    public CoordsDatabase(final CoordList plugin) {
        this.configFile = new CustomConfigFile<CoordList>(plugin, "coords");
        this.plugin = plugin;
    }

    public List<Coord> getPlayerCoords(final UUID player) {
        if (this.playerCoords.containsKey(player)) {
            return this.playerCoords.get(player);
        }
        
        this.plugin.logError("Player with id: " + player.toString() + " does not have a coordlist.");
        return null;
    }

    public void addCoord(final UUID playerId, final Coord coord) {
        if (!this.playerCoords.containsKey(playerId)) {
            // Player doesn't have a list, create a new list for them.
            this.playerCoords.put(playerId, new ArrayList<Coord>());
            this.plugin.log("New coordlist created for player with id: " + playerId);
        }
        
        // Add coord to player's list
        this.playerCoords.get(playerId).add(coord);
        saveAllCoords();
    }

    public void clearCoordList(final UUID playerId) {
        this.playerCoords.get(playerId).clear();
        saveAllCoords();
    }

    public void removeCoord(final UUID playerId, final Coord coord) {
        if (!this.playerCoords.containsKey(playerId)) {
            return;
        }

        final var playerCoordlist = this.playerCoords.get(playerId);

        for (int i = 0; i < playerCoordlist.size(); i++) {
            if (playerCoordlist.get(i).getName().equals(coord.getName())) {
                playerCoordlist.remove(i);
                break;
            }
        }

        this.playerCoords.put(playerId, playerCoordlist);
        saveAllCoords();
    }

    public void saveAllCoords() {
        if (this.configFile.getConfig() == null) return;

        for (var entry: this.playerCoords.entrySet()) {
            this.configFile.getConfig().set(this.PLAYERS_DATA_ENTRY + entry.getKey().toString(), entry.getValue());
        }

        this.configFile.saveData();
    }

    public void loadCoords() {
        final var dataConfigFile = this.configFile.getConfig();

        if (!dataConfigFile.contains(PLAYERS_DATA_ENTRY)) {
            return;
        }

        final var configSection = dataConfigFile.getConfigurationSection(this.PLAYERS_DATA_ENTRY);
        final var playerIds = configSection.getKeys(false);
    
        playerIds.forEach(playerId -> {
            var coordsBuffer = this.configFile.getConfig().get(this.PLAYERS_DATA_ENTRY + playerId);

            @SuppressWarnings("unchecked")
            var playerSavedCoords = (List<Coord>) coordsBuffer;
            
            playerCoords.put(UUID.fromString(playerId), playerSavedCoords);
        });

        this.plugin.log("Player coordlists loaded.");
    }

    public boolean playerHasCoord(UUID owner, Coord coord) {
        return this.playerCoords.get(owner).contains(coord);
    }

}
