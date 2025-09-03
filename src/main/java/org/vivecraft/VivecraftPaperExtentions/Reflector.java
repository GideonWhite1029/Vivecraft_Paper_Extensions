package org.vivecraft.VivecraftPaperExtentions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.bukkit.Bukkit;

public class Reflector {

    public static Field Entity_Data_Pose = getPrivateField("DATA_POSE", Entity.class);
    public static Field Entity_entityData = getPrivateField("entityData", Entity.class);
    public static Field Entity_eyeHeight = getPrivateField("eyeHeight", Entity.class);
    public static Field SynchedEntityData_itemsById = getPrivateField("itemsById", SynchedEntityData.class);
    public static Field availableGoals = getPrivateField("availableGoals", GoalSelector.class);
    public static Field aboveGroundTickCount = getPrivateField("aboveGroundTickCount", ServerGamePacketListenerImpl.class);
    public static Field connection = getPrivateField("connection", ServerCommonPacketListenerImpl.class);
    public static int enderManFreezePriority = 1;
    public static int enderManLookTargetPriority = 1;

    public static Object getFieldValue(Field field, Object object) {
        if (field == null || object == null) {
            return null;
        }

        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to get field value: " + field.getName(), e);
        }
        return null;
    }

    public static void setFieldValue(Field field, Object object, Object value) {
        if (field == null || object == null) {
            return;
        }

        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to set field value: " + field.getName(), e);
        }
    }

    private static Field getPrivateField(String fieldName, Class<?> clazz) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Try obfuscated names as fallback for compatibility
            field = tryObfuscatedField(fieldName, clazz);
            if (field == null) {
                Bukkit.getLogger().log(Level.SEVERE,
                        String.format("Field '%s' not found in %s - check your Paper version!",
                                fieldName, clazz.getSimpleName()));
            }
        }
        return field;
    }

    private static Field tryObfuscatedField(String mojangName, Class<?> clazz) {
        // Map of Mojang names to obfuscated names for 1.21.x
        String obfuscatedName = switch (mojangName) {
            case "entityData" -> "at";  // Entity.entityData
            case "DATA_POSE" -> "bA";   // Entity.DATA_POSE
            case "eyeHeight" -> "be";    // Entity.eyeHeight
            case "itemsById" -> "e";     // SynchedEntityData.itemsById
            case "availableGoals" -> "c"; // GoalSelector.availableGoals
            case "aboveGroundTickCount" -> "J"; // ServerGamePacketListenerImpl.aboveGroundTickCount
            case "connection" -> "e";    // ServerCommonPacketListenerImpl.connection
            default -> null;
        };

        if (obfuscatedName == null) {
            return null;
        }

        try {
            Field field = clazz.getDeclaredField(obfuscatedName);
            field.setAccessible(true);
            Bukkit.getLogger().log(Level.INFO,
                    String.format("Using obfuscated field '%s' for '%s' in %s",
                            obfuscatedName, mojangName, clazz.getSimpleName()));
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Method getPrivateMethod(String methodName, Class clazz, Class... param) {
        Method m = null;
        try {
            if (param == null || param.length == 0) {
                m = clazz.getDeclaredMethod(methodName);
            } else {
                m = clazz.getDeclaredMethod(methodName, param);
            }
            m.setAccessible(true);
        } catch (NoSuchMethodException e) {
            Bukkit.getLogger().log(Level.SEVERE,
                    String.format("Method '%s' not found in %s", methodName, clazz.getSimpleName()), e);
        }
        return m;
    }

    public static Object invoke(Method m, Object object, Object... param) {
        if (m == null || object == null) {
            return null;
        }

        try {
            if (param == null || param.length == 0) {
                return m.invoke(object);
            } else {
                return m.invoke(object, param);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to invoke method: " + m.getName(), e);
        }
        return null;
    }

    public static boolean checkFields() {
        boolean valid = true;

        if (Entity_Data_Pose == null) {
            Bukkit.getLogger().warning("Entity_Data_Pose field not found!");
            valid = false;
        }
        if (Entity_entityData == null) {  // Добавлена проверка
            Bukkit.getLogger().warning("Entity_entityData field not found!");
            valid = false;
        }
        if (Entity_eyeHeight == null) {
            Bukkit.getLogger().warning("Entity_eyeHeight field not found!");
            valid = false;
        }
        if (SynchedEntityData_itemsById == null) {
            Bukkit.getLogger().warning("SynchedEntityData_itemsById field not found!");
            valid = false;
        }
        if (availableGoals == null) {
            Bukkit.getLogger().warning("availableGoals field not found!");
            valid = false;
        }
        if (aboveGroundTickCount == null) {
            Bukkit.getLogger().warning("aboveGroundTickCount field not found!");
            valid = false;
        }
        if (connection == null) {
            Bukkit.getLogger().warning("connection field not found!");
            valid = false;
        }

        return valid;
    }
}