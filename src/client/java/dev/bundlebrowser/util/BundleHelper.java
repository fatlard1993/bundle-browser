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
     *
     * CRITICAL ASSUMPTION: iterate() returns items in FILO order (most recently added first).
     * The extraction algorithm in BundleBrowserScreen.extractSpecificItem() depends on this —
     * item at index N requires extracting N+1 items via right-click to reach it.
     * Verified against Minecraft 1.21.11. If a future version changes this ordering,
     * the extraction index calculation will break.
     */
    public static List<ItemStack> getContents(ItemStack bundle) {
        List<ItemStack> items = new ArrayList<>();
        if (!isBundle(bundle)) return items;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return items;

        for (ItemStack stack : contents.iterate()) {
            items.add(stack.copy());
        }

        return items;
    }
}
