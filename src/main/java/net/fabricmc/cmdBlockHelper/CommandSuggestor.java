package net.fabricmc.cmdBlockHelper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.cmdBlockHelper.mixin.ChatInputSuggestorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import java.util.Iterator;

public class CommandSuggestor extends ChatInputSuggestor {
    public CommandSuggestor(MinecraftClient client, Screen owner, TextFieldWidget textField, TextRenderer textRenderer, boolean slashOptional, boolean suggestingWhenEmpty, int inWindowIndexOffset, int maxSuggestionSize, boolean chatScreenSized, int color) {
        super(client, owner, textField, textRenderer, slashOptional, suggestingWhenEmpty, inWindowIndexOffset, maxSuggestionSize, chatScreenSized, color);
    }

    public void renderMessages(MatrixStack matrices) {
        int i = 0;

        ChatInputSuggestorMixin mixin = ((ChatInputSuggestorMixin)this);
        for(Iterator<OrderedText> var3 = mixin.getMessages().iterator(); var3.hasNext(); ++i) {
            OrderedText orderedText = var3.next();
            int j = mixin.getChatScreenSized() ? mixin.getOwner().height - 14 - 13 - 12 * i : 72 + 12 * i;
            DrawableHelper.fill(matrices, mixin.getX() - 1, j, mixin.getX() + mixin.getWidth() + 1, j + 12, mixin.getColor());
            mixin.getTextRenderer().drawWithShadow(matrices, orderedText, (float)mixin.getX(), (float)(j + 2), -1);
        }
    }
}
