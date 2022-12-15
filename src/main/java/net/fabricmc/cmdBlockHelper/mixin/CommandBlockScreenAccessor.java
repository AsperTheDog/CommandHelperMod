package net.fabricmc.cmdBlockHelper.mixin;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/client/gui/screen/ingame/AbstractCommandBlockScreen")
public interface CommandBlockScreenAccessor {

    @Accessor("consoleCommandTextField")
    TextFieldWidget getConsoleCommandTextField();

}
