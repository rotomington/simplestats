package network.roto.simplestats.network;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import network.roto.simplestats.Simplestats;

@EventBusSubscriber(modid = Simplestats.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkEvents {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                NetworkHandler.TYPE,
                NetworkHandler.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        NetworkHandler::handleClientData,
                        NetworkHandler::handleServerData

                )
        );
    }
}
