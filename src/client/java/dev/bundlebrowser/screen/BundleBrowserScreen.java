package dev.bundlebrowser.screen;

import dev.bundlebrowser.BundleBrowserClient;
import dev.bundlebrowser.util.BundleHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Bundle Browser screen that looks and feels like opening a chest.
 * Uses vanilla container styling for a native Minecraft feel.
 */
public class BundleBrowserScreen extends Screen {
    private static final int SLOT_SIZE = BundleSlotWidget.SLOT_SIZE;
    private static final int BORDER_LEFT = 7;
    private static final int BORDER_TOP = 17;
    private static final int TITLE_Y = 6;

    private final int bundleSlotId;
    private final AbstractContainerScreen<?> parentScreen;
    private final AbstractContainerMenu screenHandler;
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

    public BundleBrowserScreen(ItemStack bundleStack, int bundleSlotId, AbstractContainerScreen<?> parentScreen) {
        super(Component.translatable("bundlebrowser.screen.title"));
        this.bundleSlotId = bundleSlotId;
        this.parentScreen = parentScreen;
        this.screenHandler = parentScreen.getMenu();
        this.syncId = screenHandler.containerId;
        this.slotWidgets = new ArrayList<>();
        this.contents = BundleHelper.getContents(bundleStack);
    }

