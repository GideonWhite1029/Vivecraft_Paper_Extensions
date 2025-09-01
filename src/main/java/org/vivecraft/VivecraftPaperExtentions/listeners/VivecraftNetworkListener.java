package org.vivecraft.VivecraftPaperExtentions.listeners;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.vivecraft.VivecraftPaperExtentions.Reflector;
import org.vivecraft.VivecraftPaperExtentions.VPE;
import org.vivecraft.VivecraftPaperExtentions.VivePlayer;
import org.vivecraft.VivecraftPaperExtentions.utils.MetadataHelper;
import org.vivecraft.VivecraftPaperExtentions.utils.PoseOverrider;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

public class VivecraftNetworkListener implements PluginMessageListener {
    public VPE vpe;

    public VivecraftNetworkListener(VPE vpe){
        this.vpe = vpe;
    }

    public enum PacketDiscriminators {
        VERSION,
        REQUESTDATA,
        HEADDATA,
        CONTROLLER0DATA,
        CONTROLLER1DATA,
        WORLDSCALE,
        DRAW,
        MOVEMODE,
        UBERPACKET,
        TELEPORT,
        CLIMBING,
        SETTING_OVERRIDE,
        HEIGHT,
        ACTIVEHAND,
        CRAWL,
        NETWORK_VERSION,
        VR_SWITCHING,
        IS_VR_ACTIVE,
        VR_PLAYER_STATE
    }

