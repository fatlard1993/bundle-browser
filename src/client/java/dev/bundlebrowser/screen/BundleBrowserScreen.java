package dev.bundlebrowser.screen;

import dev.bundlebrowser.util.BundleHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundle Browser screen that looks and feels like opening a chest.
 * Uses vanilla container styling for a native Minecraft feel.
 */
public class BundleBrowserScreen extends Screen {
    // Standard container dimensions
    private static final int SLOT_SIZE = 18;
    private static final int BORDER_LEFT = 7;
    private static final int BORDER_TOP = 17;
    private static final int TITLE_Y = 6;

    private final int bundleSlotId;
    private final HandledScreen<?> parentScreen;
    private final ScreenHandler screenHandler;
    private final int syncId;

    private List<ItemStack> contents;
    private List<BundleSlotWidget> slotWidgets;

    // Container positioning
    private int containerX;
    private int containerY;
    private int containerWidth;
    private int containerHeight;
    private int columns;
    private int rows;

    // Hovered slot for tooltip rendering
    private BundleSlotWidget hoveredSlot;

    public BundleBrowserScreen(ItemStack bundleStack, int bundleSlotId, HandledScreen<?> parentScreen) {
        super(Text.translatable("bundlebrowser.screen.title"));
        this.bundleSlotId = bundleSlotId;
        this.parentScreen = parentScreen;
        this.screenHandler = parentScreen.getScreenHandler();
        this.syncId = screenHandler.syncId;
        this.slotWidgets = new ArrayList<>();
        this.contents = BundleHelper.getContents(bundleStack);
    }

    @Override
    protected void init() {
        super.init();
        slotWidgets.clear();
        hoveredSlot = null;

        // Refresh contents from current bundle state
        ItemStack currentBundle = screenHandler.getSlot(bundleSlotId).getStack();
        if (!BundleHelper.isBundle(currentBundle) || BundleHelper.isEmpty(currentBundle)) {
            close();
            return;
        }
        contents = BundleHelper.getContents(currentBundle);
        int itemCount = contents.size();

        // Calculate grid size (like a chest: up to 9 columns, variable rows)
        if (itemCount <= 9) {
            columns = Math.max(1, itemCount);
            rows = 1;
        } else if (itemCount <= 18) {
            columns = 9;
            rows = 2;
        } else if (itemCount <= 27) {
            columns = 9;
            rows = 3;
        } else if (itemCount <= 36) {
            columns = 9;
            rows = 4;
        } else if (itemCount <= 45) {
            columns = 9;
            rows = 5;
        } else {
            columns = 9;
            rows = 6;
        }

        // Container dimensions (like chest texture)
        containerWidth = BORDER_LEFT * 2 + columns * SLOT_SIZE + 4;
        containerHeight = BORDER_TOP + rows * SLOT_SIZE + 14 + 28; // Extra space for button

        // Center the container
        containerX = (width - containerWidth) / 2;
        containerY = (height - containerHeight) / 2;

        // Create slot widgets
        int slotStartX = containerX + BORDER_LEFT + 1;
        int slotStartY = containerY + BORDER_TOP + 1;

        for (int i = 0; i < contents.size() && i < rows * columns; i++) {
            int col = i % columns;
            int row = i / columns;
            int x = slotStartX + col * SLOT_SIZE;
            int y = slotStartY + row * SLOT_SIZE;

            BundleSlotWidget slot = new BundleSlotWidget(x, y, contents.get(i), i, this::onSlotClicked);
            slotWidgets.add(slot);
            addDrawableChild(slot);
        }

        // Add "Empty All" button at the bottom
        int buttonWidth = 60;
        int buttonY = containerY + BORDER_TOP + rows * SLOT_SIZE + 10;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("bundlebrowser.screen.empty"),
                this::onEmptyButtonClicked
        ).dimensions(
                containerX + (containerWidth - buttonWidth) / 2,
                buttonY,
                buttonWidth,
                20
        ).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent overlay (don't call renderBackground - it causes double blur)
        context.fill(0, 0, width, height, 0xC0101010);

        // Draw container background (chest-like appearance)
        drawContainerBackground(context);

        // Draw title
        context.drawText(
                textRenderer,
                title,
                containerX + 8,
                containerY + TITLE_Y,
                0x404040,
                false
        );

        // Track hovered slot for tooltip
        hoveredSlot = null;
        for (BundleSlotWidget slot : slotWidgets) {
            if (slot.isHovered()) {
                hoveredSlot = slot;
                break;
            }
        }

        // Render all children (slots, buttons)
        super.render(context, mouseX, mouseY, delta);

        // Render tooltip last (on top of everything)
        if (hoveredSlot != null) {
            hoveredSlot.renderTooltip(context, mouseX, mouseY);
        }
    }

