package org.vivecraft.VivecraftPaperExtentions.listeners;

import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import org.vivecraft.VivecraftPaperExtentions.utils.Headshot;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;


public class VivecraftCombatListener implements Listener{

    private VPE vpe;

    public VivecraftCombatListener(VPE plugin){
        this.vpe = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        //position all projectiles correctly.

        Projectile proj = event.getEntity();

        if (!(proj.getShooter() instanceof Player) || !VPE.isVive((Player) proj.getShooter()))
            return;

        Player pl = (Player)proj.getShooter();
        final VivePlayer vp = (VivePlayer) VPE.vivePlayers.get(pl.getUniqueId());

        final boolean arrow = proj instanceof AbstractArrow && !(proj instanceof Trident);

        if ((vp == null) && (this.vpe.getConfig().getBoolean("general.debug"))) {
            vpe.getLogger().warning(" Error on projectile launch!");
        }

        ServerPlayer nsme = ((CraftPlayer)pl).getHandle();

        Location pos = vp.getControllerPos(vp.activeHand);
        Vec3 aim = vp.getControllerDir(vp.activeHand);

        // Adjust for bow draw power
        if(arrow && vp.getDraw() != 0) {
            Vector originalVelocity = proj.getVelocity();
            double originalSpeed = originalVelocity.length();
            float drawPower = vp.getDraw();

            double minSpeed = originalSpeed * 0.1;
            double newSpeed = minSpeed + (originalSpeed - minSpeed) * drawPower;

            if(!vp.isSeated()){
                pos = vp.getControllerPos(0);
                Vector m = (vp.getControllerPos(1).subtract(vp.getControllerPos(0))).toVector();
                m = m.normalize();
                aim = new Vec3(m.getX(), m.getY(), m.getZ());
            }

            Location loc = new Location(proj.getWorld(),
                    pos.getX() + aim.x * 0.6f,
                    pos.getY() + aim.y * 0.6f,
                    pos.getZ() + aim.z * 0.6f);

            proj.teleport(loc);

            proj.setVelocity(new Vector(aim.x * newSpeed, aim.y * newSpeed, aim.z * newSpeed));

            if(proj instanceof Arrow arrowProj) {
                double baseDamage = 2.0;
                double damageMultiplier = 0.5 + (2.5 * drawPower);
                arrowProj.setDamage(baseDamage * damageMultiplier);

                if(drawPower > 0.9f) {
                    arrowProj.setCritical(true);
                }
            }
        } else {
            Location loc = new Location(proj.getWorld(),
                    pos.getX() + aim.x * 0.6f,
                    pos.getY() + aim.y * 0.6f,
                    pos.getZ() + aim.z * 0.6f);

            double velo = proj.getVelocity().length();
            proj.teleport(loc);
            proj.setVelocity(new Vector(aim.x * velo, aim.y * velo, aim.z * velo));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onProjectileHit(EntityDamageByEntityEvent event) {

        if(event.getDamager() instanceof Trident) return;

        if (event.getDamager() instanceof Arrow && event.getEntity() instanceof LivingEntity) {
            final Arrow arrow = (Arrow) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();
            boolean headshot = Headshot.isHeadshot(target, arrow);

            if (!(arrow.getShooter() instanceof Player) || !VPE.isVive((Player) arrow.getShooter()))
                return;
            Player pl = (Player)arrow.getShooter();
            VivePlayer vp = (VivePlayer) VPE.vivePlayers.get(pl.getUniqueId());

            if(!vp.isSeated()){
                if(headshot){
                    event.setDamage(event.getDamage() * vpe.getConfig().getDouble("bow.standingheadshotmultiplier"));
                }else{
                    event.setDamage(event.getDamage() * vpe.getConfig().getDouble("bow.standingmultiplier"));
                }
            }else{
                if(headshot){
                    event.setDamage(event.getDamage() * vpe.getConfig().getDouble("bow.seatedheadshotmultiplier"));
                }else{
                    event.setDamage(event.getDamage() * vpe.getConfig().getDouble("bow.seatedmultiplier"));
                }
            }

        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent e) {
        final Entity damager = e.getDamager();
        final Entity damaged = e.getEntity();

        if (damager instanceof Player) {
            if (damaged instanceof Player) {
                Player attacker = (Player) damager;
                Player victim = (Player) damaged;

                if (!vpe.getConfig().getBoolean("pvp.VRvsVR",true)) {
                    if (VPE.isVive(attacker) && VPE.isVive(victim)) {
                        if (VPE.isStanding(attacker) && VPE.isStanding(victim)) {
                            e.setCancelled(true);
                        }
                    }
                }

                if (!vpe.getConfig().getBoolean("pvp.VRvsNONVR", true)) {
                    if ((VPE.isVive(attacker) && !VPE.isVive(victim)) || (VPE.isVive(victim) && !VPE.isVive(attacker))) {
                        e.setCancelled(true);
                    }
                }

                if (!vpe.getConfig().getBoolean("pvp.SEATEDVRvsNONVR", true)) {
                    if(((VPE.isVive(attacker) && VPE.isSeated(attacker)) && !VPE.isVive(victim)) || ((VPE.isVive(victim) && VPE.isSeated(victim)) && !VPE.isVive(attacker))){
                        e.setCancelled(true);
                    }
                }

                if (!vpe.getConfig().getBoolean("pvp.VRvsSEATEDVR", true)) {
                    if (VPE.isVive(attacker) && VPE.isVive(victim)) {
                        if ((VPE.isSeated(attacker) && VPE.isStanding(victim)) || (VPE.isSeated(victim) && VPE.isStanding(attacker))) {
                            e.setCancelled(true);
                        }
                    }
                }

            }
            else if(damaged instanceof Fireball) {
                VivePlayer vp = (VivePlayer) VPE.vivePlayers.get(damager.getUniqueId());
                if(vp!=null && vp.isVR()) {
                    Vec3 dir = vp.getHMDDir();
                    //Interesting experiment.
                    //We know the player's look is read immediately after this event returns.
                    //Override it here. It should be set back to normal next tick.
                    //And ideally nothing weird happens because of it.

                    ((CraftEntity) damager).getHandle().setXRot((float) Math.toDegrees(Math.asin(dir.y/dir.length())));
                    ((CraftEntity) damager).getHandle().setYRot((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                }
            }
        }
    }

}