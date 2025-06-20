package network.roto.simplestats.leveling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import network.roto.simplestats.network.NetworkHandler;

@EventBusSubscriber(modid = "simplestats")
public class PlayerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            // Initialize and load perk levels
            PerkManager.loadPerkLevels(player);
            
            // Initialize XP and level if they don't exist
            CompoundTag tag = player.getPersistentData();
            if (!tag.contains(LevelManager.XP_TAG)) {
                tag.putInt(LevelManager.XP_TAG, 0);
            }
            if (!tag.contains(LevelManager.LEVEL_TAG)) {
                tag.putInt(LevelManager.LEVEL_TAG, 1);
            }
            if (!tag.contains(LevelManager.POINTS_TAG)) {
                tag.putInt(LevelManager.POINTS_TAG, 0);
            }
            
            // Get current values
            int xp = tag.getInt(LevelManager.XP_TAG);
            int level = tag.getInt(LevelManager.LEVEL_TAG);
            int points = tag.getInt(LevelManager.POINTS_TAG);
            
            // Send sync packets with actual values
            NetworkHandler xpPacket = new NetworkHandler("xp_update", String.valueOf(xp));
            PacketDistributor.sendToPlayer(serverPlayer, xpPacket);
            
            NetworkHandler levelPacket = new NetworkHandler("level_update", String.valueOf(level));
            PacketDistributor.sendToPlayer(serverPlayer, levelPacket);

            NetworkHandler pointsPacket = new NetworkHandler("points_update", String.valueOf(points));
            PacketDistributor.sendToPlayer(serverPlayer, pointsPacket);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        PerkManager.savePerkLevels(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        PerkManager.savePerkLevels(player);
        PerkManager.loadPerkLevels(player);
    }
} 