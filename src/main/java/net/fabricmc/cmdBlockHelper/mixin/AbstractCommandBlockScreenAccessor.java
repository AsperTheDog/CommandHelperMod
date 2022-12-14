package net.fabricmc.cmdBlockHelper.mixin;

import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCommandBlockScreen.class)
public interface AbstractCommandBlockScreenAccessor {

    @Accessor("consoleCommandTextField")
    TextFieldWidget getConsoleCommandTextField();

}
