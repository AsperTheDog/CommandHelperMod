package net.fabricmc.cmdBlockHelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandBlockHelperMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("commandblockhelper");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Command Block Helper mod initialized");
		// After the client has initiated, we want to create the TextRenderer with the monospace font
		ClientLifecycleEvents.CLIENT_STARTED.register((client) ->
				CmdIDEScreen.monoRenderer = MonoTextRenderer.generateTextRenderer());
	}
}
