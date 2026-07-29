package me.eaz.galacticvotes.commands;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VotesCommand implements CommandExecutor {

    private final GalacticVotes plugin;

    public VotesCommand(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("galacticvotes.votes")) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtil.color("&cPlease specify a player: /votes <player>"));
                return true;
            }

            Player self = (Player) sender;
            int votes = plugin.getVoteManager().getLifetimeVotes(self.getUniqueId());
            String msg = plugin.getConfig().getString("messages.votes-self", "&eYour lifetime votes: &6%votes%");
            sender.sendMessage(MessageUtil.color(msg.replace("%votes%", String.valueOf(votes))));
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = findUUID(targetName);

        if (targetUUID == null) {
            String notFound = plugin.getConfig().getString("messages.player-not-found", "&cPlayer &6%player% &cwas not found.");
            sender.sendMessage(MessageUtil.color(notFound.replace("%player%", targetName)));
            return true;
        }

        int votes = plugin.getVoteManager().getLifetimeVotes(targetUUID);
        String msg = plugin.getConfig().getString("messages.votes-other", "&6%player%'s &elifetime votes: &6%votes%");
        sender.sendMessage(MessageUtil.color(msg.replace("%player%", targetName).replace("%votes%", String.valueOf(votes))));

        return true;
    }

    private UUID findUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }
        return null;
    }
}
