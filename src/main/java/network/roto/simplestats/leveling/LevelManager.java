package network.roto.simplestats.leveling;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import network.roto.simplestats.utils.ConfigUtils;
import java.util.Map;

public class LevelManager {
    private static final String XP_TAG = "simplestats_xp";
    private static final String LEVEL_TAG = "simplestats_level";
    private static final int BASE_XP = 5;
    private static final double XP_SCALING = 1.5; // Each level requires 50% more XP
    private static final int BASE_LEVEL_REQUIREMENT = 100; // Base XP needed for level 1
    private static Map<String, Object> xpConfig = null;

    public static void loadConfig(String filePath) {
        xpConfig = ConfigUtils.readJsonConfig(filePath);
    }

    public static void addXp(Player player, LivingEntity mob) {
        double xp = BASE_XP;
        if (xpConfig != null) {
            String key = EntityType.getKey(mob.getType()).toString();
            Map<String, Object> xpValues = (Map<String, Object>) xpConfig.get("xp_values");
            if (xpValues != null && xpValues.containsKey(key)) {
                xp = ((Number)xpValues.get(key)).doubleValue();
            }
        }
        CompoundTag tag = player.getPersistentData();
        double currentXp = tag.getDouble(XP_TAG);
        int currentLevel = tag.getInt(LEVEL_TAG);
        
        // Add XP and check for level up
        double newXp = currentXp + xp;
        int newLevel = currentLevel;
        
        // Check for level ups
        while (newXp >= getXpRequiredForLevel(newLevel + 1)) {
            newXp -= getXpRequiredForLevel(newLevel + 1);
            newLevel++;
        }
        
        // Save new values
        tag.putDouble(XP_TAG, newXp);
        tag.putInt(LEVEL_TAG, newLevel);
        
        // If leveled up, trigger level up event
        if (newLevel > currentLevel) {
            onLevelUp(player, newLevel);
        }
    }

    public static double getXp(Player player) {
        return player.getPersistentData().getDouble(XP_TAG);
    }

    public static int getLevel(Player player) {
        return player.getPersistentData().getInt(LEVEL_TAG);
    }

    public static double getXpRequiredForLevel(int level) {
        return BASE_LEVEL_REQUIREMENT * Math.pow(XP_SCALING, level - 1);
    }

    public static double getXpProgress(Player player) {
        int currentLevel = getLevel(player);
        double currentXp = getXp(player);
        double xpForNextLevel = getXpRequiredForLevel(currentLevel + 1);
        return currentXp / xpForNextLevel;
    }

    private static void onLevelUp(Player player, int newLevel) {
        // You can add level up effects here, like:
        // - Playing a sound
        // - Showing a message
        // - Giving rewards
        // - Triggering other events
    }

    public static double getTotalXpForLevel(int level) {
        double total = 0;
        for (int i = 1; i <= level; i++) {
            total += getXpRequiredForLevel(i);
        }
        return total;
    }
} 