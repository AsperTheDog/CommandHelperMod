package net.fabricmc.cmdBlockHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.cmdBlockHelper.mixin.CommandBlockScreenAccessor;
import net.fabricmc.cmdBlockHelper.mixin.CommandBlockScreenMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class CmdIDEScreen extends Screen {
    protected final AbstractCommandBlockScreen parent;
    protected final String text;

    protected ButtonWidget doneButton;
    protected ButtonWidget cancelButton;
    protected IDETextEditor editor;

    public CmdIDEScreen(AbstractCommandBlockScreen parent, String text) {
        super(Text.literal("IDE"));
        this.parent = parent;
        this.text = text;
    }
    protected void init() {
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) ->
                this.commitAndClose()
        ).dimensions(this.width - 80, this.height - 60, 70, 20).build());
        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) ->
                this.close()
        ).dimensions(this.width - 80, this.height - 30, 70, 20).build());


        this.editor = new IDETextEditor(this.textRenderer, 10, 10, this.width - 100, this.height - 20, this::onCommandChanged);
        this.addSelectableChild(this.editor);
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
    }

    public void close() {
        this.client.setScreen(this.parent);
        ((CommandBlockScreenAccessor)this.parent).getConsoleCommandTextField().setText(editor.getInlineCommand());
    }

    protected void commitAndClose() {
        this.close();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) {
               this.editor.addTab(modifiers);
               return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != 335) {
            return false;
        } else {
            this.commitAndClose();
            return true;
        }
    }

    private void onCommandChanged(String text) {

    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.editor.render(matrices, mouseX, mouseY, delta);
    }
}
