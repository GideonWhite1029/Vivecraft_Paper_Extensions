package org.vivecraft.VivecraftPaperExtentions.entities;

import java.util.function.Predicate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;

public class CustomEndermanLookForPlayerGoal extends NearestAttackableTargetGoal<Player> {
    private final EnderMan enderman;
    private Player pendingTarget;
    private int aggroTime;
    private int teleportTime;
    private final Predicate<LivingEntity> isAngerInducing;
    private final TargetingConditions startAggroTargetConditions;
    private final TargetingConditions continueAggroTargetConditions = TargetingConditions.forCombat().ignoreLineOfSight();

    public CustomEndermanLookForPlayerGoal(EnderMan entityenderman, Predicate<LivingEntity> isAngerInducing) {
        super(entityenderman, Player.class, 10, false, false,
                (entity, level) -> entity instanceof Player player
                        && (EndermanUtils.isLookingAtMe(player, entityenderman)
                        || entityenderman.isAngryAt(player, (ServerLevel) entityenderman.level()))
                        && !entityenderman.hasIndirectPassenger(player)
        );

        this.enderman = entityenderman;
        this.isAngerInducing = isAngerInducing;

        this.startAggroTargetConditions = TargetingConditions.forCombat()
                .range(this.getFollowDistance())
                .selector((entity, level) -> entity instanceof Player player
                        && (EndermanUtils.isLookingAtMe(player, this.enderman)
                        || this.enderman.isAngryAt(player, (ServerLevel) this.enderman.level()))
                        && !this.enderman.hasIndirectPassenger(player));
    }

    @Override
    public boolean canUse() {
        ServerLevel level = (ServerLevel) this.enderman.level();
        this.pendingTarget = level.getNearestPlayer(this.startAggroTargetConditions, this.enderman);
        return this.pendingTarget != null;
    }

    @Override
    public void start() {
        this.aggroTime = this.adjustedTickDelay(5);
        this.teleportTime = 0;
        this.enderman.setBeingStaredAt();
    }

    @Override
    public void stop() {
        this.pendingTarget = null;
        super.stop();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.pendingTarget != null) {
            if (!this.isAngerInducing.test(this.pendingTarget)) {
                return false;
            } else {
                this.enderman.lookAt(this.pendingTarget, 10.0F, 10.0F);
                return true;
            }
        } else {
            if (this.target != null) {
                if (this.enderman.hasIndirectPassenger(this.target)) {
                    return false;
                }

                ServerLevel level = (ServerLevel) this.enderman.level();
                if (this.continueAggroTargetConditions.test(level, this.enderman, this.target)) {
                    return true;
                }
            }

            return super.canContinueToUse();
        }
    }

    @Override
    public void tick() {
        if (this.enderman.getTarget() == null) {
            super.setTarget((LivingEntity)null);
        }

        if (this.pendingTarget != null) {
            if (--this.aggroTime <= 0) {
                this.target = this.pendingTarget;
                this.pendingTarget = null;
                super.start();
            }
        } else {
            if (this.target != null && !this.enderman.isPassenger()) {
                if (EndermanUtils.isLookingAtMe((Player)this.target, this.enderman)) {
                    if (this.target.distanceToSqr(this.enderman) < 16.0D) {
                        this.enderman.teleport();
                    }

                    this.teleportTime = 0;
                } else if (this.target.distanceToSqr(this.enderman) > 256.0D &&
                        this.teleportTime++ >= this.adjustedTickDelay(30) &&
                        this.enderman.teleportTowards(this.target)) {
                    this.teleportTime = 0;
                }
            }

            super.tick();
        }
    }
}