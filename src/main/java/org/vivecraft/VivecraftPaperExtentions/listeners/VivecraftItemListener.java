package org.vivecraft.VivecraftPaperExtentions.listeners;

import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.util.Vector;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VivecraftItemListener implements Listener{
	VPE vpe = null;
	public VivecraftItemListener(VPE vpe){
		this.vpe = vpe;
	}
	
	 @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	 public void onPlayerDropItem(PlayerDropItemEvent event) {
		 final Player player = event.getPlayer();
		 if (!VPE.isVive(player))
		 return;
		 
		 VivePlayer vp = VPE.vivePlayers.get(player.getUniqueId());
		 
		 if(vp == null)return;
		 
		 float f2 = 0.3F;
		 
		 if(event.getItemDrop().getType() == EntityType.ITEM){
		 	 Vector v = new Vector();
			 float yaw = player.getLocation().getYaw();
			 float pitch = -player.getLocation().getPitch();
			 v.setX((double)(-Mth.sin(yaw * 0.017453292F) * Mth.cos(player.getLocation().getPitch() * 0.017453292F) * f2));
			 v.setZ((double)(Mth.cos(yaw * 0.017453292F) * Mth.cos(player.getLocation().getPitch() * 0.017453292F) * f2));
			 v.setY((double)(Mth.sin(pitch * 0.017453292F) * f2 + 0.1F));
			 
             Vec3 aim = vp.getControllerDir(0);
			 event.getItemDrop().teleport(vp.getControllerPos(0).add(0.2f*aim.x,0.25f*aim.y - 0.2f, 0.2f*aim.z));
			 event.getItemDrop().setVelocity(v);
		 }
	 }
}
