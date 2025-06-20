package network.roto.simplestats.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import network.roto.simplestats.Simplestats;
import network.roto.simplestats.client.ClientNetworkHandler;
import network.roto.simplestats.leveling.LevelManager;
import network.roto.simplestats.leveling.PerkManager;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.RegistryFriendlyByteBuf;


public record NetworkHandler(String command, String data) implements CustomPacketPayload {
    public static final Type<NetworkHandler> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Simplestats.MODID, "stats_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkHandler> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            NetworkHandler::command,
            ByteBufCodecs.STRING_UTF8,
            NetworkHandler::data,
            NetworkHandler::new
    );

    public static void handleClientData(NetworkHandler packet, IPayloadContext context){
        if (FMLEnvironment.dist.isClient()){
            ClientNetworkHandler.handleClientData(packet, context);
        }
        return;
    }

    public static void handleServerData(NetworkHandler packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            // Process your data here
            switch (packet.command()){
                case "perk":
                    // Replace {level} with the actual level (0-based for effects)
                    MinecraftServer server = player.server;
                    server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(),
                            packet.data().replace("@p", player.getName().getString())
                    );
                    break;
                case "points_update":
                    // Handle points update from client
                    int pointsChange = Integer.parseInt(packet.data());
                    LevelManager.handlePointsUpdate(player, pointsChange);
                    // Send confirmation back to client
                    int currentPoints = LevelManager.getPoints(player);
                    PacketDistributor.sendToPlayer(player, new NetworkHandler("points_update", String.valueOf(currentPoints)));
            
                    break;
                case "request":
                    String tagName;
                    String responseType;
                    switch (packet.data()) {
                        case "xp":
                            tagName = "simplestats_xp";
                            responseType = "xp_update";
                            break;
                        case "level":
                            tagName = "simplestats_level";
                            responseType = "level_update";
                            break;
                        case "points":
                            tagName = "simplestats_points";
                            responseType = "points_update";
                            break;
                        default:
                            return;
                    }
                    int data = player.getPersistentData().getInt(tagName);
                    PacketDistributor.sendToPlayer(player, new NetworkHandler(responseType, String.valueOf(data)));
                    break;
                case "perk_update":
                    // Handle perk level update
                    String[] parts = packet.data().split(",");
                    if (parts.length == 2) {
                        String perkId = parts[0];
                        int newLevel = Integer.parseInt(parts[1]);
                        PerkManager.setPerkLevel(player, perkId, newLevel);
                        PerkManager.savePerkLevels(player);
                        // Send confirmation back to client
                        PacketDistributor.sendToPlayer(player, new NetworkHandler("perk_update", packet.data()));
                    }
                    break;
                case "sync_xp":
                    // Initialize and send current XP to client
                    CompoundTag tag = player.getPersistentData();
                    if (!tag.contains("simplestats_xp")) {
                        tag.putInt("simplestats_xp", 0);
                    }
                    int xp = tag.getInt("simplestats_xp");
                    PacketDistributor.sendToPlayer(player, new NetworkHandler("xp_update", String.valueOf(xp)));
                    break;
                case "sync_level":
                    // Initialize and send current level to client
                    tag = player.getPersistentData();
                    if (!tag.contains("simplestats_level")) {
                        tag.putInt("simplestats_level", 1);
                    }
                    int level = tag.getInt("simplestats_level");
                    PacketDistributor.sendToPlayer(player, new NetworkHandler("level_update", String.valueOf(level)));
                    break;
                case "sync_points":
                    // Initialize and send current level to client
                    tag = player.getPersistentData();
                    if (!tag.contains("simplestats_points")) {
                        tag.putInt("simplestats_points", 0);
                    }
                    int points = tag.getInt("simplestats_points");
                    PacketDistributor.sendToPlayer(player, new NetworkHandler("points_update", String.valueOf(points)));
                    break;
                case "log_perks":
                    // Log all perk levels for the player
                    CompoundTag perkData = player.getPersistentData().getCompound("simplestats_perks");
                    for (String perkId : perkData.getAllKeys()) {
                        int perkLevel = perkData.getInt(perkId);
                    }
                    break;
                case "request_perk":
                    // Send perk level back to client
                    String perkId = packet.data();
                    int perkLevel = player.getPersistentData().getCompound("simplestats_perks").getInt(perkId);
                    PacketDistributor.sendToPlayer(player, new NetworkHandler("perk_update", perkId + "," + perkLevel));
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
