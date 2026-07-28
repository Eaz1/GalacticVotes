package me.eaz.galacticvotes.placeholders;

import me.eaz.galacticvotes.GalacticVotes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class GalacticVotesExpansion extends PlaceholderExpansion {

    private final GalacticVotes plugin;

    public GalacticVotesExpansion(GalacticVotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "galacticvotes";
    }

    @Override
    public String getAuthor() {
        return "eaz";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "total":
                return String.valueOf(plugin.getVoteManager().getLifetimeVotes(player.getUniqueId()));

            case "monthly":
                return String.valueOf(plugin.getVoteManager().getMonthlyVotes(player.getUniqueId()));

            case "rank": {
                int rank = plugin.getVoteManager().getLifetimeRank(player.getUniqueId());
                return rank == -1 ? "Unranked" : String.valueOf(rank);
            }

            case "monthrank": {
                int rank = plugin.getVoteManager().getMonthlyRank(player.getUniqueId());
                return rank == -1 ? "Unranked" : String.valueOf(rank);
            }

            case "nextreward": {
                int lifetime = plugin.getVoteManager().getLifetimeVotes(player.getUniqueId());
                int next = plugin.getMilestoneManager().getNextMilestone(player.getUniqueId(), lifetime);
                return next == -1 ? "None" : String.valueOf(next);
            }

            case "party_current":
                return String.valueOf(plugin.getVotePartyManager().getCurrentVotes());

            case "party_required":
                return String.valueOf(plugin.getVotePartyManager().getVotesRequired());

            default:
                return null;
        }
    }
}
