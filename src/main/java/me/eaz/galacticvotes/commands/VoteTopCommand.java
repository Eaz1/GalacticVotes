package me.eaz.galacticvotes.commands;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VoteTopCommand implements CommandExecutor {

    private final GalacticVotes plugin;

    public VoteTopCommand(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("galacticvotes.votetop")) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        int size = plugin.getConfig().getInt("leaderboard-size", 10);

        sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.votetop-header", "&6&m---&r &6&lTop Voters &6&m---")));

        List<Map.Entry<UUID, Integer>> leaderboard = plugin.getVoteManager().getLifetimeLeaderboard(size);

        if (leaderboard.isEmpty()) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.leaderboard-empty", "&eThere are no votes recorded yet.")));
            return true;
        }

        String entryFormat = plugin.getConfig().getString("messages.leaderboard-entry", "&6#%rank% &e%player%: &6%votes% &evotes");

        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<UUID, Integer> entry = leaderboard.get(i);
            String name = plugin.getVoteManager().getPlayerName(entry.getKey());
            String line = entryFormat.replace("%rank%", String.valueOf(i + 1))
                    .replace("%player%", name)
                    .replace("%votes%", String.valueOf(entry.getValue()));
            sender.sendMessage(MessageUtil.color(line));
        }

        return true;
    }
}
