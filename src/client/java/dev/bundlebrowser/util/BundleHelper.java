package dev.bundlebrowser.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public class BundleHelper {

    public static boolean isBundle(ItemStack stack) {
        return stack.getItem() instanceof BundleItem;
    }

    public static boolean isEmpty(ItemStack bundle) {
        if (!isBundle(bundle)) return true;
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null || contents.isEmpty();
    }

    /**
     * Bundle contents in extraction order (first item = next to be extracted via right-click).
     *
     * CRITICAL ASSUMPTION: itemCopies() yields FILO order (most recently added first).
     * The extraction algorithm in BundleBrowserScreen.extractSpecificItem() depends on this:
     * item at index N requires extracting N+1 items via right-click to reach it. If a
     * Minecraft version changes this ordering, the extraction index calculation breaks.
     */
    public static List<ItemStack> getContents(ItemStack bundle) {
        List<ItemStack> items = new ArrayList<>();
        if (!isBundle(bundle)) return items;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return items;

        contents.itemCopies().forEach(items::add);

        return items;
    }
}
