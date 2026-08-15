package dev.bundlebrowser.mixin;

import dev.bundlebrowser.BundleBrowserClient;
import dev.bundlebrowser.screen.BundleBrowserScreen;
import dev.bundlebrowser.util.BundleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept right-clicks on bundle slots and open our Bundle Browser.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Unique
    private static boolean bundlebrowser$failureLogged = false;

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        // Any failure here must decline and leave vanilla handling intact, never crash the client
        try {
            bundlebrowser$tryOpenBrowser(slot, button, actionType, ci);
        } catch (Exception e) {
            if (!bundlebrowser$failureLogged) {
                bundlebrowser$failureLogged = true;
                BundleBrowserClient.LOGGER.error("Bundle browser failed to open, declining", e);
            }
        }
    }

    @Unique
    private void bundlebrowser$tryOpenBrowser(Slot slot, int button, ContainerInput actionType, CallbackInfo ci) {
        // button 1 = right-click
        if (button == 1 && actionType == ContainerInput.PICKUP && slot != null && slot.hasItem() && slot.index >= 0) {
            // Creative screen menus are client-side fakes (ItemPickerMenu, containerId 0):
            // slot indices and clicks don't map to a server container, so never activate there
            if (((Object) this) instanceof CreativeModeInventoryScreen) return;

            AbstractContainerScreen<?> currentScreen = (AbstractContainerScreen<?>) (Object) this;
            ItemStack stack = slot.getItem();

            Minecraft client = Minecraft.getInstance();
            if (client.player != null && BundleHelper.isBundle(stack)) {
                // Extraction clicks only mean anything against the player's live menu,
                // and only if this slot really is that menu's slot at this index
                AbstractContainerMenu menu = currentScreen.getMenu();
                if (client.player.containerMenu != menu) return;
                if (slot.index >= menu.slots.size() || menu.getSlot(slot.index) != slot) return;

                ItemStack cursorStack = client.player.containerMenu.getCarried();

                // Only open browser if cursor is empty (would normally extract) and bundle has items
                // Empty cursor is the gesture that would vanilla-extract; that's the one we take over
                if (cursorStack.isEmpty() && !BundleHelper.isEmpty(stack)) {
                    ci.cancel();

                    BundleBrowserClient.LOGGER.debug("Opening bundle browser for slot {}", slot.index);

                    client.setScreenAndShow(new BundleBrowserScreen(stack, slot.index, currentScreen));
                }
            }
        }
    }
}
