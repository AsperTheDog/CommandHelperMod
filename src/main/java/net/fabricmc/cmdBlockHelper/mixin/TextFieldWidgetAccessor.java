package net.fabricmc.cmdBlockHelper.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextFieldWidget.class)
public interface TextFieldWidgetAccessor {

    @Accessor("firstCharacterIndex")
    int firstCharacterIndexAccessor();

    @Accessor("firstCharacterIndex")
    void firstCharacterIndexSetter(int firstCharacterIndex);
}
