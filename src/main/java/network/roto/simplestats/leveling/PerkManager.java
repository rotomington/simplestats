package network.roto.simplestats.leveling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import network.roto.simplestats.utils.ConfigUtils;
import java.util.List;
import java.util.Map;

public class PerkManager {
    private static List<Map<String, Object>> perks = null;
    private static final String PERK_DATA_TAG = "simplestats_perks";

    public static void loadConfig(String filePath) {
        Object data = ConfigUtils.readJsonConfig(filePath).get("perks");
        if (data instanceof List) {
            perks = (List<Map<String, Object>>) data;
        }
    }

    public static void onPerkLeveled(Player player, String perkId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (perks == null) return;

        for (Map<String, Object> perk : perks) {
            if (perkId.equals(perk.get("id"))) {
                int currentLevel = getPerkLevel(player, perkId);
                String commandTemplate = (String) perk.get("command");
                
                if (commandTemplate != null) {
                    // Replace {level} with the actual level (0-based for effects)
                    String command = commandTemplate.replace("{level}", String.valueOf(currentLevel - 1));
                    MinecraftServer server = serverPlayer.server;
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(),
                        command.replace("@p", player.getName().getString())
                    );
                }
            }
        }
    }

    public static int getPerkLevel(Player player, String perkId) {
        CompoundTag perkData = player.getPersistentData().getCompound(PERK_DATA_TAG);
        return perkData.getInt(perkId);
    }

    public static void setPerkLevel(Player player, String perkId, int level) {
        CompoundTag perkData = player.getPersistentData().getCompound(PERK_DATA_TAG);
        perkData.putInt(perkId, level);
        player.getPersistentData().put(PERK_DATA_TAG, perkData);
    }

    public static void savePerkLevels(Player player) {
        if (perks == null) return;
        CompoundTag perkData = new CompoundTag();
        for (Map<String, Object> perk : perks) {
            String id = (String) perk.get("id");
            int level = getPerkLevel(player, id);
            perkData.putInt(id, level);
        }
        player.getPersistentData().put(PERK_DATA_TAG, perkData);
    }

    public static void loadPerkLevels(Player player) {
        if (perks == null) return;
        CompoundTag perkData = player.getPersistentData().getCompound(PERK_DATA_TAG);
        for (Map<String, Object> perk : perks) {
            String id = (String) perk.get("id");
            if (perkData.contains(id)) {
                int level = perkData.getInt(id);
                setPerkLevel(player, id, level);
            }
        }
    }

    public static int getMaxLevel(String perkId) {
        if (perks == null) return 0;
        for (Map<String, Object> perk : perks) {
            if (perkId.equals(perk.get("id"))) {
                return perk.containsKey("maxLevel") ? ((Number)perk.get("maxLevel")).intValue() : 0;
            }
        }
        return 0;
    }

    public static int getXpCost(String perkId) {
        if (perks == null) return 0;
        for (Map<String, Object> perk : perks) {
            if (perkId.equals(perk.get("id"))) {
                return perk.containsKey("xpCost") ? ((Number)perk.get("xpCost")).intValue() : 0;
            }
        }
        return 0;
    }

    public static List<Map<String, Object>> getPerks() {
        return perks;
    }
} 