package me.eaz.galacticvotes.managers;

import me.eaz.galacticvotes.GalacticVotes;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes vote/milestone/monthly reward commands and queues rewards for
 * players who voted while offline, backed by queuedrewards.yml.
 */
public class RewardManager {

    private final GalacticVotes plugin;
    private final File queuedFile;

    // one entry per queued vote; each inner list is a full copy of the
    // vote-rewards command templates for that vote
    private final Map<UUID, List<List<String>>> queuedRewards = new HashMap<>();

    public RewardManager(GalacticVotes plugin) {
        this.plugin = plugin;
        this.queuedFile = new File(plugin.getDataFolder(), "queuedrewards.yml");
    }

    public void load() {
        if (!queuedFile.exists()) {
            try {
                queuedFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(queuedFile);

        queuedRewards.clear();

        if (cfg.contains("queued")) {
            ConfigurationSection section = cfg.getConfigurationSection("queued");
            for (String uuidStr : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                List<?> raw = cfg.getList("queued." + uuidStr + ".commands");

                if (raw != null && !raw.isEmpty()) {
                    List<List<String>> batches = new ArrayList<>();
                    for (Object o : raw) {
                        if (o instanceof List) {
                            List<String> batch = new ArrayList<>();
                            for (Object c : (List<?>) o) {
                                batch.add(String.valueOf(c));
                            }
                            batches.add(batch);
                        }
                    }
                    if (!batches.isEmpty()) {
                        queuedRewards.put(uuid, batches);
                    }
                }
            }
        }
    }

    public void save() {
        saveAsync();
    }

    private void saveAsync() {
        Map<UUID, List<List<String>>> copy = new HashMap<>();
        for (Map.Entry<UUID, List<List<String>>> entry : queuedRewards.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(queuedFile);
            for (Map.Entry<UUID, List<List<String>>> entry : copy.entrySet()) {
                cfg.set("queued." + entry.getKey() + ".commands", entry.getValue());
            }
            try {
                cfg.save(queuedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void giveVoteRewards(Player player) {
        List<String> rewardCommands = plugin.getConfig().getStringList("vote-rewards");
        for (String command : buildCommands(rewardCommands, player.getName())) {
            executeCommand(command, player.getName());
        }
    }

    public void queueRewards(UUID uuid, String playerName) {
        List<String> rewardCommands = new ArrayList<>(plugin.getConfig().getStringList("vote-rewards"));
        List<List<String>> batches = queuedRewards.computeIfAbsent(uuid, k -> new ArrayList<>());
        batches.add(rewardCommands);
        save();
    }

    public int giveQueuedRewards(Player player) {
        UUID uuid = player.getUniqueId();
        List<List<String>> batches = queuedRewards.remove(uuid);

        if (batches == null || batches.isEmpty()) {
            return 0;
        }

        for (List<String> commands : batches) {
            for (String command : buildCommands(commands, player.getName())) {
                executeCommand(command, player.getName());
            }
        }

        save();

        return batches.size();
    }

    public boolean hasQueuedRewards(UUID uuid) {
        List<List<String>> batches = queuedRewards.get(uuid);
        return batches != null && !batches.isEmpty();
    }

    public void giveMilestoneRewards(Player player, List<String> commands) {
        for (String command : buildCommands(commands, player.getName())) {
            executeCommand(command, player.getName());
        }
    }

    public void giveMonthlyReward(String playerName, List<String> commands) {
        for (String command : buildCommands(commands, playerName)) {
            executeCommand(command, playerName);
        }
    }

    private List<String> buildCommands(List<String> templates, String playerName) {
        List<String> result = new ArrayList<>();
        for (String tpl : templates) {
            result.add(tpl.replace("%player%", playerName));
        }
        return result;
    }

    private void executeCommand(String command, String playerName) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute reward command: " + e.getMessage());
        }
    }
}
