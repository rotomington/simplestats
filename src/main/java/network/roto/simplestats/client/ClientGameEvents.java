package network.roto.simplestats.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import network.roto.simplestats.Simplestats;

import static network.roto.simplestats.client.ClientModEvents.OPEN_STATS_KEY;

@EventBusSubscriber(modid = Simplestats.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientGameEvents {
    @SubscribeEvent
    public static void onClientTickEvent(InputEvent.Key event) {
        boolean debugtest = false;
        if (debugtest) {
            Simplestats.LOGGER.info("onClientTickEvent was called");
            debugtest = true;
        }
        while (OPEN_STATS_KEY.get().consumeClick()) {
            Minecraft.getInstance().setScreen(new SimpleStatsScreen());
            Simplestats.LOGGER.info("pressing the key");
        }
    }
}
