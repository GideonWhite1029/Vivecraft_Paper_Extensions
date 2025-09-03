package org.vivecraft.VivecraftPaperExtentions;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vivecraft.VivecraftPaperExtentions.command.VivecraftCommands;
import org.vivecraft.VivecraftPaperExtentions.entities.CustomEndermanFreezeWhenLookedAt;
import org.vivecraft.VivecraftPaperExtentions.entities.CustomEndermanLookForPlayerGoal;
import org.vivecraft.VivecraftPaperExtentions.entities.CustomGoalSwell;
import org.vivecraft.VivecraftPaperExtentions.listeners.VivecraftCombatListener;
import org.vivecraft.VivecraftPaperExtentions.listeners.VivecraftItemListener;
import org.vivecraft.VivecraftPaperExtentions.listeners.VivecraftNetworkListener;
import org.vivecraft.VivecraftPaperExtentions.utils.AimFixHandler;
import org.vivecraft.VivecraftPaperExtentions.utils.Headshot;
import org.vivecraft.VivecraftPaperExtentions.utils.MetadataHelper;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;

import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.entity.CraftCreeper;
import org.bukkit.craftbukkit.entity.CraftEnderman;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.SpigotConfig;

public class VPE extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();

    public final static String CHANNEL = "vivecraft:data";

    public static Map<UUID, VivePlayer> vivePlayers = new HashMap<UUID, VivePlayer>();
    private final Map<UUID, org.bukkit.permissions.PermissionAttachment> attachments = new HashMap<>();
    public static VPE me;

    private GlobalRegionScheduler globalScheduler;
    private AsyncScheduler asyncScheduler;
    private RegionScheduler regionScheduler;

    private boolean sendPosDataTaskActive = false;

    public List<String> blocklist = new ArrayList<>();
    public boolean debug = false;

    private boolean isFolia = false;

    @Override
    public void onEnable() {
        super.onEnable();
        me = this;

        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            isFolia = true;
            globalScheduler = Bukkit.getGlobalRegionScheduler();
            asyncScheduler = Bukkit.getAsyncScheduler();
            regionScheduler = Bukkit.getRegionScheduler();
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        registerCommands();

        if(getConfig().getBoolean("general.vive-crafting", true)){
            registerRecipes();
        }

        config.options().copyDefaults(true);
        saveDefaultConfig();
        saveConfig();
        saveResource("config-instructions.yml", true);
        loadBlocklist();

        vivePlayers = new HashMap<>();

        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, new VivecraftNetworkListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VivecraftCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new VivecraftItemListener(this), this);

        Headshot.init(this);

        if(getConfig().getBoolean("setSpigotConfig.enabled")){
            SpigotConfig.movedWronglyThreshold = getConfig().getDouble("setSpigotConfig.movedWronglyThreshold");
            SpigotConfig.movedTooQuicklyMultiplier = getConfig().getDouble("setSpigotConfig.movedTooQuickly");
        }

        debug = getConfig().getBoolean("general.debug", false);

        startPosDataTask();
        scheduleEntityCheck();
    }

    private void startPosDataTask() {
        if (sendPosDataTaskActive) return;

        sendPosDataTaskActive = true;

        if (isFolia) {
            globalScheduler.runAtFixedRate(this, (task) -> {
                if (!sendPosDataTaskActive) {
                    task.cancel();
                    return;
                }
                sendPosData();
            }, 20L, 1L);
        } else {
            getServer().getScheduler().runTaskTimer(this, this::sendPosData, 20L, 1L);
        }
    }

    private void scheduleEntityCheck() {
        if (isFolia) {
            globalScheduler.run(this, (task) -> CheckAllEntities());
        } else {
            getServer().getScheduler().runTask(this, this::CheckAllEntities);
        }
    }

    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            VivecraftCommands.registerCommands(commands.getDispatcher());
        });
    }

    private void registerRecipes() {
        // Jump Boots recipe
        ItemStack jumpBoots = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta bootsMeta = jumpBoots.getItemMeta();
        bootsMeta.setUnbreakable(true);
        bootsMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        ((LeatherArmorMeta) bootsMeta).setColor(Color.fromRGB(9233775));
        jumpBoots.setItemMeta(bootsMeta);
        jumpBoots = setLocalizedItemName(jumpBoots, "vivecraft.item.jumpboots", "Jump Boots");

        ShapedRecipe jumpBootsRecipe = new ShapedRecipe(new NamespacedKey(this, "jump_boots"), jumpBoots);
        jumpBootsRecipe.shape("B", "S");
        jumpBootsRecipe.setIngredient('B', Material.LEATHER_BOOTS);
        jumpBootsRecipe.setIngredient('S', Material.SLIME_BLOCK);
        Bukkit.addRecipe(jumpBootsRecipe);

        // Climb Claws recipe
        ItemStack climbClaws = new ItemStack(Material.SHEARS);
        ItemMeta clawsMeta = climbClaws.getItemMeta();
        clawsMeta.setUnbreakable(true);
        clawsMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        climbClaws.setItemMeta(clawsMeta);
        climbClaws = setLocalizedItemName(climbClaws, "vivecraft.item.climbclaws", "Climb Claws");

        ShapedRecipe climbClawsRecipe = new ShapedRecipe(new NamespacedKey(this, "climb_claws"), climbClaws);
        climbClawsRecipe.shape("E E", "S S");
        climbClawsRecipe.setIngredient('E', Material.SPIDER_EYE);
        climbClawsRecipe.setIngredient('S', Material.SHEARS);
        Bukkit.addRecipe(climbClawsRecipe);
    }

    private void loadBlocklist() {
        ConfigurationSection sec = config.getConfigurationSection("climbey");
        if(sec != null){
            List<String> temp = sec.getStringList("blocklist");
            if(temp != null){
                for (String string : temp) {
                    if (BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(string)) == null) {
                        getLogger().warning("Unknown climbey block name: " + string);
                        continue;
                    }
                    blocklist.add(string);
                }
            }
        }
    }

    public static ItemStack setLocalizedItemName(ItemStack stack, String key, String fallback) {
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.translatable(key, Component.text(fallback)));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if(!event.isCancelled()){
            scheduleEntityEdit(event.getEntity());
        }
    }

    private void scheduleEntityEdit(Entity entity) {
        if (isFolia) {
            entity.getScheduler().run(this, (task) -> EditEntity(entity), null);
        } else {
            getServer().getScheduler().runTask(this, () -> EditEntity(entity));
        }
    }

    public void CheckAllEntities(){
        List<World> worlds = this.getServer().getWorlds();
        for(World world: worlds){
            for(Entity e: world.getLivingEntities()){
                scheduleEntityEdit(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void EditEntity(Entity entity){
        if(entity.getType() == EntityType.CREEPER){
            Creeper e = ((CraftCreeper) entity).getHandle();
            AbstractCollection<WrappedGoal> goalB = (AbstractCollection<WrappedGoal>)
                    Reflector.getFieldValue(Reflector.availableGoals, e.goalSelector);

            if(goalB != null) {
                goalB.removeIf(b -> b.getGoal() instanceof net.minecraft.world.entity.ai.goal.SwellGoal);
                e.goalSelector.addGoal(2, new CustomGoalSwell(e));
            }
        }
        else if(entity.getType() == EntityType.ENDERMAN){
            EnderMan e = ((CraftEnderman) entity).getHandle();

            // Remove and replace target selector
            AbstractCollection<WrappedGoal> targets = (AbstractCollection<WrappedGoal>)
                    Reflector.getFieldValue(Reflector.availableGoals, e.targetSelector);
            if(targets != null) {
                targets.removeIf(b -> b.getPriority() == Reflector.enderManLookTargetPriority);
                e.targetSelector.addGoal(Reflector.enderManLookTargetPriority,
                        new CustomEndermanLookForPlayerGoal(e,
                                livingEntity -> e.isAngryAt(livingEntity, (ServerLevel) e.level())));
            }

            // Remove and replace goal selector
            AbstractCollection<WrappedGoal> goals = (AbstractCollection<WrappedGoal>)
                    Reflector.getFieldValue(Reflector.availableGoals, e.goalSelector);
            if(goals != null) {
                goals.removeIf(b -> b.getPriority() == Reflector.enderManFreezePriority);
                e.goalSelector.addGoal(Reflector.enderManFreezePriority,
                        new CustomEndermanFreezeWhenLookedAt(e));
            }
        }
    }

    public void sendPosData() {
        for (VivePlayer sendTo : vivePlayers.values()) {
            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline())
                continue;

            for (VivePlayer v : vivePlayers.values()) {
                if (v == sendTo || v == null || v.player == null || !v.player.isOnline()
                        || v.player.getWorld() != sendTo.player.getWorld()
                        || v.hmdData == null || v.controller0data == null || v.controller1data == null){
                    continue;
                }

                double d = sendTo.player.getLocation().distanceSquared(v.player.getLocation());
                if (d < 256 * 256) {
                    sendTo.player.sendPluginMessage(this, CHANNEL, v.getUberPacket());
                }
            }
        }
    }

    public void sendVRActiveUpdate(VivePlayer v) {
        if(v == null) return;
        var payload = v.getVRPacket();

        for (VivePlayer sendTo : vivePlayers.values()) {
            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline())
                continue;

            if (v == sendTo || v.player == null || !v.player.isOnline())
                continue;

            sendTo.player.sendPluginMessage(this, CHANNEL, payload);
        }
    }

    @Override
    public void onDisable() {
        sendPosDataTaskActive = false;
        // Clean up attachments
        for (org.bukkit.permissions.PermissionAttachment attachment : attachments.values()) {
            if (attachment != null) {
                attachment.remove();
            }
        }
        attachments.clear();

        super.onDisable();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        vivePlayers.remove(player.getUniqueId());
        MetadataHelper.cleanupMetadata(player);

        // Clean up permissions attachment
        org.bukkit.permissions.PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }

        if(getConfig().getBoolean("welcomemsg.enabled"))
            broadcastConfigString("welcomemsg.leaveMessage", player.getDisplayName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player p = event.getPlayer();

        if (debug) getLogger().info(p.getName() + " has joined the server");

        int waitTime = getConfig().getInt("general.vive-only-kickwaittime", 200);
        waitTime = Math.max(100, Math.min(1000, waitTime)); // Clamp between 100-1000

        if (debug)
            getLogger().info("Checking " + p.getName() + " for Vivecraft");

        if (isFolia) {
            p.getScheduler().runDelayed(this, (task) -> {
                checkPlayerAfterJoin(p);
            }, null, waitTime);
        } else {
            getServer().getScheduler().runTaskLater(this, () -> {
                checkPlayerAfterJoin(p);
            }, waitTime);
        }

        Connection netManager = ((CraftPlayer)p).getHandle().connection.connection;
        netManager.channel.pipeline().addBefore("packet_handler", "vr_aim_fix",
                new AimFixHandler(netManager));
    }

    private void checkPlayerAfterJoin(Player p) {
        if (p.isOnline()) {
            boolean shouldKick = false;

            if(vivePlayers.containsKey(p.getUniqueId())) {
                VivePlayer vp = vivePlayers.get(p.getUniqueId());
                if(debug) {
                    getLogger().info(p.getName() + " using: " + vp.version
                            + " " + (vp.isVR() ? "VR" : "NONVR")
                            + " " + (vp.isSeated() ? "SEATED" : ""));
                }
                if(!vp.isVR()) shouldKick = true;
            } else {
                shouldKick = true;
                if(debug)
                    getLogger().info(p.getName() + " Vivecraft not detected");
            }

            if(shouldKick && getConfig().getBoolean("general.vive-only")) {
                if (!getConfig().getBoolean("general.allow-op") || !p.isOp()) {
                    getLogger().info(p.getName() + " got kicked for not using Vivecraft");
                    p.kick(Component.text(getConfig().getString("general.vive-only-kickmessage")));
                    return;
                }
            }

            sendWelcomeMessage(p);
            setPermissionsGroup(p);
        } else {
            if (debug)
                getLogger().info(p.getName() + " no longer online!");
        }
    }

    public static boolean isVive(Player p){
        if(p == null) return false;
        return vivePlayers.containsKey(p.getUniqueId())
                && vivePlayers.get(p.getUniqueId()).isVR();
    }

    public static boolean isCompanion(Player p){
        if(p == null) return false;
        return vivePlayers.containsKey(p.getUniqueId())
                && !vivePlayers.get(p.getUniqueId()).isVR();
    }

    public void setPermissionsGroup(Player p) {
        if(!getConfig().getBoolean("permissions.enabled")) return;

        Map<String, Boolean> groups = new HashMap<>();

        boolean isvive = isVive(p);
        boolean iscompanion = isCompanion(p);

        String g_vive = getConfig().getString("permissions.vivegroup");
        String g_classic = getConfig().getString("permissions.non-vivegroup");

        if (g_vive != null && !g_vive.trim().isEmpty())
            groups.put(g_vive, isvive);
        if (g_classic != null && !g_classic.trim().isEmpty())
            groups.put(g_classic, iscompanion);

        String g_freemove = getConfig().getString("permissions.freemovegroup");
        if (g_freemove != null && !g_freemove.trim().isEmpty()) {
            if (isvive) {
                groups.put(g_freemove, !vivePlayers.get(p.getUniqueId()).isTeleportMode);
            } else {
                groups.put(g_freemove, false);
            }
        }

        updatePlayerPermissionGroup(p, groups);
    }

    public void updatePlayerPermissionGroup(Player p, Map<String, Boolean> groups) {
        try {
            org.bukkit.permissions.PermissionAttachment attachment = attachments.computeIfAbsent(
                    p.getUniqueId(),
                    uuid -> p.addAttachment(this)
            );

            for (Map.Entry<String, Boolean> entry : groups.entrySet()) {
                String permissionNode = "vivecraft.group." + entry.getKey();
                boolean shouldHave = entry.getValue();

                if (shouldHave) {
                    if (!p.hasPermission(permissionNode)) {
                        if (debug) getLogger().info("Granting " + permissionNode + " to " + p.getName());
                        attachment.setPermission(permissionNode, true);
                    }
                } else {
                    if (p.hasPermission(permissionNode)) {
                        if (debug) getLogger().info("Revoking " + permissionNode + " from " + p.getName());
                        attachment.unsetPermission(permissionNode);
                    }
                }
            }

            p.recalculatePermissions();

        } catch (Exception e) {
            getLogger().severe("Could not update player permissions: " + e.getMessage());
        }
    }

    public void broadcastConfigString(String node, String playername){
        String message = this.getConfig().getString(node);
        if(message == null || message.isEmpty()) return;

        String[] formats = message.replace("&player", playername).split("\\n");
        for(Player p : Bukkit.getOnlinePlayers()){
            for (String line : formats) {
                VivecraftCommands.sendMessage(p, line);
            }
        }
    }

    public void sendWelcomeMessage(Player p){
        if(!getConfig().getBoolean("welcomemsg.enabled")) return;

        VivePlayer vp = vivePlayers.get(p.getUniqueId());

        if(vp == null){
            broadcastConfigString("welcomemsg.welcomeVanilla", p.getDisplayName());
        } else {
            if(vp.isSeated())
                broadcastConfigString("welcomemsg.welcomeSeated", p.getDisplayName());
            else if (!vp.isVR())
                broadcastConfigString("welcomemsg.welcomenonVR", p.getDisplayName());
            else
                broadcastConfigString("welcomemsg.welcomeVR", p.getDisplayName());
        }
    }

    public static boolean isSeated(Player player){
        return vivePlayers.containsKey(player.getUniqueId())
                && vivePlayers.get(player.getUniqueId()).isSeated();
    }

    public static boolean isStanding(Player player){
        if(vivePlayers.containsKey(player.getUniqueId())){
            VivePlayer vp = vivePlayers.get(player.getUniqueId());
            return !vp.isSeated() && vp.isVR();
        }
        return false;
    }

    public boolean isFolia() {
        return isFolia;
    }

    public GlobalRegionScheduler getGlobalScheduler() {
        return globalScheduler;
    }

    public AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    public RegionScheduler getRegionScheduler() {
        return regionScheduler;
    }
}