    @Override
    public void onPluginMessageReceived(String channel, Player sender, byte[] payload) {

        if(!channel.equalsIgnoreCase(VPE.CHANNEL)) return;

        if(payload.length==0) return;

        VivePlayer vp = VPE.vivePlayers.get(sender.getUniqueId());

        PacketDiscriminators disc = PacketDiscriminators.values()[payload[0]];
        if(vp == null && disc != PacketDiscriminators.VERSION) {
            //how?
            return;
        }

        byte[] data = Arrays.copyOfRange(payload, 1, payload.length);
        switch (disc){
            case CONTROLLER0DATA:
                vp.controller0data = data;
                MetadataHelper.updateMetdata(vp);
                break;
            case CONTROLLER1DATA:
                vp.controller1data = data;
                MetadataHelper.updateMetdata(vp);
                break;
            case DRAW:
                vp.draw = data;
                break;
            case HEADDATA:
                vp.hmdData = data;
                MetadataHelper.updateMetdata(vp);
                break;
            case MOVEMODE:
                break;
            case REQUESTDATA:
                //only we can use that word.
                break;
            case VERSION:
                vp = new VivePlayer(sender);
                ByteArrayInputStream byin = new ByteArrayInputStream(data);
                DataInputStream da = new DataInputStream(byin);
                InputStreamReader is = new InputStreamReader(da);
                BufferedReader br = new BufferedReader(is);
                VPE.vivePlayers.put(sender.getUniqueId(), vp);

                sender.sendPluginMessage(vpe, VPE.CHANNEL, StringToPayload(PacketDiscriminators.VERSION, vpe.getDescription().getFullName()));

                try {
                    String version = br.readLine();
                    vp.version = version;
                    if(version.contains("NONVR")){
                        vp.setVR(false);
                    }
                    else{
                        vp.setVR(true);
                        PoseOverrider.injectPlayer(sender);
                    }

                    if(vpe.getConfig().getBoolean("SendPlayerData.enabled") == true)
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, new byte[]{(byte) PacketDiscriminators.REQUESTDATA.ordinal()});

                    if(vpe.getConfig().getBoolean("crawling.enabled") == true)
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, new byte[]{(byte) PacketDiscriminators.CRAWL.ordinal()});

                    if(vpe.getConfig().getBoolean("general.vive-only") == false)
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, new byte[]{(byte) PacketDiscriminators.VR_SWITCHING.ordinal(), 1});
                    else
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, new byte[]{(byte) PacketDiscriminators.VR_SWITCHING.ordinal(), 0});

                    if(vpe.getConfig().getBoolean("climbey.enabled") == true){

                        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        byteArrayOutputStream.write(PacketDiscriminators.CLIMBING.ordinal());
                        byteArrayOutputStream.write(1); // climbey allowed

                        String mode = vpe.getConfig().getString("climbey.blockmode","none");
                        if(!sender.hasPermission(vpe.getConfig().getString("permissions.climbperm"))){
                            if(mode.trim().equalsIgnoreCase("include"))
                                byteArrayOutputStream.write(1);
                            else if(mode.trim().equalsIgnoreCase("exclude"))
                                byteArrayOutputStream.write(2);
                            else
                                byteArrayOutputStream.write(0);
                        } else {
                            byteArrayOutputStream.write(0);
                        }

                        for (String block : vpe.blocklist) {
                            if (!writeString(byteArrayOutputStream, block))
                                vpe.getLogger().warning("Block name too long: " + block);
                        }

                        final byte[] p = byteArrayOutputStream.toByteArray();
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, p);
                    }

                    if (vpe.getConfig().getBoolean("teleport.limitedsurvival")) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "limitedTeleport", true); // do it
                        writeSetting(baos, "teleportLimitUp", Mth.clamp(vpe.getConfig().getInt("teleport.uplimit"), 0, 4));
                        writeSetting(baos, "teleportLimitDown", Mth.clamp(vpe.getConfig().getInt("teleport.downlimit"), 0, 16));
                        writeSetting(baos, "teleportLimitHoriz", Mth.clamp(vpe.getConfig().getInt("teleport.horizontallimit"), 0, 32));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, p);
                    }

                    if (vpe.getConfig().getBoolean("worldscale.limitrange")) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "worldScale.min", Mth.clamp(vpe.getConfig().getDouble("worldscale.min"), 0.1, 100));
                        writeSetting(baos, "worldScale.max", Mth.clamp(vpe.getConfig().getDouble("worldscale.max"), 0.1, 100));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, p);
                    }

                    if (vpe.getConfig().getBoolean("settingOverrides.thirdPersonItems")) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "thirdPersonItems", true);

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, p);
                    }

                    if (vpe.getConfig().getBoolean("teleport.enabled"))
                        sender.sendPluginMessage(vpe, VPE.CHANNEL, new byte[]{(byte) PacketDiscriminators.TELEPORT.ordinal()});

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case WORLDSCALE:
                ByteArrayInputStream a = new ByteArrayInputStream(data);
                DataInputStream b = new DataInputStream(a);
                try {
                    vp.worldScale = b.readFloat();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case HEIGHT:
                ByteArrayInputStream a1 = new ByteArrayInputStream(data);
                DataInputStream b1 = new DataInputStream(a1);
                try {
                    vp.heightScale = b1.readFloat();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            case TELEPORT:
                if (!vpe.getConfig().getBoolean("teleport.enabled"))
                    break;

                ByteArrayInputStream in = new ByteArrayInputStream(data);
                DataInputStream d = new DataInputStream(in);
                try {
                    float x = d.readFloat();
                    float y = d.readFloat();
                    float z = d.readFloat();
                    ServerPlayer nms = 	((CraftPlayer)sender).getHandle();
                    nms.absSnapTo(x, y, z, nms.getXRot(), nms.getYRot());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case CLIMBING:
                ServerPlayer nms = ((CraftPlayer)sender).getHandle();
                nms.fallDistance = 0;
                Reflector.setFieldValue(Reflector.aboveGroundTickCount, nms.connection, 0);
                break;
            case ACTIVEHAND:
                ByteArrayInputStream a2 = new ByteArrayInputStream(data);
                DataInputStream b2 = new DataInputStream(a2);
                try {
                    vp.activeHand = b2.readByte();
                    if (vp.isSeated()) vp.activeHand = 0;
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case CRAWL:
                if (!vpe.getConfig().getBoolean("crawling.enabled"))
                    break;
                ByteArrayInputStream a3 = new ByteArrayInputStream(data);
                DataInputStream b3 = new DataInputStream(a3);
                try {
                    vp.crawling = b3.readBoolean();
                    if (vp.crawling)
                        ((CraftPlayer)sender).getHandle().setPose(Pose.SWIMMING);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case IS_VR_ACTIVE:
                ByteArrayInputStream vrb = new ByteArrayInputStream(data);
                DataInputStream vrd = new DataInputStream(vrb);
                boolean vr;
                try {
                    vr = vrd.readBoolean();
                    if(vp.isVR()==vr) break;
                    if (!vr) {
                        vp.setVR(false);
                    } else {
                        vp.setVR(true);
                        PoseOverrider.injectPlayer(sender);
                    }
                    vpe.sendVRActiveUpdate(vp);
                    vpe.setPermissionsGroup(sender);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case NETWORK_VERSION:
                //don't care yet.
                break;
            case VR_PLAYER_STATE:
                //todo.
                break;
            default:
                break;
        }
    }

    public void writeSetting(ByteArrayOutputStream output, String name, Object value) {
        if (!writeString(output, name)) {
            vpe.getLogger().warning("Setting name too long: " + name);
            return;
        }
        if (!writeString(output, value.toString())) {
            vpe.getLogger().warning("Setting value too long: " + value);
            writeString(output, "");
        }
    }

    public static byte[] StringToPayload(PacketDiscriminators version, String input){
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write((byte)version.ordinal());
        if(!writeString(output, input)) {
            output.reset();
            return output.toByteArray();
        }

        return output.toByteArray();

    }

    public static boolean writeString(ByteArrayOutputStream output, String str) {
        byte[] bytes = str.getBytes(Charset.forName("UTF-8"));
        int len = bytes.length;
        try {
            if(!writeVarInt(output, len, 2))
                return false;
            output.write(bytes);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static int varIntByteCount(int toCount)
    {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : ((toCount & 0xFFFFC000) == 0 ? 2 : ((toCount & 0xFFE00000) == 0 ? 3 : ((toCount & 0xF0000000) == 0 ? 4 : 5)));
    }

    public static boolean writeVarInt(ByteArrayOutputStream to, int toWrite, int maxSize)
    {
        if (varIntByteCount(toWrite) > maxSize) return false;
        while ((toWrite & -128) != 0)
        {
            to.write(toWrite & 127 | 128);
            toWrite >>>= 7;
        }

        to.write(toWrite);
        return true;
    }
}
