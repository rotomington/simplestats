package network.roto.simplestats.leveling;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import network.roto.simplestats.Simplestats;
import network.roto.simplestats.network.NetworkHandler;
import network.roto.simplestats.utils.Config;
import network.roto.simplestats.utils.StringIntergerPair;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    public static final String XP_TAG = "simplestats_xp";
    public static final String LEVEL_TAG = "simplestats_level";
    public static final String POINTS_TAG = "simplestats_points";
    private static final int BASE_XP = Config.CONFIG.baseXP.get();
    private static final double XP_SCALING = Config.CONFIG.xpScaling.get(); // Each level requires 50% more XP
    private static final int BASE_LEVEL_REQUIREMENT = Config.CONFIG.baseLevelRequirement.get(); // Base XP needed for level 1
    private static final int POINTS_PER_LEVEL = Config.CONFIG.pointsPerLevel.get();
    private static List<StringIntergerPair> xpConfig = new ArrayList<>(List.of());
    public static int LEVEL;
    public static int XP;
    public static int POINTS;

    public void updateXpConfig (StringIntergerPair sip) {
        xpConfig.add(sip);
    }

    public static void requestDatafromServer(String type, String data) {
        NetworkHandler packet = new NetworkHandler(type, data);
        PacketDistributor.sendToServer(packet);
    }



    public static void loadXpConfig(LevelManager levelManager){
        StringIntergerPair sip = new StringIntergerPair("",0);
        List<String> encodedConfig = Config.CONFIG.xpValues.get().stream().map(obj -> (String) obj).toList();
        if (!encodedConfig.isEmpty() && encodedConfig != null){
            for (String str : encodedConfig) {
                sip = sip.decodeSIP(str);
                levelManager.updateXpConfig(sip);
            }
        }
    }

    public static void addXp(Player player, LivingEntity mob) {
        int xp = new LevelManager().BASE_XP;
        if (xpConfig != null) {
            String key = EntityType.getKey(mob.getType()).toString();
            for (StringIntergerPair value : xpConfig){
                if (value.string.equals(key)){
                    xp = value.interger;
                }
            }
        }
        giveXp(player, xp);
    }

    public static boolean canPay(Player player, int cost) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = serverPlayer.getPersistentData();
            if (!tag.contains(POINTS_TAG)) {
                tag.putInt(POINTS_TAG, 0);
                serverPlayer.getPersistentData().put(POINTS_TAG, tag.get(POINTS_TAG));
            }
            int currentPoints = tag.getInt(POINTS_TAG);
            return currentPoints >= cost;
        } else {
            requestDatafromServer("request", "points");
            return POINTS >= cost;
        }
    }

    public static void giveXp(Player player, int xp){
        CompoundTag tag = player.getPersistentData();
        
        // Initialize default values if they don't exist
        if (!tag.contains(XP_TAG)) {
            tag.putInt(XP_TAG, 0);
        }
        if (!tag.contains(LEVEL_TAG)) {
            tag.putInt(LEVEL_TAG, 1);
        }
        
        int currentXp = tag.getInt(XP_TAG);
        int currentLevel = tag.getInt(LEVEL_TAG);

        // Add XP and check for level up
        int newXp = currentXp + xp;
        int newLevel = currentLevel;

        // Check for level ups
        while (newXp >= getXpRequiredForLevel(newLevel + 1)) {
            newXp -= getXpRequiredForLevel(newLevel + 1);
            newLevel++;
        }

        //Check for Level Down
        while (newXp < 0) {
            newXp += getXpRequiredForLevel(newLevel - 1);
            newLevel--;
        }

        // Save new values
        tag.putInt(XP_TAG, newXp);
        tag.putInt(LEVEL_TAG, newLevel);

        // If leveled up, trigger level up event and give points
        if (newLevel > currentLevel) {
            onLevelUp(player, newLevel);
            givePoints(player, 1);
        }

        // Sync with client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler packet = new NetworkHandler("xp_update", String.valueOf(newXp));
            PacketDistributor.sendToPlayer(serverPlayer, packet);
            
            if (newLevel != currentLevel) {
                packet = new NetworkHandler("level_update", String.valueOf(newLevel));
                PacketDistributor.sendToPlayer(serverPlayer, packet);
            }
        }
    }

    public static void givePoints(Player player, int points){
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = serverPlayer.getPersistentData();
            if (!tag.contains(POINTS_TAG)) {
                tag.putInt(POINTS_TAG, 0);
            }

            int currentPoints = tag.getInt(POINTS_TAG);
            int newPoints = currentPoints + points;
            
            // Ensure points don't go negative
            if (newPoints < 0) {
                newPoints = 0;
            }
            
            // Save new values
            tag.putInt(POINTS_TAG, newPoints);

            // Sync with client immediately
            NetworkHandler packet = new NetworkHandler("points_update", String.valueOf(newPoints));
            PacketDistributor.sendToPlayer(serverPlayer, packet);

        } else {
            // Client-side update
            POINTS += points;
            if (POINTS < 0) {
                POINTS = 0;
            }
            // Request server update
            requestDatafromServer("request", "points");
        }
    }

    public static void handlePointsUpdate(ServerPlayer player, int points) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(POINTS_TAG)) {
            tag.putInt(POINTS_TAG, 0);
        }

        int currentPoints = tag.getInt(POINTS_TAG);
        int newPoints = currentPoints + points;
        
        // Ensure points don't go negative
        if (newPoints < 0) {
            newPoints = 0;
        }
        
        // Save new values
        tag.putInt(POINTS_TAG, newPoints);
        player.getPersistentData().put(POINTS_TAG, tag.get(POINTS_TAG));

        // Sync with client immediately
        NetworkHandler packet = new NetworkHandler("points_update", String.valueOf(newPoints));
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static int getXp(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = serverPlayer.getPersistentData();
            if (!tag.contains(XP_TAG)) {
                tag.putInt(XP_TAG, 0);
            }
            return tag.getInt(XP_TAG);
        } else {
            requestDatafromServer("request", "xp");
            return XP;
        }
    }

    public static int getPoints(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = serverPlayer.getPersistentData();
            if (!tag.contains(POINTS_TAG)) {
                tag.putInt(POINTS_TAG, 0);
                serverPlayer.getPersistentData().put(POINTS_TAG, tag.get(POINTS_TAG));
            }
            return tag.getInt(POINTS_TAG);
        } else {
            // Request points update from server
            requestDatafromServer("request", "points");
            return POINTS;
        }
    }

    public static int getLevel(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = serverPlayer.getPersistentData();
            if (!tag.contains(LEVEL_TAG)) {
                tag.putInt(LEVEL_TAG, 1);
            }
            return tag.getInt(LEVEL_TAG);
        } else {
            requestDatafromServer("request", "level");
            return LEVEL;
        }
    }

    public static int getXpRequiredForLevel(int level) {
        double scaling = new LevelManager().XP_SCALING;
        int base_level = new LevelManager().BASE_LEVEL_REQUIREMENT;
        return (int) Math.floor(base_level * Math.pow(scaling, level - 1));
    }

    public static double getXpProgress(Player player) {
        int currentLevel = getLevel(player);
        float currentXp = (float) getXp(player);
        float xpForNextLevel = (float) getXpRequiredForLevel(currentLevel + 1);
        return currentXp / xpForNextLevel;
    }

    private static void onLevelUp(Player player, int newLevel) {
        // You can add level up effects here, like:
        // - Playing a sound
        // - Showing a message
        // - Giving rewards
        // - Triggering other events
        player.playSound(SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath("minecraft", "entity.player.levelup"), 1));
    }

    public static double getTotalXpForLevel(int level) {
        double total = 0;
        for (int i = 1; i <= level; i++) {
            total += getXpRequiredForLevel(i);
        }
        return total;
    }

    public static void addXP(ServerPlayer player, int amount) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(XP_TAG)) {
            tag.putInt(XP_TAG, 0);
        }
        int currentXP = tag.getInt(XP_TAG);
        int newXP = currentXP + amount;
        tag.putInt(XP_TAG, newXP);
        
        // Sync with client
        NetworkHandler packet = new NetworkHandler("xp_update", String.valueOf(newXP));
        PacketDistributor.sendToPlayer(player, packet);
        
        // Check for level up
        checkLevelUp(player);
    }

    public static void setXP(ServerPlayer player, int amount) {
        CompoundTag tag = player.getPersistentData();
        tag.putInt(XP_TAG, amount);
        tag.putInt(LEVEL_TAG, 1);
        
        // Sync with client
        NetworkHandler packet = new NetworkHandler("xp_update", String.valueOf(amount));
        PacketDistributor.sendToPlayer(player, packet);
        
        // Check for level up
        checkLevelUp(player);
    }

    public static void addLevel(ServerPlayer player, int amount) {
        CompoundTag tag = player.getPersistentData();
        tag.putInt(LEVEL_TAG, tag.getInt(LEVEL_TAG) + amount);

        // Sync with client
        NetworkHandler packet = new NetworkHandler("level_update", String.valueOf(amount));
        PacketDistributor.sendToPlayer(player, packet);
        handlePointsUpdate(player, amount);
    }

    private static void checkLevelUp(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        int currentXP = tag.getInt(XP_TAG);
        int currentLevel = tag.getInt(LEVEL_TAG);
        
        // Calculate XP needed for next level
        int xpNeeded = (int) (BASE_LEVEL_REQUIREMENT * Math.pow(XP_SCALING, currentLevel - 1));
        
        while (currentXP >= xpNeeded) {
            currentXP -= xpNeeded;
            currentLevel++;
            xpNeeded = (int) (BASE_LEVEL_REQUIREMENT * Math.pow(XP_SCALING, currentLevel - 1));
            handlePointsUpdate(player, POINTS_PER_LEVEL);
        }
        while (currentXP < 0) {
            currentLevel--;
            xpNeeded = (int) (BASE_LEVEL_REQUIREMENT * Math.pow(XP_SCALING, currentLevel - 1));
            currentXP += xpNeeded;
            handlePointsUpdate(player, -POINTS_PER_LEVEL);
        }
        
        // Save updated values
        tag.putInt(XP_TAG, currentXP);
        tag.putInt(LEVEL_TAG, currentLevel);
        
        // Sync level with client
        NetworkHandler levelPacket = new NetworkHandler("level_update", String.valueOf(currentLevel));
        PacketDistributor.sendToPlayer(player, levelPacket);
    }
} 