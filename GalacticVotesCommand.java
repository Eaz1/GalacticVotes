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

public class GalacticVotesCommand implements CommandExecutor {

    private final GalacticVotes plugin;

    public GalacticVotesCommand(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("galacticvotes.admin")) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                plugin.reload();
                sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.reload-success", "&aGalacticVotes configuration reloaded.")));
                return true;

            case "resetmonth":
                plugin.getMonthlyManager().forceReset();
                sender.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.month-reset-success", "&aMonthly votes have been reset.")));
                return true;

            case "addvotes":
            case "setvotes":
            case "removevotes": {
                if (args.length < 3) {
                    sendUsage(sender);
                    return true;
                }

                String targetName = args[1];
                int amount = parseAmount(sender, args[2]);
                if (amount < 0) {
                    return true;
                }

                UUID uuid = resolveUUID(targetName);
                if (uuid == null) {
                    String msg = plugin.getConfig().getString("messages.player-not-found", "&cPlayer &6%player% &cwas not found.");
                    sender.sendMessage(MessageUtil.color(msg.replace("%player%", targetName)));
                    return true;
                }

                if (sub.equals("addvotes")) {
                    plugin.getVoteManager().addLifetimeVotes(uuid, targetName, amount);
                    String msg = plugin.getConfig().getString("messages.votes-added", "&aAdded &6%amount% &avotes to &6%player%&a.");
                    sender.sendMessage(MessageUtil.color(msg.replace("%amount%", String.valueOf(amount)).replace("%player%", targetName)));
                } else if (sub.equals("setvotes")) {
                    plugin.getVoteManager().setLifetimeVotes(uuid, targetName, amount);
                    String msg = plugin.getConfig().getString("messages.votes-set", "&aSet &6%player%'s &alifetime votes to &6%votes%&a.");
                    sender.sendMessage(MessageUtil.color(msg.replace("%player%", targetName).replace("%votes%", String.valueOf(amount))));
                } else {
                    plugin.getVoteManager().removeLifetimeVotes(uuid, targetName, amount);
                    String msg = plugin.getConfig().getString("messages.votes-removed", "&aRemoved &6%amount% &avotes from &6%player%&a.");
                    sender.sendMessage(MessageUtil.color(msg.replace("%amount%", String.valueOf(amount)).replace("%player%", targetName)));
                }
                return true;
            }

            case "voteparty": {
                int current = plugin.getVotePartyManager().getCurrentVotes();
                int required = plugin.getVotePartyManager().getVotesRequired();
                sender.sendMessage(MessageUtil.color("&d&lVote Party &8» &e" + current + "&7/&e" + required + " &evotes."));
                return true;
            }

            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MessageUtil.color("&6GalacticVotes Admin Commands:"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes reload"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes resetmonth"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes voteparty"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes addvotes <player> <amount>"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes setvotes <player> <amount>"));
        sender.sendMessage(MessageUtil.color("&e/galacticvotes removevotes <player> <amount>"));
    }

    private int parseAmount(CommandSender sender, String str) {
        try {
            int val = Integer.parseInt(str);
            if (val < 0) {
                sender.sendMessage(MessageUtil.color("&cAmount must be a non-negative integer."));
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.color("&c'" + str + "' is not a valid number."));
            return -1;
        }
    }

    private UUID resolveUUID(String name) {
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
