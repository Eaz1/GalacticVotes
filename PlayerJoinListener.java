package me.eaz.galacticvotes.listeners;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final GalacticVotes plugin;

    public PlayerJoinListener(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getRewardManager().hasQueuedRewards(player.getUniqueId())) {
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
                int batches = plugin.getRewardManager().giveQueuedRewards(player);

                String msg = plugin.getConfig().getString("messages.queued-rewards-given",
                        "&eWelcome back! You have &6%count% &evote reward(s) waiting for you.");
                msg = msg.replace("%count%", String.valueOf(batches));
                player.sendMessage(MessageUtil.color(msg));
            }, 20L);
        }

        int lifetime = plugin.getVoteManager().getLifetimeVotes(player.getUniqueId());
        plugin.getMilestoneManager().checkMilestones(player, lifetime);
    }
}
