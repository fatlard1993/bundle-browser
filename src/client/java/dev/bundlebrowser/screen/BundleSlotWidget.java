package dev.bundlebrowser.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A clickable widget that displays a single item from a bundle.
 */
public class BundleSlotWidget extends ClickableWidget {
    public static final int SLOT_SIZE = 18;

    private final ItemStack itemStack;
    private final int index;
    private final Consumer<BundleSlotWidget> onClick;

    public BundleSlotWidget(int x, int y, ItemStack itemStack, int index, Consumer<BundleSlotWidget> onClick) {
        super(x, y, SLOT_SIZE, SLOT_SIZE, Text.empty());
        this.itemStack = itemStack;
        this.index = index;
        this.onClick = onClick;
        this.active = true; // Ensure widget is active
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Draw slot background (vanilla style)
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF8B8B8B);
        context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF373737);

        // Draw the item
        if (!itemStack.isEmpty()) {
            context.drawItem(itemStack, getX() + 1, getY() + 1);
            context.drawStackOverlay(client.textRenderer, itemStack, getX() + 1, getY() + 1);
        }

        // Draw hover highlight
        if (isHovered()) {
            context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x80FFFFFF);
        }
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (click.button() == 0 && onClick != null && !itemStack.isEmpty()) {
            // Play click sound
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            this.onClick.accept(this);
        }
    }

    /**
     * Render the tooltip for this slot.
     */
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        if (isHovered() && !itemStack.isEmpty()) {
            context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, itemStack, mouseX, mouseY);
        }
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        if (!itemStack.isEmpty()) {
            builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, itemStack.getName());
        }
    }
}
