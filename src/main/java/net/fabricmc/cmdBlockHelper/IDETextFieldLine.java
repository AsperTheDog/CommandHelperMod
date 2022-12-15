package net.fabricmc.cmdBlockHelper;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class IDETextFieldLine extends TextFieldWidget {
    public IDETextFieldLine(TextRenderer textRenderer, int x, int y, int width, int height, String text) {
        super(textRenderer, x, y, width, height, Text.literal(text));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delt){
        super.render(matrices, mouseX, mouseY, delt);
    }

    public void addTab(int modifiers) {
        this.write("    ");
    }
}
