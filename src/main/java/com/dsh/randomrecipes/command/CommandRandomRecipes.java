package com.dsh.randomrecipes.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.dsh.randomrecipes.ModConfig;
import com.dsh.randomrecipes.RecipeShuffler;
import com.dsh.randomrecipes.ServerTickHandler;

/** {@code /randrec now|info|list|restore|resume} */
public class CommandRandomRecipes extends CommandBase {

    private static final String[] SUBCOMMANDS = new String[] { "now", "info", "list", "restore", "resume" };

    @Override
    public String getCommandName() {
        return "randrec";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/randrec <now|info|list [n]|restore|resume>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("randomrecipes");
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase();
        if ("now".equals(sub)) {
            if (RecipeShuffler.shuffleRandom()) {
                reply(sender, EnumChatFormatting.GREEN + "Scrambled " + RecipeShuffler.getWrappedCount()
                        + " recipe products (total shuffles: " + RecipeShuffler.getShuffleCount() + ").");
                ServerTickHandler.sync(MinecraftServer.getServer(), RecipeShuffler.getCurrentEpoch());
            } else {
                reply(sender, EnumChatFormatting.RED + "Nothing to shuffle (fewer than two craftable recipes).");
            }
            return;
        }
        if ("restore".equals(sub)) {
            RecipeShuffler.restore();
            reply(sender, EnumChatFormatting.YELLOW + "All recipe products restored; the automatic timer is paused.");
            return;
        }
        if ("resume".equals(sub)) {
            RecipeShuffler.resume();
            reply(sender, EnumChatFormatting.GREEN + "Automatic shuffling resumed.");
            return;
        }
        if ("list".equals(sub)) {
            int limit = 20;
            if (args.length > 1) {
                try {
                    limit = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                    throw new CommandException("Not a number: " + args[1]);
                }
            }
            List<String> lines = RecipeShuffler.describe(limit);
            reply(sender, EnumChatFormatting.GOLD + "Current assignments (" + lines.size() + " of "
                    + RecipeShuffler.getWrappedCount() + "):");
            for (String line : lines) {
                reply(sender, "  " + line);
            }
            return;
        }
        if (!"info".equals(sub)) {
            throw new CommandException("Unknown subcommand: " + sub);
        }
        reply(sender, EnumChatFormatting.GOLD + "Random Recipes status");
        reply(sender, "  enabled: " + ModConfig.enabled + "   interval: " + ModConfig.intervalMinutes + " min"
                + "   paused: " + RecipeShuffler.isSuspended());
        reply(sender, "  wrapped recipes: " + RecipeShuffler.getWrappedCount()
                + "   interval index: " + RecipeShuffler.getCurrentEpoch()
                + "   shuffles so far: " + RecipeShuffler.getShuffleCount());
    }

    private static void reply(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand other) {
        return getCommandName().compareTo(other.getCommandName());
    }
}
