package dev.bundlebrowser.mixin;

import dev.bundlebrowser.BundleBrowserClient;
import dev.bundlebrowser.screen.BundleBrowserScreen;
import dev.bundlebrowser.util.BundleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept right-clicks on bundle slots and open our Bundle Browser.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        // Check for right-click (button 1) with PICKUP action on a valid bundle slot
        if (button == 1 && actionType == ContainerInput.PICKUP && slot != null && slot.hasItem() && slot.index >= 0) {
            // Skip creative inventory — its synthetic screen handler doesn't support extraction
            if (((Object) this) instanceof CreativeModeInventoryScreen) return;

            ItemStack stack = slot.getItem();

            // Only intercept if it's a bundle and cursor is empty (normal right-click to extract)
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && BundleHelper.isBundle(stack)) {
                ItemStack cursorStack = client.player.containerMenu.getCarried();

                // Only open browser if cursor is empty (would normally extract) and bundle has items
                if (cursorStack.isEmpty() && !BundleHelper.isEmpty(stack)) {
                    // Cancel the vanilla extraction behavior
                    ci.cancel();

                    BundleBrowserClient.LOGGER.debug("Opening bundle browser for slot {}", slot.index);

                    // Open our bundle browser screen
                    AbstractContainerScreen<?> currentScreen = (AbstractContainerScreen<?>) (Object) this;
                    client.setScreen(new BundleBrowserScreen(stack, slot.index, currentScreen));
                }
            }
        }
    }
}
