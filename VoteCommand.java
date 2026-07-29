package me.eaz.galacticvotes.commands;

import me.eaz.galacticvotes.GalacticVotes;
import me.eaz.galacticvotes.util.MessageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class VoteCommand implements CommandExecutor {

    private final GalacticVotes plugin;

    public VoteCommand(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.color("&cThis command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("galacticvotes.vote")) {
            player.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        player.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.vote-header", "&6&m---&r &6&lVote Links &6&m---")));

        List<String> links = plugin.getConfig().getStringList("vote-links");

        if (links.isEmpty()) {
            player.sendMessage(MessageUtil.color("&cNo vote links are configured."));
        } else {
            for (int i = 0; i < links.size(); i++) {
                String url = links.get(i);

                TextComponent component = new TextComponent(MessageUtil.color("  &6[Link " + (i + 1) + "] &e" + url));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(MessageUtil.color("&eClick to vote!")).create()));

                player.spigot().sendMessage(component);
            }
        }

        player.sendMessage(MessageUtil.color(plugin.getConfig().getString("messages.vote-footer", "&6&m---&r &eClick a link above to vote! &6&m---")));

        return true;
    }
}
