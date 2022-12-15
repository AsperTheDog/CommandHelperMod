package net.fabricmc.cmdBlockHelper.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatInputSuggestor.class)
public interface ChatInputSuggestorMixin {

    @Accessor("messages")
    List<OrderedText> getMessages();

    @Accessor("chatScreenSized")
    boolean getChatScreenSized();

    @Accessor("owner")
    Screen getOwner();

    @Accessor("x")
    int getX();

    @Accessor("width")
    int getWidth();

    @Accessor("color")
    int getColor();

    @Accessor("textRenderer")
    TextRenderer getTextRenderer();
}
