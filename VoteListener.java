package me.eaz.galacticvotes.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * GalacticVotes is the only plugin that handles incoming votes. Every
 * real vote flows through here: lifetime/monthly stats, rewards,
 * milestones, broadcasts, and (new) the server-wide vote party counter
 * that tells GalacticBosses when to spawn its boss.
 */
public class VoteListener implements Listener {

    private final GalacticVotes plugin;

    public VoteListener(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVote(VotifierEvent event) {
        String playerName = event.getVote().getUsername();
        String serviceName = event.getVote().getServiceName();

        plugin.getLogger().info("[Vote] " + playerName + " voted on " + serviceName);

        Bukkit.getScheduler().runTask(plugin, () -> processVote(playerName));
    }

    private void processVote(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);

        if (player != null) {
            UUID uuid = player.getUniqueId();
            int newTotal = plugin.getVoteManager().addVote(uuid, playerName);

            String msg = plugin.getConfig().getString("vote-message", "&6Thank you for voting, &e%player%&6!");
            player.sendMessage(MessageUtil.colorPlayer(msg, playerName));

            plugin.getRewardManager().giveVoteRewards(player);
            plugin.getMilestoneManager().checkMilestones(player, newTotal);

            plugin.getLogger().info(playerName + " is online. Rewards given. Lifetime: " + newTotal);
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = offlinePlayer.getUniqueId();
            int newTotal = plugin.getVoteManager().addVote(uuid, playerName);

            plugin.getRewardManager().queueRewards(uuid, playerName);

            plugin.getLogger().info(playerName + " is offline. Rewards queued. Lifetime: " + newTotal);
        }

        if (plugin.getConfig().getBoolean("broadcast-enabled", true)) {
            String broadcast = plugin.getConfig().getString("vote-broadcast", "&e%player% &6just voted!");
            Bukkit.broadcastMessage(MessageUtil.colorPlayer(broadcast, playerName));
        }

        // Every processed vote (online or offline) counts towards the
        // server-wide vote party. This is the single place in the whole
        // project where the vote party threshold is actually reached.
        plugin.getVotePartyManager().addVote();
    }
}
