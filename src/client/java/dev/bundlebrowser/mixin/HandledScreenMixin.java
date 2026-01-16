package dev.bundlebrowser.mixin;

import dev.bundlebrowser.screen.BundleBrowserScreen;
import dev.bundlebrowser.util.BundleHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept right-clicks on bundle slots and open our Bundle Browser.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // Check for right-click (button 1) with PICKUP action on a bundle
        if (button == 1 && actionType == SlotActionType.PICKUP && slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();

            // Only intercept if it's a bundle and cursor is empty (normal right-click to extract)
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && BundleHelper.isBundle(stack)) {
                ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();

                // Only open browser if cursor is empty (would normally extract) and bundle has items
                if (cursorStack.isEmpty() && !BundleHelper.isEmpty(stack)) {
                    // Cancel the vanilla extraction behavior
                    ci.cancel();

                    // Open our bundle browser screen
                    HandledScreen<?> currentScreen = (HandledScreen<?>) (Object) this;
                    client.setScreen(new BundleBrowserScreen(stack, slot.id, currentScreen));
                }
            }
        }
    }
}
