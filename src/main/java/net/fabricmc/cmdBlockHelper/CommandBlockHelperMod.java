package net.fabricmc.cmdBlockHelper;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandBlockHelperMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("commandblockhelper");

	@Override
	public void onInitialize() {
		LOGGER.info("Command Block Helper mod initialized");
	}
}
