package dev.bundlebrowser.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for working with bundle items and their contents.
 */
public class BundleHelper {

    /**
     * Check if an ItemStack is a bundle item.
     */
    public static boolean isBundle(ItemStack stack) {
        return stack.getItem() instanceof BundleItem;
    }

    /**
     * Check if a bundle is empty.
     */
    public static boolean isEmpty(ItemStack bundle) {
        if (!isBundle(bundle)) return true;
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        return contents == null || contents.isEmpty();
    }

    /**
     * Get the contents of a bundle as a list of ItemStacks.
     * Items are returned in extraction order (first item = next to be extracted via right-click).
     */
    public static List<ItemStack> getContents(ItemStack bundle) {
        List<ItemStack> items = new ArrayList<>();
        if (!isBundle(bundle)) return items;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return items;

        for (ItemStack stack : contents.iterate()) {
            items.add(stack.copy());
        }

        // iterate() already returns in FILO order (most recent first)
        // so no reversal needed
        return items;
    }

    /**
     * Get the number of items in a bundle.
     */
    public static int getItemCount(ItemStack bundle) {
        if (!isBundle(bundle)) return 0;
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        return contents == null ? 0 : contents.size();
    }

    /**
     * Get the current occupancy of a bundle (0.0 to 1.0).
     * Calculates based on total item count vs bundle capacity.
     */
    public static float getOccupancy(ItemStack bundle) {
        if (!isBundle(bundle)) return 0f;
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return 0f;

        // Calculate occupancy manually: items take up weight based on max stack size
        // A bundle has 64 "weight" capacity (same as 64 single-stack items)
        float totalWeight = 0;
        for (ItemStack stack : contents.iterate()) {
            // Each item's weight = count / maxCount (e.g., 64 dirt = 1.0 weight, 1 sword = 1.0 weight)
            totalWeight += (float) stack.getCount() / stack.getMaxCount();
        }
        return Math.min(1.0f, totalWeight / 64.0f);
    }

    /**
     * Calculate grid dimensions for displaying items.
     * Returns [columns, rows].
     */
    public static int[] calculateGridDimensions(int itemCount) {
        if (itemCount <= 0) return new int[]{1, 1};
        if (itemCount <= 4) return new int[]{itemCount, 1};
        if (itemCount <= 8) return new int[]{4, 2};
        if (itemCount <= 12) return new int[]{4, 3};
        if (itemCount <= 16) return new int[]{4, 4};
        if (itemCount <= 24) return new int[]{6, 4};
        if (itemCount <= 36) return new int[]{6, 6};
        if (itemCount <= 48) return new int[]{8, 6};
        // Max bundle capacity is 64 slots worth
        return new int[]{8, 8};
    }
}
