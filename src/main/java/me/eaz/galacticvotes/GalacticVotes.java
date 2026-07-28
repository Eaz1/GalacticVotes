aa
    A
    package me.eaz.galacticvotes;

import me.eaz.galacticvotes.commands.GalacticVotesCommand;
import me.eaz.galacticvotes.commands.VoteCommand;
import me.eaz.galacticvotes.commands.VoteMonthCommand;
import me.eaz.galacticvotes.commands.VoteTopCommand;
import me.eaz.galacticvotes.commands.VotesCommand;
import me.eaz.galacticvotes.listeners.PlayerJoinListener;
import me.eaz.galacticvotes.listeners.VoteListener;
import me.eaz.galacticvotes.managers.MilestoneManager;
import me.eaz.galacticvotes.managers.MonthlyManager;
import me.eaz.galacticvotes.managers.RewardManager;
import me.eaz.galacticvotes.managers.VoteManager;
import me.eaz.galacticvotes.managers.VotePartyManager;
import me.eaz.galacticvotes.placeholders.GalacticVotesExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class GalacticVotes extends JavaPlugin {

    private static GalacticVotes instance;

    private VoteManager voteManager;
    private RewardManager rewardManager;
    private MilestoneManager milestoneManager;
    private MonthlyManager monthlyManager;
    private VotePartyManager votePartyManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        voteManager = new VoteManager(this);
        voteManager.load();

        rewardManager = new RewardManager(this);
        rewardManager.load();

        milestoneManager = new MilestoneManager(this);
        milestoneManager.load();

        monthlyManager = new MonthlyManager(this);
        monthlyManager.load();

        votePartyManager = new VotePartyManager(this);
        votePartyManager.load();

        Bukkit.getPluginManager().registerEvents(new VoteListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        if (getCommand("vote") != null) {
            getCommand("vote").setExecutor(new VoteCommand(this));
        }
        if (getCommand("votes") != null) {
            getCommand("votes").setExecutor(new VotesCommand(this));
        }
        if (getCommand("votetop") != null) {
            getCommand("votetop").setExecutor(new VoteTopCommand(this));
        }
        if (getCommand("votemonth") != null) {
            getCommand("votemonth").setExecutor(new VoteMonthCommand(this));
        }
        if (getCommand("galacticvotes") != null) {
            getCommand("galacticvotes").setExecutor(new GalacticVotesCommand(this));
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GalacticVotesExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        if (Bukkit.getPluginManager().getPlugin("GalacticBosses") == null) {
            getLogger().warning("GalacticBosses was not found. The vote party will track progress, "
                    + "but no boss will spawn until GalacticBosses is installed.");
        }

        monthlyManager.startScheduler();

        getLogger().info("GalacticVotes v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.save();
        }
        if (rewardManager != null) {
            rewardManager.save();
        }
        if (milestoneManager != null) {
            milestoneManager.save();
        }
        if (monthlyManager != null) {
            monthlyManager.save();
        }
        if (votePartyManager != null) {
            votePartyManager.save();
        }

        getLogger().info("GalacticVotes has been disabled. All data saved.");
    }

    public void reload() {
        reloadConfig();
    }

    public static GalacticVotes getInstance() {
        return instance;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MilestoneManager getMilestoneManager() {
        return milestoneManager;
    }

    public MonthlyManager getMonthlyManager() {
        return monthlyManager;
    }

    public VotePartyManager getVotePartyManager() {
        return votePartyManager;
    }
}
