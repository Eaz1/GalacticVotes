package me.eaz.galacticvotes.managers;

import me.eaz.galacticbosses.GalacticBosses;
import me.eaz.galacticbosses.bosses.BossManager;
import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * GalacticVotes is the ONLY plugin that tracks votes. This manager counts
 * every processed vote towards a server-wide "vote party" and, once the
 * configured threshold (config: vote-party.votes-required) is reached, it
 * directly calls GalacticBosses' existing BossManager API to spawn the
 * boss - no custom event system and no listener inside GalacticBosses is
 * involved.
 *
 * The current vote party progress is persisted in voteparty.yml so it
 * survives restarts.
 */
public class VotePartyManager {

    private final GalacticVotes plugin;
    private final File stateFile;
    private FileConfiguration stateConfig;

    private int currentVotes;

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
    }

    public void save() {
        stateConfig.set("current-votes", currentVotes);
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
        save();
    }

    /**
     * Called once for every successfully processed vote (see
     * VoteListener#processVote). Increments the vote party counter and,
     * once the threshold is reached, asks GalacticBosses to spawn its
     * boss directly.
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
            boolean spawned = requestBossSpawn();
            if (spawned) {
                currentVotes = 0;
            }
        }

        save();
    }

    /**
     * Directly tells GalacticBosses to spawn its boss using its existing
     * BossManager API. Reads the spawn location and spawn broadcast from
     * GalacticBosses' own config.yml, since that plugin still owns
     * everything about the boss itself.
     *
     * @return true if the boss was spawned, false if GalacticBosses is
     *         unavailable or a boss is already active.
     */
    private boolean requestBossSpawn() {
        Plugin found = Bukkit.getPluginManager().getPlugin("GalacticBosses");

        if (!(found instanceof GalacticBosses) || !found.isEnabled()) {
            plugin.getLogger().warning("Vote party threshold reached, but GalacticBosses is not installed or enabled!");
            return false;
        }

        GalacticBosses bosses = (GalacticBosses) found;
        BossManager bossManager = bosses.getBossManager();

        if (bossManager.hasBoss()) {
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

        bossManager.spawnBoss(spawnLocation);

        String prefix = bosses.getConfig().getString("messages.prefix", "");
        String spawnMessage = bosses.getConfig().getString("messages.boss-spawn", "&5&lThe Galactic Wither has appeared!");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + spawnMessage));

        plugin.getLogger().info("Vote party threshold reached - GalacticBosses spawned the boss.");

        return true;
    }
}
