package network.roto.simplestats.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import network.roto.simplestats.leveling.PerkManager;
import network.roto.simplestats.leveling.LevelManager;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class SimpleStatsScreen extends Screen {
    private final Player player;
    private final List<Map<String, Object>> perks;
    private final Map<String, Integer> perkLevels = new HashMap<>();
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private String errorMessage = null;
    private int errorTicks = 0;
    private int scrollOffset = 0;
    private int maxVisibleRows = 7;
    private int rowHeight = 30;
    private int totalRows = 0;
    private int scrollBarX, scrollBarY, scrollBarHeight, scrollBarWidth;
    private boolean scrolling = false;

    public SimpleStatsScreen() {
        super(Component.literal("Simple Stats"));
        this.player = Minecraft.getInstance().player;
        this.perks = PerkManager.getPerks();
        // Load perk levels from player persistent data
        if (perks != null && player != null) {
            PerkManager.loadPerkLevels(player);
            for (Map<String, Object> perk : perks) {
                String id = (String) perk.get("id");
                int level = PerkManager.getPerkLevel(player, id);
                perkLevels.put(id, level);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        if (perks == null) return;
        int y = this.height / 2 - 40;
        int x = this.width / 2 - 100;
        int i = 0;
        totalRows = perks.size();
        // Only add visible buttons
        for (int j = 0; j < perks.size(); j++) {
            if (j < scrollOffset || j >= scrollOffset + maxVisibleRows) continue;
            Map<String, Object> perk = perks.get(j);
            String id = (String) perk.get("id");
            String name = (String) perk.get("name");
            int level = perkLevels.getOrDefault(id, 0);
            int maxLevel = PerkManager.getMaxLevel(id);
            int xpCost = PerkManager.getXpCost(id);
            int rowY = y + (j - scrollOffset) * rowHeight;
            // Upgrade button
            this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                int currentLevel = perkLevels.getOrDefault(id, 0);
                double xp = LevelManager.getXp(player);
                if (currentLevel >= maxLevel) {
                    showError("Max level reached");
                    return;
                }
                if (xp < xpCost) {
                    showError("Not enough XP");
                    return;
                }
                perkLevels.put(id, currentLevel + 1);
                PerkManager.setPerkLevel(player, id, currentLevel + 1);
                // Deduct XP
                double newXp = xp - xpCost;
                player.getPersistentData().putDouble("simplestats_xp", newXp);
                PerkManager.onPerkLeveled(player, id);
                btn.active = false;
            }).pos(x + 180, rowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());
            // Downgrade button
            this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
                int currentLevel = perkLevels.getOrDefault(id, 0);
                if (currentLevel <= 0) {
                    showError("Already at 0");
                    return;
                }
                perkLevels.put(id, currentLevel - 1);
                PerkManager.setPerkLevel(player, id, currentLevel - 1);
                btn.active = false;
            }).pos(x + 155, rowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
        // Scrollbar geometry
        scrollBarX = x + 210;
        scrollBarY = y;
        scrollBarHeight = maxVisibleRows * rowHeight;
        scrollBarWidth = 8;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (perks == null) return false;
        int maxScroll = Math.max(0, totalRows - maxVisibleRows);
        if (scrollY < 0 && scrollOffset < maxScroll) {
            scrollOffset++;
            this.rebuildWidgets();
            return true;
        } else if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Scrollbar drag
        if (mouseX >= scrollBarX && mouseX <= scrollBarX + scrollBarWidth && mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarHeight) {
            scrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && perks != null) {
            int maxScroll = Math.max(0, totalRows - maxVisibleRows);
            double percent = (mouseY - scrollBarY) / (double)scrollBarHeight;
            scrollOffset = Math.max(0, Math.min(maxScroll, (int)(percent * maxScroll + 0.5)));
            this.rebuildWidgets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    private void showError(String msg) {
        this.errorMessage = msg;
        this.errorTicks = 60; // Show for 3 seconds (20 ticks/sec)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(this.font, this.title.getString(), this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        
        // Show XP and XP bar
        if (player != null) {
            double xp = LevelManager.getXp(player);
            int level = LevelManager.getLevel(player);
            int xpBarWidth = 200;
            int xpBarHeight = 5;
            int xpBarX = this.width / 2 - xpBarWidth / 2;
            int xpBarY = this.height / 2 - 80;
            
            // Draw Level and XP text
            guiGraphics.drawCenteredString(this.font, "Level " + level, this.width / 2, xpBarY - 24, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "XP: " + (int)xp + " / " + (int)LevelManager.getXpRequiredForLevel(level + 1), this.width / 2, xpBarY - 12, 0xFFFF00);
            
            // Draw XP bar background
            guiGraphics.fill(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, 0xFF555555);
            
            // Draw XP bar fill
            double progress = LevelManager.getXpProgress(player);
            int fillWidth = (int)(progress * xpBarWidth);
            guiGraphics.fill(xpBarX, xpBarY, xpBarX + fillWidth, xpBarY + xpBarHeight, 0xFF00FF00);
            
            // Draw XP bar border
            guiGraphics.fill(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + 1, 0xFF000000);
            guiGraphics.fill(xpBarX, xpBarY + xpBarHeight - 1, xpBarX + xpBarWidth, xpBarY + xpBarHeight, 0xFF000000);
            guiGraphics.fill(xpBarX, xpBarY, xpBarX + 1, xpBarY + xpBarHeight, 0xFF000000);
            guiGraphics.fill(xpBarX + xpBarWidth - 1, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, 0xFF000000);
        }
        
        // Render perks
        if (perks != null) {
            int y = this.height / 2 - 40;
            int x = this.width / 2 - 100;
            int i = 0;
            for (int j = 0; j < perks.size(); j++) {
                if (j < scrollOffset || j >= scrollOffset + maxVisibleRows) continue;
                Map<String, Object> perk = perks.get(j);
                String id = (String) perk.get("id");
                String name = (String) perk.get("name");
                int level = perkLevels.getOrDefault(id, 0);
                int maxLevel = PerkManager.getMaxLevel(id);
                int xpCost = PerkManager.getXpCost(id);
                int rowY = y + (i++) * rowHeight + 6;
                // Draw icon if present
                if (perk.containsKey("icon")) {
                    String iconStr = (String) perk.get("icon");
                    ResourceLocation itemId = null;
                    if (iconStr.startsWith("minecraft:item/")) {
                        itemId = ResourceLocation.tryParse("minecraft" + iconStr.substring("minecraft:item/".length()));
                    } else if (iconStr.contains(":")) {
                        // Support modded items: e.g. 'modid:itemid'
                        String[] split = iconStr.split(":");
                        if (split.length == 2) {
                            itemId = ResourceLocation.tryParse(iconStr);
                        } else if (split.length == 3 && split[1].equals("item")) {
                            // Support 'modid:item/itemid'
                            itemId = ResourceLocation.tryParse(split[0] + ":" + split[2]);
                        }
                    }
                    if (itemId != null) {
                        Item item = BuiltInRegistries.ITEM.get(itemId);
                        if (item != null && BuiltInRegistries.ITEM.getKey(item) != BuiltInRegistries.ITEM.getDefaultKey()) {
                            ItemStack stack = new ItemStack(item);
                            guiGraphics.renderItem(stack, x, rowY - 2);
                        }
                    }
                }
                guiGraphics.drawString(this.font, name + " (Level: " + level + "/" + maxLevel + ", XP cost: " + xpCost + ")", x + 20, rowY, 0xFFFFFF);
            }
            // Draw scrollbar
            if (totalRows > maxVisibleRows) {
                double percent = scrollOffset / (double)Math.max(1, totalRows - maxVisibleRows);
                int barHeight = Math.max(20, (int)(scrollBarHeight * (maxVisibleRows / (double)totalRows)));
                int barY = scrollBarY + (int)((scrollBarHeight - barHeight) * percent);
                guiGraphics.fill(scrollBarX, barY, scrollBarX + scrollBarWidth, barY + barHeight, 0xFFAAAAAA);
            }
        }
        // Show error message
        if (errorMessage != null && errorTicks > 0) {
            guiGraphics.drawCenteredString(this.font, errorMessage, this.width / 2, this.height / 2 + 80, 0xFF5555);
            errorTicks--;
            if (errorTicks == 0) errorMessage = null;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 