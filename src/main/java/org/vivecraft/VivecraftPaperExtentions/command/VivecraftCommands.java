package org.vivecraft.VivecraftPaperExtentions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Iterator;
import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class VivecraftCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("vpe")
                        .executes(VivecraftCommands::executeMainCommand)
                        .then(literal("help")
                                .executes(VivecraftCommands::executeHelp))
                        .then(literal("version")
                                .executes(VivecraftCommands::executeVersion))
                        .then(literal("list")
                                .executes(VivecraftCommands::executeList))
                        .then(literal("vive-only")
                                .requires(source -> hasPermission(source.getSender()))
                                .executes(VivecraftCommands::executeViveOnlyStatus)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(VivecraftCommands::executeViveOnly)))
                        .then(literal("sendplayerdata")
                                .requires(source -> hasPermission(source.getSender()))
                                .executes(VivecraftCommands::executeSendPlayerDataStatus)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(VivecraftCommands::executeSendPlayerData)))
                        .then(literal("creeperradius")
                                .requires(source -> hasPermission(source.getSender()))
                                .executes(VivecraftCommands::executeCreeperRadiusStatus)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(VivecraftCommands::executeCreeperRadiusToggle))
                                .then(argument("radius", DoubleArgumentType.doubleArg(0.0))
                                        .executes(VivecraftCommands::executeCreeperRadiusSet)))
                        .then(literal("waittime")
                                .requires(source -> hasPermission(source.getSender()))
                                .executes(VivecraftCommands::executeWaitTimeStatus)
                                .then(argument("ticks", IntegerArgumentType.integer(1))
                                        .executes(VivecraftCommands::executeWaitTime)))
                        .then(literal("bow")
                                .requires(source -> hasPermission(source.getSender()))
                                .executes(VivecraftCommands::executeBowStatus)
                                .then(argument("multiplier", IntegerArgumentType.integer(1))
                                        .executes(VivecraftCommands::executeBow)))
                        .then(literal("set")
                                .requires(source -> hasPermission(source.getSender()))
                                .then(argument("config_path", StringArgumentType.word())
                                        .then(argument("value", StringArgumentType.greedyString())
                                                .executes(VivecraftCommands::executeSet))))
        );
    }

    private static boolean hasPermission(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("vive.command");
    }

    private static VPE getPlugin() {
        return VPE.me;
    }

    private static int executeMainCommand(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sendMessage(sender, "Download Vivecraft at http://www.vivecraft.org/ type '/vpe help' to list options");
        return SINGLE_SUCCESS;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage(Component.text("-------------- ", NamedTextColor.BLUE)
                .append(Component.text("VPE Commands", NamedTextColor.GRAY))
                .append(Component.text(" --------------", NamedTextColor.BLUE)));

        sendHelpCommand(sender, "vive-only", "Set to true to only allow Vivecraft players to play. Default: false", "Example: /vpe vive-only true");
        sendHelpCommand(sender, "waittime", "Ticks to wait before kicking a player. Default: 60", "Example: /vpe waittime 60");
        sendHelpCommand(sender, "version", "returns the version of the plugin.", "Example: /vpe version");
        sendHelpCommand(sender, "sendplayerdata", "set to false to disable sending player data to clients. Default: true", "Example: /vpe sendplayerdata true");
        sendHelpCommand(sender, "creeperradius", "type false to disable or type a number to change the radius. Default: 1.75", "Example: /vpe creeperradius 1.75");
        sendHelpCommand(sender, "bow", "Sets the multiplier for bow damage of vive users. Default: 2", "Example: /vpe bow 2");
        sendHelpCommand(sender, "set", "Allows Editing the plugin config ingame. May need to restart server to take effect.", "Example: /vpe set general.vive-only true");
        sendHelpCommand(sender, "list", "Lists all the users using Vivecraft.", "Example: /vpe list");

        return SINGLE_SUCCESS;
    }

    private static void sendHelpCommand(CommandSender sender, String command, String description, String example) {
        Component message = Component.text(command + ": ", NamedTextColor.BLUE)
                .append(Component.text(description, NamedTextColor.WHITE))
                .hoverEvent(HoverEvent.showText(Component.text(example, NamedTextColor.BLUE)));

        sender.sendMessage(message);
    }

    private static int executeVersion(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            PluginDescriptionFile pdf = plugin.getDescription();
            String version = pdf.getVersion();
            sendMessage(sender, "Version: " + version);
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        if (VPE.vivePlayers == null) {
            sendMessage(sender, "Plugin not yet initialized");
            return SINGLE_SUCCESS;
        }

        Iterator<Map.Entry<java.util.UUID, VivePlayer>> it = VPE.vivePlayers.entrySet().iterator();
        int size = VPE.vivePlayers.size();
        int total = Bukkit.getOnlinePlayers().size();

        sendMessage(sender, "There are " + total + " players online");

        if (size > 1) {
            sendMessage(sender, "There are " + size + " Vivecraft Players");
        } else if (size == 1) {
            sendMessage(sender, "There is 1 Vivecraft Player");
        } else {
            sendMessage(sender, "There are no Vivecraft players");
        }

        while (it.hasNext()) {
            Map.Entry<java.util.UUID, VivePlayer> pair = it.next();
            VivePlayer vp = pair.getValue();
            sendMessage(sender, vp.player.getDisplayName() + ": " +
                    (vp.isVR() ? "VR " + (vp.isSeated() ? "SEATED" : "STANDING") : "NONVR"));
        }

        return SINGLE_SUCCESS;
    }

    private static int executeViveOnlyStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            sendMessage(sender, "Vive-Only: " + plugin.getConfig().getBoolean("general.vive-only"));
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeViveOnly(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("general.vive-only", enabled);
            plugin.saveConfig();
            sendMessage(sender, "Vive-Only has been " + (enabled ? "enabled" : "disabled") + ".");
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeSendPlayerDataStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            sendMessage(sender, "SendPlayerData: " + plugin.getConfig().getBoolean("general.sendplayerdata", true));
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeSendPlayerData(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("general.sendplayerdata", enabled);
            plugin.saveConfig();
            sendMessage(sender, "SendPlayerData has been " + (enabled ? "enabled" : "disabled") + ".");
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeCreeperRadiusStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            sendMessage(sender, "Creeper Radius: " + plugin.getConfig().getBoolean("creeper.enabled", true) +
                    " Radius set to: " + plugin.getConfig().getDouble("creeper.radius", 1.75));
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeCreeperRadiusToggle(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("creeper.enabled", enabled);
            plugin.saveConfig();
            sendMessage(sender, "Creeper Radius has been " + (enabled ? "enabled" : "disabled") + ".");
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeCreeperRadiusSet(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        double radius = DoubleArgumentType.getDouble(context, "radius");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("creeper.enabled", true);
            plugin.getConfig().set("creeper.radius", radius);
            plugin.saveConfig();
            sendMessage(sender, "Creeper Radius set to " + radius);
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeWaitTimeStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            sendMessage(sender, "waittime: " + plugin.getConfig().getInt("general.vive-only-kickwaittime", 200));
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeWaitTime(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("general.vive-only-kickwaittime", ticks);
            plugin.saveConfig();
            sendMessage(sender, "waittime set to " + ticks);
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeBowStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        VPE plugin = getPlugin();
        if (plugin != null) {
            sendMessage(sender, "Multiplier: " + plugin.getConfig().getInt("bow.multiplier", 2));
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeBow(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        int multiplier = IntegerArgumentType.getInteger(context, "multiplier");
        VPE plugin = getPlugin();
        if (plugin != null) {
            plugin.getConfig().set("bow.multiplier", multiplier);
            plugin.saveConfig();
            sendMessage(sender, "Multiplier set to " + multiplier);
        } else {
            sendMessage(sender, "Plugin not yet initialized");
        }
        return SINGLE_SUCCESS;
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String configPath = StringArgumentType.getString(context, "config_path");
        String value = StringArgumentType.getString(context, "value");

        VPE plugin = getPlugin();
        if (plugin == null) {
            sendMessage(sender, "Plugin not yet initialized");
            return SINGLE_SUCCESS;
        }

        if (plugin.getConfig().get(configPath) != null) {
            // Определяем тип значения и устанавливаем соответствующий тип в конфиге
            if (isBoolean(value)) {
                plugin.getConfig().set(configPath, Boolean.parseBoolean(value));
            } else if (isInteger(value)) {
                plugin.getConfig().set(configPath, Integer.parseInt(value));
            } else if (isDouble(value)) {
                plugin.getConfig().set(configPath, Double.parseDouble(value));
            } else {
                plugin.getConfig().set(configPath, value);
            }
            plugin.saveConfig();
            sendMessage(sender, configPath + " has been set to: " + plugin.getConfig().get(configPath));
        } else {
            sendMessage(sender, "That config option does not exist.");
        }

        return SINGLE_SUCCESS;
    }

    private static boolean isBoolean(String str) {
        return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false");
    }

    private static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String str) {
        if (isInteger(str)) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        Component component = Component.text("[", NamedTextColor.BLUE)
                .append(Component.text("Vivecraft", NamedTextColor.GRAY))
                .append(Component.text("] ", NamedTextColor.BLUE))
                .append(Component.text(message, NamedTextColor.WHITE));

        sender.sendMessage(component);
    }
}