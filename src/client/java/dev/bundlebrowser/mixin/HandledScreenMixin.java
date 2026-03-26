package dev.bundlebrowser.mixin;

import dev.bundlebrowser.BundleBrowserClient;
import dev.bundlebrowser.screen.BundleBrowserScreen;
import dev.bundlebrowser.util.BundleHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept right-clicks on bundle slots and open our Bundle Browser.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // Check for right-click (button 1) with PICKUP action on a valid bundle slot
        if (button == 1 && actionType == SlotActionType.PICKUP && slot != null && slot.hasStack() && slot.id >= 0) {
            // Skip creative inventory — its synthetic screen handler doesn't support extraction
            if (((Object) this) instanceof CreativeInventoryScreen) return;

            ItemStack stack = slot.getStack();

            // Only intercept if it's a bundle and cursor is empty (normal right-click to extract)
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && BundleHelper.isBundle(stack)) {
                ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();

                // Only open browser if cursor is empty (would normally extract) and bundle has items
                if (cursorStack.isEmpty() && !BundleHelper.isEmpty(stack)) {
                    // Cancel the vanilla extraction behavior
                    ci.cancel();

                    BundleBrowserClient.LOGGER.debug("Opening bundle browser for slot {}", slot.id);

                    // Open our bundle browser screen
                    HandledScreen<?> currentScreen = (HandledScreen<?>) (Object) this;
                    client.setScreen(new BundleBrowserScreen(stack, slot.id, currentScreen));
                }
            }
        }
    }
}
