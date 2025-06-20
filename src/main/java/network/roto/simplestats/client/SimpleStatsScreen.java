package network.roto.simplestats.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import network.roto.simplestats.Simplestats;
import network.roto.simplestats.leveling.PerkManager;
import network.roto.simplestats.leveling.LevelManager;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import network.roto.simplestats.utils.Perks;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.network.PacketDistributor;
import network.roto.simplestats.network.NetworkHandler;
import java.util.ArrayList;

public class SimpleStatsScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Simplestats.MODID, "textures/screens/stats_bg.png");
    private static final ResourceLocation XP_BAR_BG_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/sprites/hud/experience_bar_background.png");
    private static final ResourceLocation XP_BAR_FG_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/sprites/hud/experience_bar_progress.png");
    private final Player player;
    private List<Perks> perks;
    private final Map<String, Integer> perkLevels = new HashMap<>();
    private static final int BUTTON_WIDTH = 15;
    private static final int BUTTON_HEIGHT = 15;
    private String errorMessage = null;
    private int errorTicks = 0;
    private int scrollOffset = 0;
    private final int maxVisibleRows = 4;
    private final int rowHeight = 36;
    private int totalRows = 0;
    private int scrollBarX, scrollBarY, scrollBarHeight, scrollBarWidth;
    private boolean scrolling = false;
    private int imageWidth = 220;
    private int imageHeight = 190;
    private int leftPos;
    private int topPos;
    private long lastPerkUpdate = 0;
    private static final long PERK_UPDATE_INTERVAL = 1000;
    private boolean needsPerkUpdate = true;
    private List<Button> perkButtons = new ArrayList<>();

    public SimpleStatsScreen() {
        super(Component.translatable("screen.simplestats.title"));
        this.player = Minecraft.getInstance().player;
        if (player != null) {
            this.perks = PerkManager.getPerks();
            if (perks != null) {
                // Request perk levels from server for each perk
                for (Perks perk : perks) {
                    String id = perk.id;
                    // Initialize with 0, will be updated when server responds
                    perkLevels.put(id, 0);
                    NetworkHandler perkRequest = new NetworkHandler("request_perk", id);
                    PacketDistributor.sendToServer(perkRequest);
                }
            }
            // Request initial data sync
            LevelManager.requestDatafromServer("request", "points");
            // Request perk level logging
            NetworkHandler logRequest = new NetworkHandler("log_perks", "");
            PacketDistributor.sendToServer(logRequest);
            needsPerkUpdate = true;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Force initial data sync
        if (player != null) {
            LevelManager.requestDatafromServer("request", "points");
            needsPerkUpdate = true;
        }
        
        updatePerkButtons();
    }

    public void updatePerkButtons() {
        for (Button button : perkButtons) {
            this.removeWidget(button);
        }
        perkButtons.clear();

        if (perks == null || perks.isEmpty()) return;

        int y = this.topPos + 43;
        int x = this.leftPos + 12;

        for (int j = 0; j < perks.size(); j++) {
            if (j < scrollOffset || j >= scrollOffset + maxVisibleRows) continue;
            
            Perks perk = perks.get(j);
            String id = perk.id;
            int level = perkLevels.getOrDefault(id, 0);
            int maxLevel = PerkManager.getMaxLevel(id);
            int xpCost = PerkManager.getXpCost(id);
            int rowY = y + (j - scrollOffset) * rowHeight;

            Button upgradeButton = Button.builder(Component.literal("+"), btn -> {
                handlePerkUpgrade(id, level, maxLevel, xpCost);
            }).pos(x + this.imageWidth - 45, rowY-5).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
            
            Button downgradeButton = Button.builder(Component.literal("-"), btn -> {
                handlePerkDowngrade(id, level, xpCost);
            }).pos(x + this.imageWidth - 45, rowY+10).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();

            this.addRenderableWidget(upgradeButton);
            this.addRenderableWidget(downgradeButton);
            perkButtons.add(upgradeButton);
            perkButtons.add(downgradeButton);
        }
    }

    public void updatePerkLevel(String perkId, int level) {
        perkLevels.put(perkId, level);
    }

    private void handlePerkUpgrade(String id, int currentLevel, int maxLevel, int xpCost) {
        if (currentLevel >= maxLevel) {
            showError(Component.translatable("screen.simplestate.error.maxlevel"));
            return;
        }
        if (!LevelManager.canPay(player, xpCost)) {
            showError(Component.translatable("screen.simplestate.error.tooexpensive"));
            return;
        }
        // Update perk level and points
        perkLevels.put(id, currentLevel + 1);
        PerkManager.setPerkLevel(player, id, currentLevel + 1);
        PerkManager.onPerkLeveled(player, id);
        // Save the changes
        PerkManager.savePerkLevels(player);
        // Send perk update to server
        NetworkHandler perkPacket = new NetworkHandler("perk_update", id + "," + (currentLevel + 1));
        PacketDistributor.sendToServer(perkPacket);
        // Send points update to server
        NetworkHandler pointsPacket = new NetworkHandler("points_update", String.valueOf(-xpCost));
        PacketDistributor.sendToServer(pointsPacket);
        // Update local points
        LevelManager.POINTS -= xpCost;
        // Request points update immediately
        LevelManager.requestDatafromServer("request", "points");
        needsPerkUpdate = true;
        updatePerkButtons();
    }

    private void handlePerkDowngrade(String id, int currentLevel, int xpCost) {
        if (currentLevel == 0) {
            showError(Component.translatable("screen.simplestate.error.minlevel"));
            return;
        }
        // Update perk level and points
        perkLevels.put(id, currentLevel - 1);
        PerkManager.setPerkLevel(player, id, currentLevel - 1);
        PerkManager.onPerkDeleveled(player, id);
        // Save the changes
        PerkManager.savePerkLevels(player);
        // Send perk update to server
        NetworkHandler perkPacket = new NetworkHandler("perk_update", id + "," + (currentLevel - 1));
        PacketDistributor.sendToServer(perkPacket);
        // Send points update to server
        NetworkHandler pointsPacket = new NetworkHandler("points_update", String.valueOf(xpCost));
        PacketDistributor.sendToServer(pointsPacket);
        // Update local points
        LevelManager.POINTS += xpCost;
        // Request points update immediately
        LevelManager.requestDatafromServer("request", "points");
        needsPerkUpdate = true;
        updatePerkButtons();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderMenuBackground(guiGraphics);
        guiGraphics.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (perks == null) return false;
        int maxScroll = Math.max(0, totalRows - maxVisibleRows);
        int oldScrollOffset = scrollOffset;
        
        if (scrollY < 0 && scrollOffset < maxScroll) {
            scrollOffset++;
        } else if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
        }
        
        if (oldScrollOffset != scrollOffset) {
            updatePerkButtons();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
            this.updatePerkButtons();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Update perk data periodically
        long currentTime = System.currentTimeMillis();
        if (needsPerkUpdate || currentTime - lastPerkUpdate > PERK_UPDATE_INTERVAL) {
            if (player != null) {
                // Request both points and perk data
                LevelManager.requestDatafromServer("request", "points");
                for (Perks perk : perks) {
                    NetworkHandler perkRequest = new NetworkHandler("request_perk", perk.id);
                    PacketDistributor.sendToServer(perkRequest);
                }
                lastPerkUpdate = currentTime;
                needsPerkUpdate = false;
            }
        }
        
        renderXpBar(guiGraphics);
        renderPerks(guiGraphics);

        // Show error message
        if (errorMessage != null && errorTicks > 0) {
            guiGraphics.drawCenteredString(this.font, errorMessage, this.width / 2, this.topPos + this.imageHeight + 10, 0xFF5555);
            errorTicks--;
            if (errorTicks == 0) errorMessage = null;
        }
    }

    public void renderXpBar(GuiGraphics guiGraphics) {
        if (player != null) {
            int xp = LevelManager.getXp(player);
            int level = LevelManager.getLevel(player);
            int points = LevelManager.getPoints(player);
            int xpBarWidth = 182;
            int xpBarHeight = 5;
            int xpBarX = this.leftPos + (this.imageWidth - xpBarWidth) / 2;
            int xpBarY = this.topPos + 29;

            guiGraphics.drawCenteredString(this.font, "Points Remaining:  " + points,
                    this.width / 2, xpBarY - 22, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "Level " + level + " XP: " + xp + " / " + LevelManager.getXpRequiredForLevel(level + 1),
                this.width / 2, xpBarY - 11, 0xFFFFFF);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0,0,1);
            guiGraphics.blit(XP_BAR_BG_TEXTURE, xpBarX, xpBarY, 0, 0, xpBarWidth, xpBarHeight, 182, 5);
            guiGraphics.pose().popPose();

            double progress = LevelManager.getXpProgress(player);
            int fillWidth = (int) (progress * xpBarWidth);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0,0,100);
            guiGraphics.blit(XP_BAR_FG_TEXTURE, xpBarX, xpBarY, 0, 0, fillWidth, xpBarHeight, 182, 5);
            guiGraphics.pose().popPose();
        }
    }

    public void renderPerks(GuiGraphics guiGraphics) {
        if (perks != null && !perks.isEmpty()) {
            int y = this.topPos + 43;
            int x = this.leftPos + 12;
            int i = 0;

            for (int j = 0; j < perks.size(); j++) {
                if (j < scrollOffset || j >= scrollOffset + maxVisibleRows) continue;
                Perks perk = perks.get(j);
                String id = perk.id;
                String name = perk.name;
                int level = perkLevels.getOrDefault(id, 0);
                int maxLevel = PerkManager.getMaxLevel(id);
                int xpCost = PerkManager.getXpCost(id);
                int rowY = y + (i++) * rowHeight;

                guiGraphics.fill(x - 4, rowY - 6, x + this.imageWidth - 49, rowY + 28, 0x80000000);

                if (perk.icon != null && !perk.icon.isEmpty()) {
                    String iconStr = perk.icon;
                    ResourceLocation itemId = null;
                    if (iconStr.startsWith("minecraft:item/")) {
                        itemId = ResourceLocation.tryParse("minecraft" + iconStr.substring("minecraft:item/".length()));
                    } else if (iconStr.contains(":")) {
                        String[] split = iconStr.split(":");
                        if (split.length == 2) {
                            itemId = ResourceLocation.tryParse(iconStr);
                        } else if (split.length == 3 && split[1].equals("item")) {
                            itemId = ResourceLocation.tryParse(split[0] + ":" + split[2]);
                        }
                    }
                    if (itemId != null) {
                        Item item = BuiltInRegistries.ITEM.get(itemId);
                        if (item != null && BuiltInRegistries.ITEM.getKey(item) != BuiltInRegistries.ITEM.getDefaultKey()) {
                            ItemStack stack = new ItemStack(item);
                            guiGraphics.renderItem(stack, x, rowY + 2);
                        }
                    }
                }

                guiGraphics.drawString(this.font, name, x + 20, rowY - 2, 0xFFFFFF);
                guiGraphics.drawString(this.font, 
                    "Level: " + level + "/" + maxLevel, 
                    x + 20, rowY + 8, 0xAAAAAA);
                guiGraphics.drawString(this.font, 
                    "Cost: " + xpCost, 
                    x + 20, rowY + 18, 0xAAAAAA);
            }

            if (totalRows > maxVisibleRows) {
                scrollBarX = x + this.imageWidth - 26;
                scrollBarY = y - 4;
                scrollBarHeight = maxVisibleRows * rowHeight - 4;
                scrollBarWidth = 6;

                double percent = scrollOffset / (double)Math.max(1, totalRows - maxVisibleRows);
                int barHeight = Math.max(20, (int)(scrollBarHeight * (maxVisibleRows / (double)totalRows)));
                int barY = scrollBarY + (int)((scrollBarHeight - barHeight) * percent);
                
                guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight, 0x80000000);
                guiGraphics.fill(scrollBarX, barY, scrollBarX + scrollBarWidth, barY + barHeight, 0x90AAAAAA);
            }

            totalRows = perks.size();
        } else if (perks.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, 
                Component.translatable("screen.simplestats.error.no_perks"), 
                this.width/2, this.height/2, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(this.font, 
                Component.translatable("screen.simplestats.error.null_perks"), 
                this.width/2, this.height/2, 0xFFFFFF);
        }
    }

    private void showError(Component msg) {
        this.errorMessage = msg.getString();
        this.errorTicks = 60;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 