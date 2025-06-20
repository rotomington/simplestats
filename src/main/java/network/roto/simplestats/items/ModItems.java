package network.roto.simplestats.items;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import network.roto.simplestats.Simplestats;


public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Simplestats.MODID);

    public static final DeferredItem<Item> WORNNOTEBOOK = ITEMS.register("worn_notebook",() -> new WornNotebook(new Item.Properties()));
    public static final DeferredItem<Item> FILAGREETEXTS = ITEMS.register("filagree_texts",() -> new FilagreeTexts(new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
