package network.roto.simplestats.leveling;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import network.roto.simplestats.network.NetworkHandler;
import network.roto.simplestats.utils.Config;
import network.roto.simplestats.utils.Perks;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PerkManager {
    private static List<Perks> perks = new ArrayList<>(List.of());
    private static final String PERK_DATA_TAG = "simplestats_perks";

    public static List<Perks> getPerks() {
        return perks;
    }

    public static void sendDataToServer(String data) {
        NetworkHandler packet = new NetworkHandler("perk", data);
        PacketDistributor.sendToServer(packet);
    }

    public void updatePerkList (Perks perk) {
        perks.add(perk);
    }

    public static void loadPerkConfig (PerkManager perkManager){
        // Clear existing perks before loading new ones
        perks.clear();
        Perks perk = new Perks("", "", "",0,0,"","");
        List<String> encodedConfig = Config.CONFIG.perkList.get().stream().map(obj -> (String) obj).toList();
        if (!encodedConfig.isEmpty() && encodedConfig != null) {
            for (String str : encodedConfig) {
                perk = perk.decodePerks(str);
                perkManager.updatePerkList(perk);
            }
        }
    }

    public static void onPerkLeveled(Player player, String perkId) {
        if (perks == null) return;

        for (Perks perk : perks) {
            if (perkId.equals(perk.id)) {
                int currentLevel = getPerkLevel(player, perkId);
                String commandTemplate = perk.levelUpCommand;
                
                if (commandTemplate != null) {
                    // Replace {level} with the actual level (0-based for effects)
                    String command = commandTemplate.replace("{level}", String.valueOf(currentLevel - 1));
                    sendDataToServer(command);

                }
            }
        }
    }
    public static void onPerkDeleveled(Player player, String perkId) {
        if (perks == null) return;

        for (Perks perk : perks) {
            if (perkId.equals(perk.id)) {
                int currentLevel = getPerkLevel(player, perkId);
                String commandTemplate = perk.levelDownCommand;

                if (commandTemplate != null) {
                    // Replace {level} with the actual level (0-based for effects)
                    String command = commandTemplate.replace("{level}", String.valueOf(currentLevel - 1));
                    sendDataToServer(command);
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
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getPersistentData().put(PERK_DATA_TAG, perkData);
        }
    }

    public static void savePerkLevels(Player player) {
        if (perks == null) return;
        CompoundTag perkData = new CompoundTag();
        for (Perks perk : perks) {
            String id = perk.id;
            int level = getPerkLevel(player, id);
            perkData.putInt(id, level);
        }
        player.getPersistentData().put(PERK_DATA_TAG, perkData);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getPersistentData().put(PERK_DATA_TAG, perkData);
        }
    }

    public static void loadPerkLevels(Player player) {
        if (perks == null) return;
        CompoundTag perkData = player.getPersistentData().getCompound(PERK_DATA_TAG);
        for (Perks perk : perks) {
            String id = perk.id;
            if (perkData.contains(id)) {
                int level = perkData.getInt(id);
                setPerkLevel(player, id, level);
            } else {
                setPerkLevel(player, id, 0);
            }
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getPersistentData().put(PERK_DATA_TAG, perkData);
        }
    }

    public static int getMaxLevel(String perkId) {
        if (perks == null) return 0;
        for (Perks perk : perks) {
            if (perkId.equals(perk.id)) {
                return perk.maxLevel;
            }
        }
        return 0;
    }

    public static int getXpCost(String perkId) {
        if (perks == null) return 0;
        for (Perks perk : perks) {
            if (perkId.equals(perk.id)) {
                return perk.cost;
            }
        }
        return 0;
    }
} 