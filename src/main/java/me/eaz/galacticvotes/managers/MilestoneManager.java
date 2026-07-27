package me.eaz.galacticvotes.managers;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Awards one-time rewards when a player reaches configured lifetime-vote
 * milestones (config: vote-milestones), backed by claimedmilestones.yml.
 */
public class MilestoneManager {

    private final GalacticVotes plugin;
    private final File claimedFile;
    private final Map<UUID, Set<Integer>> claimedMilestones = new HashMap<>();

    public MilestoneManager(GalacticVotes plugin) {
        this.plugin = plugin;
        this.claimedFile = new File(plugin.getDataFolder(), "claimedmilestones.yml");
    }

    public void load() {
        if (!claimedFile.exists()) {
            try {
                claimedFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(claimedFile);

        claimedMilestones.clear();

        if (cfg.contains("claimed")) {
            ConfigurationSection section = cfg.getConfigurationSection("claimed");
            for (String uuidStr : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                List<Integer> milestones = cfg.getIntegerList("claimed." + uuidStr + ".milestones");
                claimedMilestones.put(uuid, new HashSet<>(milestones));
            }
        }
    }

    public void save() {
        saveAsync();
    }

    private void saveAsync() {
        Map<UUID, Set<Integer>> copy = new HashMap<>();
        for (Map.Entry<UUID, Set<Integer>> entry : claimedMilestones.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(claimedFile);
            for (Map.Entry<UUID, Set<Integer>> entry : copy.entrySet()) {
                cfg.set("claimed." + entry.getKey() + ".milestones", new ArrayList<>(entry.getValue()));
            }
            try {
                cfg.save(claimedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void checkMilestones(Player player, int lifetimeVotes) {
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("vote-milestones");

        if (milestonesSection == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Set<Integer> claimed = claimedMilestones.computeIfAbsent(uuid, k -> new HashSet<>());

        boolean changed = false;

        for (String key : milestonesSection.getKeys(false)) {
            int milestone;
            try {
                milestone = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid milestone key in config: " + key);
                continue;
            }

            if (lifetimeVotes >= milestone && !claimed.contains(milestone)) {
                awardMilestone(player, milestone, milestonesSection.getConfigurationSection(key));
                claimed.add(milestone);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    private void awardMilestone(Player player, int milestone, ConfigurationSection section) {
        plugin.getLogger().info("[Milestone] " + player.getName() + " reached " + milestone + " lifetime votes.");

        List<String> commands = section.getStringList("commands");
        List<String> resolved = replacePlayer(commands, player.getName());

        plugin.getRewardManager().giveMilestoneRewards(player, resolved);

        String broadcast = section.getString("broadcast", "");
        if (!broadcast.isEmpty()) {
            Bukkit.broadcastMessage(MessageUtil.colorPlayer(broadcast, player.getName()));
        }
    }

    private List<String> replacePlayer(List<String> commands, String playerName) {
        List<String> result = new ArrayList<>();
        for (String cmd : commands) {
            result.add(cmd.replace("%player%", playerName));
        }
        return result;
    }

    public int getNextMilestone(UUID uuid, int currentVotes) {
        ConfigurationSection milestonesSection = plugin.getConfig().getConfigurationSection("vote-milestones");

        if (milestonesSection == null) {
            return -1;
        }

        int next = -1;

        for (String key : milestonesSection.getKeys(false)) {
            int milestone;
            try {
                milestone = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }

            if (milestone > currentVotes) {
                next = (next == -1) ? milestone : Math.min(next, milestone);
            }
        }

        return next;
    }
}
