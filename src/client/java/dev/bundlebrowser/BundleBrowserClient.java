package dev.bundlebrowser;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-side only: right-click a bundle to open it in a grid view. */
public class BundleBrowserClient implements ClientModInitializer {
    public static final String MOD_ID = "bundlebrowser";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Bundle Browser initialized! Right-click any bundle in your inventory to open it.");
    }
}
