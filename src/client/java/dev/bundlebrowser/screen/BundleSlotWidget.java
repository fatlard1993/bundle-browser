package dev.bundlebrowser.screen;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * A clickable widget that displays a single item from a bundle.
 */
public class BundleSlotWidget extends AbstractWidget {
    public static final int SLOT_SIZE = 18;

    private final ItemStack itemStack;
    private final int index;
    private final Consumer<BundleSlotWidget> onClick;

    public BundleSlotWidget(int x, int y, ItemStack itemStack, int index, Consumer<BundleSlotWidget> onClick) {
        super(x, y, SLOT_SIZE, SLOT_SIZE, Component.empty());
        this.itemStack = itemStack;
        this.index = index;
        this.onClick = onClick;
        this.active = true; // Ensure widget is active
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();

        // Draw slot background (vanilla style)
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF8B8B8B);
        context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF373737);

        // Draw the item
        if (!itemStack.isEmpty()) {
            context.item(itemStack, getX() + 1, getY() + 1);
            context.itemDecorations(client.font, itemStack, getX() + 1, getY() + 1);
        }

        // Draw hover highlight
        if (isHovered()) {
            context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x80FFFFFF);
        }
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0 && onClick != null && !itemStack.isEmpty()) {
            // Play click sound
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            this.onClick.accept(this);
        }
    }

    /**
     * Render the tooltip for this slot.
     */
    public void renderTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (isHovered() && !itemStack.isEmpty()) {
            context.setTooltipForNextFrame(Minecraft.getInstance().font, itemStack, mouseX, mouseY);
        }
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        if (!itemStack.isEmpty()) {
            builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, itemStack.getHoverName());
        }
    }
}
