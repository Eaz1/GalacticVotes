package me.eaz.galacticvotes.managers;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detects month rollover, awards the configured top monthly voters, and
 * resets monthly vote counts. State is persisted in monthlystate.yml.
 */
public class MonthlyManager {

    private final GalacticVotes plugin;
    private final File stateFile;
    private FileConfiguration stateConfig;
    private String lastResetPeriod;

    public MonthlyManager(GalacticVotes plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "monthlystate.yml");
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
        lastResetPeriod = stateConfig.getString("last-reset", currentPeriod());
    }

    public void save() {
        stateConfig.set("last-reset", lastResetPeriod);
        try {
            stateConfig.save(stateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startScheduler() {
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::checkMonthRollover, 20L * 60L, 20L * 60L * 60L);
    }

    public void checkMonthRollover() {
        String current = currentPeriod();

        if (!current.equals(lastResetPeriod)) {
            Bukkit.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[GalacticVotes] Month rollover detected. Awarding top voters and resetting...");
                awardTopMonthlyVoters();
                lastResetPeriod = current;
                plugin.getVoteManager().resetMonthlyVotes();
                save();
                plugin.getLogger().info("[GalacticVotes] Monthly reset complete.");
            });
        }
    }

    public void forceReset() {
        awardTopMonthlyVoters();
        lastResetPeriod = currentPeriod();
        plugin.getVoteManager().resetMonthlyVotes();
        save();
    }

    private void awardTopMonthlyVoters() {
        ConfigurationSection topRewards = plugin.getConfig().getConfigurationSection("monthly-top-rewards");

        if (topRewards == null) {
            return;
        }

        List<Map.Entry<UUID, Integer>> leaderboard =
                plugin.getVoteManager().getMonthlyLeaderboard(topRewards.getKeys(false).size());

        for (String posStr : topRewards.getKeys(false)) {
            int position;
            try {
                position = Integer.parseInt(posStr);
            } catch (NumberFormatException e) {
                continue;
            }

            int idx = position - 1;

            if (idx < 0 || idx >= leaderboard.size()) {
                continue;
            }

            Map.Entry<UUID, Integer> entry = leaderboard.get(idx);
            UUID uuid = entry.getKey();
            int votes = entry.getValue();
            String name = plugin.getVoteManager().getPlayerName(uuid);

            ConfigurationSection posSection = topRewards.getConfigurationSection(posStr);
            List<String> commands = posSection.getStringList("commands");

            plugin.getLogger().info("[Monthly Reward] #" + position + " - " + name + " (" + votes + " votes)");

            plugin.getRewardManager().giveMonthlyReward(name, commands);

            String broadcast = posSection.getString("broadcast", "");
            if (!broadcast.isEmpty()) {
                broadcast = broadcast.replace("%player%", name).replace("%votes%", String.valueOf(votes));
                Bukkit.broadcastMessage(MessageUtil.color(broadcast));
            }
        }
    }

    private String currentPeriod() {
        LocalDate now = LocalDate.now();
        return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    }

    public String getLastResetPeriod() {
        return lastResetPeriod;
    }
}
