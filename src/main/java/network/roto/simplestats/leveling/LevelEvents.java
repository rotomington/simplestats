package network.roto.simplestats.leveling;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = "simplestats")
public class LevelEvents {
    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (player.level().isClientSide) return;
        LevelManager.addXp(player, mob);
    }
} 