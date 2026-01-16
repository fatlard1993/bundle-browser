package dev.bundlebrowser;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle Browser - A client-side mod that lets you open bundles like chests.
 *
 * Simply right-click a bundle in your inventory to open it in a grid view.
 * Click any item to extract it, or use "Empty All" to dump everything.
 */
public class BundleBrowserClient implements ClientModInitializer {
    public static final String MOD_ID = "bundlebrowser";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Bundle Browser initialized! Right-click any bundle in your inventory to open it.");
    }
}
