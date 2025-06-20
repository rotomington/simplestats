package network.roto.simplestats.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import network.roto.simplestats.items.components.ModDataComponents;
import network.roto.simplestats.leveling.LevelManager;
import network.roto.simplestats.sounds.ModSounds;

import java.util.List;

public class FilagreeTexts extends Item {

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (stack.get(ModDataComponents.POINTS_AWARDED) != null && stack.get(ModDataComponents.POINTS_AWARDED) != 0){
            LevelManager.givePoints(player, stack.get(ModDataComponents.POINTS_AWARDED));
            if (!player.getAbilities().instabuild) { // Don't consume in creative mode
                stack.shrink(1);
            }
            if (!(player instanceof ServerPlayer)){
                player.sendSystemMessage(Component.translatable("gui.simplestats.notebook.use").append(Component.literal(stack.get(ModDataComponents.POINTS_AWARDED) + " Points")));
                level.playSound(null, player.getOnPos(), ModSounds.BOOK_USE.get(), SoundSource.NEUTRAL);
            }
        } else {
            player.sendSystemMessage(Component.translatable("gui.simplestats.error.useless"));
        }

        return super.use(level, player, usedHand);

    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flags){
        tooltipComponents.add(Component.translatable("gui.simplestats.filagree.flavor"));
        if (stack.get(ModDataComponents.POINTS_AWARDED) != null && stack.get(ModDataComponents.POINTS_AWARDED) != 0){
            tooltipComponents.add(Component.translatable("gui.simplestats.notebook.hint").append(Component.literal(stack.get(ModDataComponents.POINTS_AWARDED) + " Points")));
        }
    }
    public FilagreeTexts(Properties properties) {
        super(properties);
    }
}