    private void drawContainerBackground(DrawContext context) {
        // Draw a chest-like container background
        // Main panel
        context.fill(containerX, containerY, containerX + containerWidth, containerY + containerHeight, 0xFFC6C6C6);

        // Top border (lighter - 3D effect)
        context.fill(containerX, containerY, containerX + containerWidth, containerY + 2, 0xFFFFFFFF);
        context.fill(containerX, containerY, containerX + 2, containerY + containerHeight, 0xFFFFFFFF);

        // Bottom border (darker - 3D effect)
        context.fill(containerX, containerY + containerHeight - 2, containerX + containerWidth, containerY + containerHeight, 0xFF555555);
        context.fill(containerX + containerWidth - 2, containerY, containerX + containerWidth, containerY + containerHeight, 0xFF555555);

        // Slot area background (darker inset)
        int slotsX = containerX + BORDER_LEFT;
        int slotsY = containerY + BORDER_TOP;
        int slotsWidth = columns * SLOT_SIZE + 2;
        int slotsHeight = rows * SLOT_SIZE + 2;

        // Inset for slot area
        context.fill(slotsX, slotsY, slotsX + slotsWidth, slotsY + slotsHeight, 0xFF373737);
        context.fill(slotsX + 1, slotsY + 1, slotsX + slotsWidth - 1, slotsY + slotsHeight - 1, 0xFF8B8B8B);
    }

    private void onSlotClicked(BundleSlotWidget slot) {
        // Due to FILO, to get item at index N, we need to extract N+1 items
        // Items are displayed in extraction order (index 0 = next to extract)
        extractSpecificItem(slot.getIndex());
    }

    private void extractSpecificItem(int targetIndex) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        final int slot = bundleSlotId;
        final int sync = syncId;
        // Extract targetIndex + 1 items to get the one at targetIndex
        final int extractCount = targetIndex + 1;

