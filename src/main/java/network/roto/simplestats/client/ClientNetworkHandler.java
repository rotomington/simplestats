package network.roto.simplestats.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import network.roto.simplestats.Simplestats;
import network.roto.simplestats.leveling.LevelManager;
import network.roto.simplestats.leveling.PerkManager;
import net.minecraft.client.Minecraft;
import network.roto.simplestats.network.NetworkHandler;

@Mod(value = Simplestats.MODID, dist = Dist.CLIENT)
public class ClientNetworkHandler {
    public static void handleClientData(NetworkHandler packet, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> {
            switch (packet.command()) {
                case "xp_update":
                    LevelManager.XP = Integer.parseInt(packet.data());
                    break;
                case "level_update":
                    LevelManager.LEVEL = Integer.parseInt(packet.data());
                    break;
                case "points_update":
                    LevelManager.POINTS = Integer.parseInt(packet.data());
                    break;
                case "perk_update":
                    // Handle perk update confirmation
                    String[] parts = packet.data().split(",");
                    if (parts.length == 2) {
                        String perkId = parts[0];
                        int newLevel = Integer.parseInt(parts[1]);
                        // Update local perk level
                        if (Minecraft.getInstance().player != null) {
                            PerkManager.setPerkLevel(Minecraft.getInstance().player, perkId, newLevel);
                            PerkManager.savePerkLevels(Minecraft.getInstance().player);
                            // Force UI update
                            if (Minecraft.getInstance().screen instanceof SimpleStatsScreen statsScreen) {
                                statsScreen.updatePerkLevel(perkId, newLevel);
                                statsScreen.updatePerkButtons();
                            }
                        }
                    }
                    break;
            }
        });
    }
} 