package org.vivecraft.VivecraftPaperExtentions.utils;

import net.minecraft.world.entity.animal.Animal;
import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.bukkit.entity.*;

/**
 * Created by Yildri on 7-1-2017.
 */
public class Headshot {
    private static VPE vse;

    public static void init(VPE plugin){
        vse = plugin;
    }

    public static boolean isHeadshot(Entity target, Arrow arrow){
        boolean headshot = false;

        if(target.isInsideVehicle())
            return false;

        if(target instanceof Player){
            Player player = (Player) target;
            if(player.isSneaking()){
                //totalHeight = 1.65;
                //bodyHeight = 1.20;
                //headHeight = 0.45;
                if(arrow.getLocation().getY() >= player.getLocation().getY() + 1.20)
                    headshot = true;

            }else if(!player.isGliding()){
                //This means they must be standing normally (I can't calculate it for gliding players)
                //totalHeight = 1.80;
                //bodyHeight = 1.35;
                //headHeight = 0.45;
                if(arrow.getLocation().getY() >= player.getLocation().getY() + 1.35)
                    headshot = true;
            }

        }else if(!vse.getConfig().getBoolean("bow.headshotmobs")){
            return false;
        }else if(target instanceof LivingEntity){
            LivingEntity mob = (LivingEntity) target;
            double mobHeight = mob.getHeight();
            double headshotThreshold = 0.75;

            if(mob instanceof Zombie){
                if(mob instanceof ZombieVillager){
                    headshotThreshold = 0.72;
                }else if(((Zombie) mob).isBaby()){
                    headshotThreshold = 0.70;
                }

            }else if(mob instanceof Skeleton){
                headshotThreshold = 0.75;
                if(mob instanceof WitherSkeleton){
                    headshotThreshold = 0.77;
                }

            }else if(mob instanceof Creeper){
                headshotThreshold = 0.65;

            }else if(mob instanceof Enderman){
                headshotThreshold = 0.85;

            }else if(mob instanceof Villager || mob instanceof Illager){
                headshotThreshold = 0.72;

            }else if(mob instanceof IronGolem){
                headshotThreshold = 0.80;

            }else if(mob instanceof Witch){
                headshotThreshold = 0.72;

            }else if(mob instanceof PiglinBrute || mob instanceof Piglin){
                headshotThreshold = 0.73;

            }else if(mob instanceof Spider){
                return false;

            }else if(mob instanceof Slime || mob instanceof MagmaCube){
                headshotThreshold = 0.5;

            }else if(mob instanceof Animal){
                if(mob instanceof Cow || mob instanceof Pig || mob instanceof Sheep){
                    headshotThreshold = 0.60;
                }else if(mob instanceof Chicken){
                    headshotThreshold = 0.65;
                }else if(mob instanceof Wolf || mob instanceof Cat){
                    headshotThreshold = 0.70;
                }else{
                    headshotThreshold = 0.65;
                }
            }

            double arrowY = arrow.getLocation().getY();
            double mobY = mob.getLocation().getY();
            double headshotHeight = mobY + (mobHeight * headshotThreshold);

            if(arrowY >= headshotHeight){
                headshot = true;
            }
        }

        return headshot;
    }
}