        client.execute(() -> {
            client.setScreen(parentScreen);
            extractItemsAndKeepLast(client, player, slot, sync, extractCount, new ArrayList<>());
        });
    }

    private void extractItemsAndKeepLast(MinecraftClient client, ClientPlayerEntity player,
            int slot, int sync, int remaining, List<Integer> extractedSlots) {
        if (remaining <= 0) {
            // Put back all but the last extracted item
            if (extractedSlots.size() > 1) {
                putBackItems(client, player, slot, sync, extractedSlots, extractedSlots.size() - 1);
            } else {
                // Reopen browser
                reopenBrowser(client, slot);
            }
            return;
        }

        client.execute(() -> {
            // Right-click to extract from bundle
            client.interactionManager.clickSlot(sync, slot, 1, SlotActionType.PICKUP, player);

            client.execute(() -> {
                // Find empty slot and place the extracted item
                for (int i = 9; i <= 44; i++) {
                    if (i != slot && screenHandler.getSlot(i).getStack().isEmpty()) {
                        client.interactionManager.clickSlot(sync, i, 0, SlotActionType.PICKUP, player);
                        extractedSlots.add(i);
                        break;
                    }
                }

                // Continue extracting
                extractItemsAndKeepLast(client, player, slot, sync, remaining - 1, extractedSlots);
            });
        });
    }

    private void putBackItems(MinecraftClient client, ClientPlayerEntity player,
            int bundleSlot, int sync, List<Integer> extractedSlots, int countToPutBack) {
        if (countToPutBack <= 0) {
            reopenBrowser(client, bundleSlot);
            return;
        }

        // Put back items in order (first extracted goes back first)
        int slotIndex = extractedSlots.size() - countToPutBack - 1;
        int slotToPutBack = extractedSlots.get(slotIndex);

        client.execute(() -> {
            // Pick up item
            client.interactionManager.clickSlot(sync, slotToPutBack, 0, SlotActionType.PICKUP, player);

            client.execute(() -> {
                // Put into bundle (left-click on bundle with item on cursor)
                client.interactionManager.clickSlot(sync, bundleSlot, 0, SlotActionType.PICKUP, player);

                client.execute(() -> {
                    putBackItems(client, player, bundleSlot, sync, extractedSlots, countToPutBack - 1);
                });
            });
        });
    }

    private void reopenBrowser(MinecraftClient client, int slot) {
        client.execute(() -> {
            ItemStack bundle = screenHandler.getSlot(slot).getStack();
            if (BundleHelper.isBundle(bundle) && !BundleHelper.isEmpty(bundle)) {
                client.setScreen(new BundleBrowserScreen(bundle, slot, parentScreen));
            }
        });
    }

    private void onEmptyButtonClicked(ButtonWidget button) {
        extractAllItems();
    }

    private void extractAllItems() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        final int slot = bundleSlotId;
        final int sync = syncId;
        final int count = contents.size();

        // Go to parent screen and extract all items there
        client.execute(() -> {
            client.setScreen(parentScreen);

            // Extract all items with slight delays
            extractNextItem(client, player, slot, sync, count);
        });
    }

    private void extractNextItem(MinecraftClient client, ClientPlayerEntity player, int slot, int sync, int remaining) {
        if (remaining <= 0) return;

        client.execute(() -> {
            // Right-click to extract
            client.interactionManager.clickSlot(sync, slot, 1, SlotActionType.PICKUP, player);

            client.execute(() -> {
                // Place cursor item in empty slot
                for (int i = 9; i <= 44; i++) {
                    if (i != slot && screenHandler.getSlot(i).getStack().isEmpty()) {
                        client.interactionManager.clickSlot(sync, i, 0, SlotActionType.PICKUP, player);
                        break;
                    }
                }

                // Continue with next item
                if (remaining > 1) {
                    extractNextItem(client, player, slot, sync, remaining - 1);
                }
            });
        });
    }

    /**
     * Extract an item from the bundle by temporarily returning to parent screen.
     */
    private void extractItem() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        // Store bundle info before closing
        final int slot = bundleSlotId;
        final int sync = syncId;

        // Temporarily switch to parent screen, do the click, then come back
        client.execute(() -> {
            // Go back to inventory
            client.setScreen(parentScreen);

            // Schedule the click for next tick when screen is active
            client.execute(() -> {
                // Right-click on bundle slot to extract
                client.interactionManager.clickSlot(sync, slot, 1, SlotActionType.PICKUP, player);

                // Then shift-click to move from cursor to inventory
                client.execute(() -> {
                    // Check if cursor has item, click empty slot to drop it
                    ItemStack cursor = screenHandler.getCursorStack();
                    if (!cursor.isEmpty()) {
                        // Find empty slot and place
                        for (int i = 9; i <= 44; i++) {
                            if (i != slot && screenHandler.getSlot(i).getStack().isEmpty()) {
                                client.interactionManager.clickSlot(sync, i, 0, SlotActionType.PICKUP, player);
                                break;
                            }
                        }
                    }

                    // Reopen bundle browser
                    client.execute(() -> {
                        ItemStack bundle = screenHandler.getSlot(slot).getStack();
                        if (BundleHelper.isBundle(bundle) && !BundleHelper.isEmpty(bundle)) {
                            client.setScreen(new BundleBrowserScreen(bundle, slot, parentScreen));
                        }
                    });
                });
            });
        });
    }

    /**
     * Refresh the UI after extraction.
     */
    private void refreshUI() {
        // Get updated bundle from the slot
        ItemStack currentBundle = screenHandler.getSlot(bundleSlotId).getStack();

        if (!BundleHelper.isBundle(currentBundle) || BundleHelper.isEmpty(currentBundle)) {
            close();
        } else {
            // Rebuild UI with updated contents
            clearChildren();
            init();
        }
    }

    @Override
    public void close() {
        // Return to parent inventory screen
        if (client != null) {
            client.setScreen(parentScreen);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Allow ESC or inventory key to close
        if (input.key() == 256 || (client != null && client.options.inventoryKey.matchesKey(input))) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
