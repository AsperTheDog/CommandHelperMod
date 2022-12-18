package net.fabricmc.cmdBlockHelper.mixin;

import net.minecraft.client.gui.screen.ingame.CommandBlockScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CommandBlockScreen.class)
public interface CommandBlockScreenAccessor {

    @Invoker("setButtonsActive")
    void invokeSetButtonsActive(boolean active);
}
