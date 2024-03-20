package com.lavy01.coordlist;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.floor;
import static java.lang.Math.toDegrees;

import java.util.HashMap;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;

import net.md_5.bungee.api.ChatColor;

public class CoordTracker extends BukkitRunnable {

    private final String TRACKER_OBJECTIVE_NAME = "coordTracker";
    private final String TARGET_COORDS_NAME_KEY = ChatColor.YELLOW + "Tracking: ";
    private final String TARGET_COORDS_KEY = ChatColor.YELLOW + "Destination: ";
    private final String CURRENT_COORDS_KEY = ChatColor.YELLOW + "Your position: ";
    private final double MIN_DISTANCE_TO_STOP_TRACKING = 15.0D;
    
    private double distanceToTarget = 0.0;
    private Coord targetCoord;
    private CoordList plugin;
    private Player player;

    private HashMap<Double, String> angleToArrow = new HashMap<>();
    private HashMap<String, ChatColor> arrowToChatColor = new HashMap<>();

    public double getDistanceToTarget() {
        return this.distanceToTarget;
    }

    private void resetScoreboard() {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        });
    }

    @Override
    public void run() {
        if (this.player == null) {
            Bukkit.getServer().getLogger().severe("[CoordList] Please call initTracker() before scheduling Coord Tracker.");
            return;
        }

        if (this.player.getScoreboard() != null && this.player.getScoreboard().getObjective(this.TRACKER_OBJECTIVE_NAME) != null) {
            updateTracker(this.player, Coord.getFormattedLocation(this.player.getLocation()), targetCoord);
        }

        if (this.distanceToTarget < MIN_DISTANCE_TO_STOP_TRACKING) {
            resetScoreboard(); // prevents creating asynchronous scoreboard (Illegal Exception)

            this.player.sendMessage(this.plugin.NAME + ChatColor.WHITE + "You arrived your destination.");
            this.player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 1);
            this.cancel();
        }
    }

    public CoordTracker(final Player player, final CoordList plugin, final String currentCoord, final Coord targetCoord) {
        this.plugin = plugin;
        this.targetCoord = targetCoord;
        this.player = player;

        populateMaps();

        final var scoreBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        final var objective = scoreBoard.registerNewObjective(this.TRACKER_OBJECTIVE_NAME, "dummy", Utils.colorize("&8[&aCoord Tracker&8]"));
        final var targetCoordsName = scoreBoard.registerNewTeam("targetCoordsName");
        final var targetCoords = scoreBoard.registerNewTeam("targetCoords");
        final var currentCoords = scoreBoard.registerNewTeam("currentCoords");
        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        targetCoordsName.addEntry(TARGET_COORDS_NAME_KEY);
        targetCoords.addEntry(TARGET_COORDS_KEY);
        currentCoords.addEntry(CURRENT_COORDS_KEY);

        player.setScoreboard(scoreBoard);

        showTracker(objective, currentCoord, targetCoord);
    }

    private void showTracker(final Objective objective, final String currentCoords, final Coord targetCoord) {
        final String howToCancelMsg = Utils.colorize("&7&oUse </coordlist track> to cancel");

        objective.getScore(ChatColor.DARK_BLUE + " ").setScore(5); // Unique empty space
        objective.getScore(TARGET_COORDS_NAME_KEY).setScore(4);
        objective.getScore(TARGET_COORDS_KEY).setScore(3);
        objective.getScore(CURRENT_COORDS_KEY).setScore(2);
        objective.getScore(ChatColor.DARK_PURPLE + " ").setScore(1);
        objective.getScore(howToCancelMsg).setScore(0);
    }

    public void updateTracker(final Player player, final String currentCoords, final Coord targetCoord) {
        final String arrow = getArrowForDirection(player.getLocation(), targetCoord.getLocation());
        final var arrowChatColor = this.arrowToChatColor.get(arrow.substring(0, 2)); // substring extracts the arrow
        final var targetPos = targetCoord.getLocation();
        final String targetCoordsNumbers = (int)floor(targetPos.getX()) + ", " + (int)floor(targetPos.getY()) + ", " + (int)floor(targetPos.getZ());

        final String targetCoordName = ChatColor.AQUA + targetCoord.getName() + ChatColor.GRAY + " (" + (int)floor(this.distanceToTarget) + ") blocks away";
        final String targetCoordPos  = ChatColor.DARK_GRAY + "[" + arrowChatColor + targetCoordsNumbers + ChatColor.DARK_GRAY + "] " + arrowChatColor + arrow;
        final String currentCoordMsg = currentCoords;
        
        final var scoreBoard = player.getScoreboard();
        this.distanceToTarget = player.getLocation().distance(targetCoord.getLocation());
        
        scoreBoard.getTeam("targetCoordsName").setSuffix(targetCoordName);
        scoreBoard.getTeam("targetCoords").setSuffix(targetCoordPos);
        scoreBoard.getTeam("currentCoords").setSuffix(currentCoordMsg);
    }

    // "playerLocation" paramater can be changed to "originLocation" since this funcition does not depend of the Player at all.
    private String getArrowForDirection(Location playerLocation, Location targetLocation) {
        // This took me longer than I expected.
        final var playerDir = playerLocation.getDirection().setY(0).normalize();
        final var playerPos = playerLocation.toVector();
        final var targetPos = targetLocation.toVector();

        final var playerDirToTarget = targetPos.clone().subtract(playerPos).normalize();
        final double playerAngleToTarget = toDegrees(atan2(playerDirToTarget.getX(), playerDirToTarget.getZ()));
        final double playerAngle = toDegrees(atan2(-playerDir.getX(), playerDir.getZ()));

        double targetAngle = (playerAngle + playerAngleToTarget + 90) % 360.0;

        // Keep the angle within the range [0, 360] because Minecraft works in [-180, 180]
        targetAngle = (targetAngle + 360.0) % 360.0;
        
        double nearestAngle = 0.0;
        double nearestAngleDst = 360.0;

        // Find in which range of angles the player direction falls and returns the corresponding arrow
        for (var angleDir: this.angleToArrow.keySet()) {
            double currentDst = targetAngle - angleDir;
            // src: https://stackoverflow.com/questions/1878907/how-can-i-find-the-smallest-difference-between-two-angles-around-a-point
            currentDst = abs(((currentDst + 180.0) % 360.0) - 180.0);

            if (currentDst < nearestAngleDst) {
                nearestAngle = angleDir;
                nearestAngleDst = currentDst;
            }
        }
        
        return this.angleToArrow.get(nearestAngle);
    }

    private void populateMaps() {
        this.angleToArrow = new HashMap<>() {{
            put(Double.NaN, "NaN");
            put(90.0,  "🢁 " + ChatColor.GRAY + "-Z");
            put(45.0,  "🡽 " + ChatColor.GRAY + "+X -Z");
            put(0.0,   "🢂 " + ChatColor.GRAY + "+X");
            put(315.0, "🢆 " + ChatColor.GRAY + "+X +Z");
            put(270.0, "🢃 " + ChatColor.GRAY + "+Z");
            put(225.0, "🢇 " + ChatColor.GRAY + "-X +Z");
            put(180.0, "🢀 " + ChatColor.GRAY + "-X");
            put(135.0, "🢄 " + ChatColor.GRAY + "-X -Z");
        }};

        this.arrowToChatColor = new HashMap<>() {{
            put("🢁", ChatColor.GREEN);
            put("🡽", ChatColor.YELLOW);
            put("🢂", ChatColor.GOLD);
            put("🢆", ChatColor.RED);
            put("🢃", ChatColor.DARK_RED);
            put("🢇", ChatColor.RED);
            put("🢀", ChatColor.GOLD);
            put("🢄", ChatColor.YELLOW);
        }};
    }

}

// [N, -Z]
// [+X, -Z]
// [E, +X]
// [SE, +X +Z]
// [S, +Z]
// [SW, -X +Z]
// [W, -X]
// [NW, -X -Z]