package me.eaz.galacticvotes.managers;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * GalacticVotes is the ONLY plugin that tracks votes. This manager counts
 * every processed vote towards a server-wide "vote party" and, once the
 * configured threshold (config: vote-party.votes-required) is reached, it
 * tells GalacticBosses to spawn its boss - no custom event system and no
 * listener inside GalacticBosses is involved.
 * <p>
 * IMPORTANT: this class does NOT import any GalacticBosses classes. It
 * talks to GalacticBosses purely via Bukkit's plugin manager and Java
 * reflection, so GalacticVotes has no compile-time dependency on
 * GalacticBosses at all - it builds and runs standalone (e.g. in CI)
 * whether or not GalacticBosses is present on the classpath or the
 * server. If GalacticBosses is missing, or any reflective call fails for
 * any reason, this manager logs a warning and simply skips spawning the
 * boss - it never throws or crashes the plugin.
 * <p>
 * The current vote party progress is persisted in voteparty.yml so it
 * survives restarts.
 */
public class VotePartyManager {

    private static final String GALACTIC_BOSSES_PLUGIN_NAME = "GalacticBosses";

    private final GalacticVotes plugin;
    private final File stateFile;
    private FileConfiguration stateConfig;

    private int currentVotes;
    private boolean rewardsGiven;

    public VotePartyManager(GalacticVotes plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "voteparty.yml");
    }

    public void load() {
        if (!stateFile.exists()) {
            try {
                stateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        stateConfig = YamlConfiguration.loadConfiguration(stateFile);
        currentVotes = stateConfig.getInt("current-votes", 0);
        rewardsGiven = stateConfig.getBoolean("rewards-given", false);
    }

    public void save() {
        stateConfig.set("current-votes", currentVotes);
        stateConfig.set("rewards-given", rewardsGiven);
        try {
            stateConfig.save(stateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentVotes() {
        return currentVotes;
    }

    public int getVotesRequired() {
        return Math.max(1, plugin.getConfig().getInt("vote-party.votes-required", 20));
    }

    public void resetVotes() {
        currentVotes = 0;
        rewardsGiven = false;
        save();
    }

    /**
     * Called once for every successfully processed vote (see
     * VoteListener#processVote). Increments the vote party counter and,
     * once the threshold is reached, asks GalacticBosses to spawn its
     * boss.
     */
    public void addVote() {
        if (!plugin.getConfig().getBoolean("vote-party.enabled", true)) {
            return;
        }

        currentVotes++;

        int required = getVotesRequired();

        if (currentVotes < required) {
            if (plugin.getConfig().getBoolean("vote-party.broadcast-progress", true)) {
                String msg = plugin.getConfig().getString("vote-party.progress-message",
                        "&d&lVote Party &8» &e%current%&7/&e%required% &evotes until the &5Galactic Wither &espawns!");
                msg = msg.replace("%current%", String.valueOf(currentVotes))
                         .replace("%required%", String.valueOf(required));
                Bukkit.broadcastMessage(MessageUtil.color(msg));
            }
        } else {
            if (!rewardsGiven) {
                giveVotePartyRewards();
                rewardsGiven = true;
            }

            boolean spawned = requestBossSpawn();
            if (spawned) {
                currentVotes = 0;
                rewardsGiven = false;
            }
        }

        save();
    }

    /**
     * Executes the configurable vote-party reward commands (config:
     * vote-party.rewards) for every online player once the threshold is
     * reached, and broadcasts vote-party.reward-broadcast if set. This
     * fires exactly once per threshold crossing (see the rewardsGiven
     * guard in addVote), independently of whether GalacticBosses is able
     * to spawn its boss right away.
     */
    private void giveVotePartyRewards() {
        List<String> rewardCommands = plugin.getConfig().getStringList("vote-party.rewards");

        if (!rewardCommands.isEmpty()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                for (String template : rewardCommands) {
                    String command = template.replace("%player%", online.getName());
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to execute vote party reward command: " + e.getMessage());
                    }
                }
            }
        }

        String rewardBroadcast = plugin.getConfig().getString("vote-party.reward-broadcast", "");
        if (!rewardBroadcast.isEmpty()) {
            Bukkit.broadcastMessage(MessageUtil.color(rewardBroadcast));
        }
    }

    /**
     * Tells GalacticBosses to spawn its boss, entirely through reflection
     * so this class has no compile-time dependency on GalacticBosses.
     * Reads the spawn location and spawn broadcast from GalacticBosses'
     * own config.yml, since that plugin still owns everything about the
     * boss itself.
     *
     * @return true if the boss was spawned, false if GalacticBosses is
     *         unavailable, a boss is already active, or anything about
     *         the reflective call failed.
     */
    private boolean requestBossSpawn() {
        Plugin found = Bukkit.getPluginManager().getPlugin(GALACTIC_BOSSES_PLUGIN_NAME);

        if (found == null || !found.isEnabled()) {
            plugin.getLogger().warning("Vote party threshold reached, but GalacticBosses is not installed or enabled! "
                    + "Vote party progress will be kept, but no boss will spawn.");
            return false;
        }

        // GalacticBosses is a standard Bukkit plugin (extends JavaPlugin),
        // so we can safely use getConfig() from the Spigot API itself
        // without needing anything GalacticBosses-specific.
        if (!(found instanceof JavaPlugin)) {
            plugin.getLogger().warning("GalacticBosses was found but is not a JavaPlugin - cannot spawn the boss.");
            return false;
        }

        JavaPlugin bosses = (JavaPlugin) found;

        try {
            // Object bossManager = bosses.getBossManager();
            Method getBossManagerMethod = found.getClass().getMethod("getBossManager");
            Object bossManager = getBossManagerMethod.invoke(found);

            if (bossManager == null) {
                plugin.getLogger().warning("GalacticBosses.getBossManager() returned null - cannot spawn the boss.");
                return false;
            }

            // boolean hasBoss = bossManager.hasBoss();
            Method hasBossMethod = bossManager.getClass().getMethod("hasBoss");
            boolean hasBoss = (Boolean) hasBossMethod.invoke(bossManager);

            if (hasBoss) {
                plugin.getLogger().info("Vote party threshold reached, but a Galactic boss is already active. Waiting for it to be defeated.");
                return false;
            }

            String worldName = bosses.getConfig().getString("boss.spawn.world", "world");
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning("Cannot spawn the vote party boss: world '" + worldName + "' is not loaded.");
                return false;
            }

            double x = bosses.getConfig().getDouble("boss.spawn.x", 0);
            double y = bosses.getConfig().getDouble("boss.spawn.y", 100);
            double z = bosses.getConfig().getDouble("boss.spawn.z", 0);

            Location spawnLocation = new Location(world, x, y, z);

            // bossManager.spawnBoss(spawnLocation);
            Method spawnBossMethod = bossManager.getClass().getMethod("spawnBoss", Location.class);
            spawnBossMethod.invoke(bossManager, spawnLocation);

            String prefix = bosses.getConfig().getString("messages.prefix", "");
            String spawnMessage = bosses.getConfig().getString("messages.boss-spawn", "&5&lThe Galactic Wither has appeared!");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + spawnMessage));

            plugin.getLogger().info("Vote party threshold reached - GalacticBosses spawned the boss.");

            return true;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Could not reflectively call into GalacticBosses (is it an unexpected/older version?): " + e);
            return false;
        } catch (Exception e) {
            // Defensive catch-all: the vote party must never crash the
            // server just because GalacticBosses misbehaved.
            plugin.getLogger().warning("Unexpected error while asking GalacticBosses to spawn the boss: " + e);
            return false;
        }
    }
}
