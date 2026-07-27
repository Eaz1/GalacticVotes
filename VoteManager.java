package me.eaz.galacticvotes.managers;

import me.eaz.galacticvotes.GalacticVotes;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tracks lifetime and monthly vote counts per player, backed by
 * votes.yml and monthlyvotes.yml in the plugin's data folder.
 */
public class VoteManager {

    private final GalacticVotes plugin;

    private final File votesFile;
    private final File monthlyFile;

    private FileConfiguration votesConfig;
    private FileConfiguration monthlyConfig;

    private final Map<UUID, Integer> lifetimeVotes = new HashMap<>();
    private final Map<UUID, Integer> monthlyVotes = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();

    public VoteManager(GalacticVotes plugin) {
        this.plugin = plugin;
        this.votesFile = new File(plugin.getDataFolder(), "votes.yml");
        this.monthlyFile = new File(plugin.getDataFolder(), "monthlyvotes.yml");
    }

    public void load() {
        loadLifetime();
        loadMonthly();
    }

    private void loadLifetime() {
        if (!votesFile.exists()) {
            try {
                votesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        votesConfig = YamlConfiguration.loadConfiguration(votesFile);

        lifetimeVotes.clear();
        playerNames.clear();

        if (votesConfig.contains("players")) {
            ConfigurationSection section = votesConfig.getConfigurationSection("players");
            for (String uuidStr : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int votes = votesConfig.getInt("players." + uuidStr + ".votes");
                String name = votesConfig.getString("players." + uuidStr + ".name", "Unknown");
                lifetimeVotes.put(uuid, votes);
                playerNames.put(uuid, name);
            }
        }
    }

    private void loadMonthly() {
        if (!monthlyFile.exists()) {
            try {
                monthlyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        monthlyConfig = YamlConfiguration.loadConfiguration(monthlyFile);

        monthlyVotes.clear();

        if (monthlyConfig.contains("players")) {
            ConfigurationSection section = monthlyConfig.getConfigurationSection("players");
            for (String uuidStr : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int votes = monthlyConfig.getInt("players." + uuidStr + ".votes");
                monthlyVotes.put(uuid, votes);
            }
        }
    }

    public void save() {
        saveLifetimeAsync();
        saveMonthlyAsync();
    }

    public void saveLifetimeAsync() {
        Map<UUID, Integer> lifetimeCopy = new HashMap<>(lifetimeVotes);
        Map<UUID, String> namesCopy = new HashMap<>(playerNames);

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, Integer> entry : lifetimeCopy.entrySet()) {
                String path = "players." + entry.getKey();
                votesConfig.set(path + ".votes", entry.getValue());
                votesConfig.set(path + ".name", namesCopy.get(entry.getKey()));
            }
            try {
                votesConfig.save(votesFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveMonthlyAsync() {
        Map<UUID, Integer> monthlyCopy = new HashMap<>(monthlyVotes);

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, Integer> entry : monthlyCopy.entrySet()) {
                monthlyConfig.set("players." + entry.getKey() + ".votes", entry.getValue());
            }
            try {
                monthlyConfig.save(monthlyFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public int addVote(UUID uuid, String playerName) {
        int lifetime = lifetimeVotes.merge(uuid, 1, Integer::sum);
        monthlyVotes.merge(uuid, 1, Integer::sum);
        playerNames.put(uuid, playerName);
        save();
        return lifetime;
    }

    public int getLifetimeVotes(UUID uuid) {
        return lifetimeVotes.getOrDefault(uuid, 0);
    }

    public int getMonthlyVotes(UUID uuid) {
        return monthlyVotes.getOrDefault(uuid, 0);
    }

    public void setLifetimeVotes(UUID uuid, String playerName, int amount) {
        lifetimeVotes.put(uuid, amount);
        playerNames.put(uuid, playerName);
        save();
    }

    public void addLifetimeVotes(UUID uuid, String playerName, int amount) {
        int current = lifetimeVotes.getOrDefault(uuid, 0);
        lifetimeVotes.put(uuid, current + amount);
        playerNames.put(uuid, playerName);
        save();
    }

    public void removeLifetimeVotes(UUID uuid, String playerName, int amount) {
        int current = lifetimeVotes.getOrDefault(uuid, 0);
        lifetimeVotes.put(uuid, Math.max(0, current - amount));
        playerNames.put(uuid, playerName);
        save();
    }

    public String getPlayerName(UUID uuid) {
        return playerNames.getOrDefault(uuid, "Unknown");
    }

    public List<Map.Entry<UUID, Integer>> getLifetimeLeaderboard(int size) {
        return lifetimeVotes.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(size)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<UUID, Integer>> getMonthlyLeaderboard(int size) {
        return monthlyVotes.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(size)
                .collect(Collectors.toList());
    }

    public int getLifetimeRank(UUID uuid) {
        List<Map.Entry<UUID, Integer>> leaderboard = getLifetimeLeaderboard(lifetimeVotes.size());
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getMonthlyRank(UUID uuid) {
        List<Map.Entry<UUID, Integer>> leaderboard = getMonthlyLeaderboard(monthlyVotes.size());
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    public void resetMonthlyVotes() {
        monthlyVotes.clear();
        save();
    }

    public Map<UUID, Integer> getMonthlyVotesMap() {
        return Collections.unmodifiableMap(monthlyVotes);
    }
}
