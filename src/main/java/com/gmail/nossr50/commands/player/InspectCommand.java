package com.gmail.nossr50.commands.player;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.scoreboards.ScoreboardManager;
import com.gmail.nossr50.util.skills.SkillTools;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class InspectCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (args.length == 1) {
            String playerName = CommandUtils.getMatchedPlayerName(args[0]);
            final McMMOPlayer mmoPlayer = UserManager.getOfflinePlayer(playerName);

            // If the mmoPlayer doesn't exist, create a temporary profile and check if it's present in the database. If it's not, abort the process.
            if (mmoPlayer == null) {
                PlayerProfile profile = mcMMO.getDatabaseManager()
                        .loadPlayerProfile(playerName); // Temporary Profile

                if (!CommandUtils.isLoaded(sender, profile)) {
                    return true;
                }

                if (mcMMO.p.getGeneralConfig().getScoreboardsEnabled()
                        && sender instanceof Player
                        && mcMMO.p.getGeneralConfig().getInspectUseBoard()) {
                    ScoreboardManager.enablePlayerInspectScoreboard((Player) sender, profile);

                    if (!mcMMO.p.getGeneralConfig().getInspectUseChat()) {
                        return true;
                    }
                }

                sender.sendMessage(LocaleLoader.getString("Inspect.OfflineStats", playerName));

                sender.sendMessage(LocaleLoader.getString("Stats.Header.Gathering"));
                for (PrimarySkillType skill : mcMMO.p.getSkillTools().GATHERING_SKILLS) {
                    sender.sendMessage(CommandUtils.displaySkill(profile, skill));
                }

                sender.sendMessage(LocaleLoader.getString("Stats.Header.Combat"));
                for (PrimarySkillType skill : mcMMO.p.getSkillTools().COMBAT_SKILLS) {
                    sender.sendMessage(CommandUtils.displaySkill(profile, skill));
                }

                sender.sendMessage(LocaleLoader.getString("Stats.Header.Misc"));
                for (PrimarySkillType skill : mcMMO.p.getSkillTools().MISC_SKILLS) {
                    sender.sendMessage(CommandUtils.displaySkill(profile, skill));
                }

                // Sum power level
                int powerLevel = 0;
                for (PrimarySkillType skill : SkillTools.NON_CHILD_SKILLS) {
                    powerLevel += profile.getSkillLevel(skill);
                }

                sender.sendMessage(LocaleLoader.getString("Commands.PowerLevel", powerLevel));
            } else {
                Player target = mmoPlayer.getPlayer();
                boolean isVanished = CommandUtils.hidden(sender, target,
                        Permissions.inspectHidden(sender));

                //Only distance check players who are online and not vanished
                if (!isVanished && CommandUtils.tooFar(sender, target,
                        Permissions.inspectFar(sender))) {
                    return true;
                }

                if (mcMMO.p.getGeneralConfig().getScoreboardsEnabled()
                        && sender instanceof Player
                        && mcMMO.p.getGeneralConfig().getInspectUseBoard()) {
                    ScoreboardManager.enablePlayerInspectScoreboard((Player) sender, mmoPlayer);

                    if (!mcMMO.p.getGeneralConfig().getInspectUseChat()) {
                        return true;
                    }
                }

                if (isVanished) {
                    sender.sendMessage(LocaleLoader.getString("Inspect.OfflineStats", playerName));
                } else {
                    sender.sendMessage(LocaleLoader.getString("Inspect.Stats", target.getName()));
                }

                CommandUtils.printGatheringSkills(target, sender);
                CommandUtils.printCombatSkills(target, sender);
                CommandUtils.printMiscSkills(target, sender);

                sender.sendMessage(
                        LocaleLoader.getString("Commands.PowerLevel", mmoPlayer.getPowerLevel()));
            }

            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = CommandUtils.getOnlinePlayerNames(sender);
            return StringUtil.copyPartialMatches(args[0], playerNames,
                    new ArrayList<>(playerNames.size()));
        }
        return ImmutableList.of();
    }
}
