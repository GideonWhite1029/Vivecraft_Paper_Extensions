package org.vivecraft.VivecraftPaperExtentions.utils;

import org.vivecraft.VivecraftPaperExtentions.Reflector;
import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Player;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.world.entity.Pose;


public class PoseOverrider {
    @SuppressWarnings("unchecked")
    public static void injectPlayer(Player player) {
        EntityDataAccessor<Pose> poseObj = (EntityDataAccessor<Pose>) Reflector.getFieldValue(Reflector.Entity_Data_Pose, player);
        SynchedEntityData dataWatcher = (SynchedEntityData) Reflector.getFieldValue(Reflector.Entity_entityData, ((CraftEntity) player).getHandle());

        if (dataWatcher == null) {
            dataWatcher = ((CraftEntity) player).getHandle().getEntityData();
        }

        SynchedEntityData.DataItem<?>[] entries = (DataItem<?>[]) Reflector.getFieldValue(Reflector.SynchedEntityData_itemsById, dataWatcher);

        if (poseObj != null && entries != null && entries.length > poseObj.id()) {
            InjectedDataWatcherItem item = new InjectedDataWatcherItem(poseObj, Pose.STANDING, player);
            entries[poseObj.id()] = item;
        }
    }

    public static class InjectedDataWatcherItem extends SynchedEntityData.DataItem<Pose> {
        protected final Player player;

        public InjectedDataWatcherItem(EntityDataAccessor<Pose> datawatcherobject, Pose t0, Player player) {
            super(datawatcherobject, t0);
            this.player = player;
        }

        @Override
        public void setValue(Pose pose) {
            VivePlayer vp = VPE.vivePlayers.get(player.getUniqueId());
            if (vp != null && vp.isVR() && vp.crawling)
                super.setValue(Pose.SWIMMING);
            else
                super.setValue(pose);
        }
    }
}