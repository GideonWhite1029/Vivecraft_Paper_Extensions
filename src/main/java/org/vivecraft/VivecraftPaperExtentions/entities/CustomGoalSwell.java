package org.vivecraft.VivecraftPaperExtentions.entities;

import java.util.EnumSet;

import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

public class CustomGoalSwell extends Goal {

    private final Creeper creeper;
    private LivingEntity target;
    private final boolean creeperRadiusEnabled;
    private final double configuredRadius;

    public CustomGoalSwell(Creeper var0) {
        this.creeper = var0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));

        VPE vse = VPE.getPlugin(VPE.class);
        this.creeperRadiusEnabled = vse.getConfig().getBoolean("CreeperRadius.enabled", true);
        this.configuredRadius = vse.getConfig().getDouble("CreeperRadius.radius", 3.0);
    }

    public double creeperBlowyUppyRadius = 3.0f; // VIVE default is 3

    @Override
    public boolean canUse(){
        LivingEntity livingentity = this.creeper.getTarget();

        if(creeperRadiusEnabled && livingentity != null){
            if(VPE.vivePlayers.containsKey(livingentity.getBukkitEntity().getUniqueId())) {
                VivePlayer vivePlayer = VPE.vivePlayers.get(livingentity.getBukkitEntity().getUniqueId());
                if(vivePlayer != null && VPE.isVive(vivePlayer.player)) {
                    creeperBlowyUppyRadius = configuredRadius;
                }
            }
        }

        return this.creeper.getSwellDir() > 0 ||
                (livingentity != null && this.creeper.distanceToSqr(livingentity) < creeperBlowyUppyRadius * creeperBlowyUppyRadius);
    }

    @Override
    public void start() {
        this.creeper.getNavigation().stop();
        this.target = this.creeper.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            this.creeper.setSwellDir(-1);
        } else if (this.creeper.distanceToSqr(this.target) > 49.0D) {
            this.creeper.setSwellDir(-1);
        } else if (!this.creeper.getSensing().hasLineOfSight(this.target)) {
            this.creeper.setSwellDir(-1);
        } else {
            this.creeper.setSwellDir(1);
        }
    }
}