    @Override
    protected void init() {
        super.init();
        slotWidgets.clear();
        hoveredSlot = null;

        // Refresh contents from current bundle state
        ItemStack currentBundle = screenHandler.getSlot(bundleSlotId).getItem();
        if (!BundleHelper.isBundle(currentBundle) || BundleHelper.isEmpty(currentBundle)) {
            onClose();
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
            addRenderableWidget(slot);
        }

        // Add "Empty All" button at the bottom
        int buttonWidth = 60;
        int buttonY = containerY + BORDER_TOP + rows * SLOT_SIZE + 10;
        addRenderableWidget(Button.builder(
                Component.translatable("bundlebrowser.screen.empty"),
                this::onEmptyButtonClicked
        ).bounds(
                containerX + (containerWidth - buttonWidth) / 2,
                buttonY,
                buttonWidth,
                20
        ).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent overlay (don't call renderBackground - it causes double blur)
        context.fill(0, 0, width, height, 0xC0101010);

        // Draw container background (chest-like appearance)
        drawContainerBackground(context);

        // Draw title
        context.text(
                font,
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
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Render tooltip last (on top of everything)
        if (hoveredSlot != null) {
            hoveredSlot.renderTooltip(context, mouseX, mouseY);
        }
    }

    private void drawContainerBackground(GuiGraphicsExtractor context) {
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

    /**
     * Find an empty player inventory slot in the screen handler.
     * Works across all container types by checking the slot's backing inventory.
     * Returns the screen handler slot index, or -1 if no empty slot found.
     */
    private int findEmptyPlayerSlot(LocalPlayer player) {
        for (int i = 0; i < screenHandler.slots.size(); i++) {
            Slot slot = screenHandler.getSlot(i);
            if (i != bundleSlotId && slot.container == player.getInventory() && slot.getItem().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void onSlotClicked(BundleSlotWidget slot) {
        // Due to FILO, to get item at index N, we need to extract N+1 items
        // Items are displayed in extraction order (index 0 = next to extract)
        extractSpecificItem(slot.getIndex());
    }

    private void extractSpecificItem(int targetIndex) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        // Extract targetIndex + 1 items to get the one at targetIndex
        final int extractCount = targetIndex + 1;

        client.execute(() -> {
            client.setScreen(parentScreen);
            extractItemsAndKeepLast(client, player, extractCount, new ArrayList<>());
        });
    }

    private void extractItemsAndKeepLast(Minecraft client, LocalPlayer player,
            int remaining, List<Integer> extractedSlots) {
        if (remaining <= 0) {
            // Put back all but the last extracted item
            if (extractedSlots.size() > 1) {
                putBackItems(client, player, extractedSlots, extractedSlots.size() - 1);
            } else {
                reopenBrowser(client);
            }
            return;
        }

        if (client.player == null || client.gameMode == null) return;

        client.execute(() -> {
            // Right-click to extract from bundle
            client.gameMode.handleContainerInput(syncId, bundleSlotId, 1, ContainerInput.PICKUP, player);

            client.execute(() -> {
                int emptySlot = findEmptyPlayerSlot(player);
                if (emptySlot == -1) {
                    // No room — put cursor item back in bundle and abort
                    BundleBrowserClient.LOGGER.debug("No empty inventory slot for extraction, aborting");
                    client.gameMode.handleContainerInput(syncId, bundleSlotId, 0, ContainerInput.PICKUP, player);
                    client.execute(() -> abortExtraction(client, player, extractedSlots));
                    return;
                }

                // Place extracted item in the empty slot
                client.gameMode.handleContainerInput(syncId, emptySlot, 0, ContainerInput.PICKUP, player);
                extractedSlots.add(emptySlot);

                // Continue extracting
                extractItemsAndKeepLast(client, player, remaining - 1, extractedSlots);
            });
        });
    }

    /**
     * Abort an in-progress extraction by putting all extracted items back into the bundle.
     */
    private void abortExtraction(Minecraft client, LocalPlayer player,
            List<Integer> extractedSlots) {
        if (extractedSlots.isEmpty()) {
            reopenBrowser(client);
            return;
        }

        if (client.player == null || client.gameMode == null) return;

        int slotToReturn = extractedSlots.remove(extractedSlots.size() - 1);

        client.execute(() -> {
            // Pick up item from where we stashed it
            client.gameMode.handleContainerInput(syncId, slotToReturn, 0, ContainerInput.PICKUP, player);

            client.execute(() -> {
                // Put it back into the bundle
                client.gameMode.handleContainerInput(syncId, bundleSlotId, 0, ContainerInput.PICKUP, player);

                client.execute(() -> abortExtraction(client, player, extractedSlots));
            });
        });
    }

    private void putBackItems(Minecraft client, LocalPlayer player,
            List<Integer> extractedSlots, int countToPutBack) {
        if (countToPutBack <= 0) {
            reopenBrowser(client);
            return;
        }

        if (client.player == null || client.gameMode == null) return;

        // Put back items in order (first extracted goes back first)
        int slotIndex = extractedSlots.size() - countToPutBack - 1;
        int slotToPutBack = extractedSlots.get(slotIndex);

        client.execute(() -> {
            // Pick up item
            client.gameMode.handleContainerInput(syncId, slotToPutBack, 0, ContainerInput.PICKUP, player);

            client.execute(() -> {
                // Put into bundle (left-click on bundle with item on cursor)
                client.gameMode.handleContainerInput(syncId, bundleSlotId, 0, ContainerInput.PICKUP, player);

                client.execute(() -> {
                    putBackItems(client, player, extractedSlots, countToPutBack - 1);
                });
            });
        });
    }

    private void reopenBrowser(Minecraft client) {
        client.execute(() -> {
            ItemStack bundle = screenHandler.getSlot(bundleSlotId).getItem();
            if (BundleHelper.isBundle(bundle) && !BundleHelper.isEmpty(bundle)) {
                client.setScreen(new BundleBrowserScreen(bundle, bundleSlotId, parentScreen));
            }
        });
    }

    private void onEmptyButtonClicked(Button button) {
        extractAllItems();
    }

    private void extractAllItems() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        final int count = contents.size();

        // Go to parent screen and extract all items there
        client.execute(() -> {
            client.setScreen(parentScreen);

            // Extract all items with slight delays
            extractNextItem(client, player, count);
        });
    }

    private void extractNextItem(Minecraft client, LocalPlayer player, int remaining) {
        if (remaining <= 0) return;
        if (client.player == null || client.gameMode == null) return;

        client.execute(() -> {
            // Right-click to extract
            client.gameMode.handleContainerInput(syncId, bundleSlotId, 1, ContainerInput.PICKUP, player);

            client.execute(() -> {
                int emptySlot = findEmptyPlayerSlot(player);
                if (emptySlot == -1) {
                    // No room — put cursor item back in bundle and stop
                    BundleBrowserClient.LOGGER.debug("Inventory full during Empty All, stopping");
                    client.gameMode.handleContainerInput(syncId, bundleSlotId, 0, ContainerInput.PICKUP, player);
                    return;
                }

                // Place cursor item in empty slot
                client.gameMode.handleContainerInput(syncId, emptySlot, 0, ContainerInput.PICKUP, player);

                // Continue with next item
                if (remaining > 1) {
                    extractNextItem(client, player, remaining - 1);
                }
            });
        });
    }

    @Override
    public void onClose() {
        // Return to parent inventory screen
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // Allow ESC or inventory key to close
        if (input.key() == GLFW.GLFW_KEY_ESCAPE || (minecraft != null && minecraft.options.keyInventory.matches(input))) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }
}
