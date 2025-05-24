package network.roto.simplestats.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.jarjar.nio.util.Lazy;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import network.roto.simplestats.Simplestats;

@EventBusSubscriber(modid = Simplestats.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    public static final Lazy<KeyMapping> OPEN_STATS_KEY = Lazy.of(() -> new KeyMapping(
            "key.simplestats.open_stats",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.misc")
    );
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS_KEY.get());
    }

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
