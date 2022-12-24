package net.fabricmc.cmdBlockHelper.ide;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.cmdBlockHelper.mixin.AbstractCommandBlockScreenAccessor;
import net.fabricmc.cmdBlockHelper.mixin.CommandBlockScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.CommandBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class CmdScreen extends Screen {
    public static TextRenderer monoRenderer = null;

    protected final AbstractCommandBlockScreen parent;
    protected String text;
    protected String unformattedText;

    protected ButtonWidget doneButton;
    protected ButtonWidget cancelButton;
    protected TextEditor editor;

    public CmdScreen(AbstractCommandBlockScreen parent) {
        super(Text.literal("IDE"));
        this.parent = parent;
        this.text = "";
        this.unformattedText = "";
    }
    protected void init() {
        // Done button creation
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) ->
                this.commitAndClose()
        ).dimensions(this.width - 80, this.height - 60, 70, 20).build());
        // Cancel button creation
        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) ->
                this.cancel()
        ).dimensions(this.width - 80, this.height - 30, 70, 20).build());

        // Editor box creation
        this.editor = new TextEditor(monoRenderer, 10, 10, this.width - 100, this.height - 20);
        // Extract the current command in the command block GUI
        this.text = ((AbstractCommandBlockScreenAccessor)this.parent).getConsoleCommandTextField().getText();
        // TODO: Properly store the unformatted command as an NBT tag or something similar. Classes are recreated every time
        if (!Objects.equals(this.unformattedText, ""))
            this.editor.updateCommand(this.unformattedText, false);
        else this.editor.updateCommand(this.text, true);
        // Initial state must be saved so they can undo to a moment where they didn't touch anything
        this.editor.setInitialUndo();
        this.addSelectableChild(this.editor);
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.unformattedText = this.editor.getCommand(false);
        // Update screen size
        this.init(client, width, height);
        this.editor.resize(width - 100, height - 20);
    }

    public void cancel(){
        // This should never happen, but you never know I guess
        if (this.client == null) return;
        // Go back to the Command Block screen
        this.client.setScreen(this.parent);
        // Reactivate the buttons (why is this even necessary???)
        if(this.parent instanceof CommandBlockScreen){
            ((CommandBlockScreenAccessor)this.parent).invokeSetButtonsActive(true);
        }
        // We restore the initial text that was in the command block text field
        ((AbstractCommandBlockScreenAccessor)this.parent).getConsoleCommandTextField().setText(text);
    }

    public void close() {
        // This should never happen, but you never know I guess
        if (this.client == null) return;
        // Go back to the Command Block screen
        this.client.setScreen(this.parent);
        // Reactivate the buttons (why is this even necessary???)
        if(this.parent instanceof CommandBlockScreen){
            ((CommandBlockScreenAccessor)this.parent).invokeSetButtonsActive(true);
        }
        // We push the inline version of the command created in the Editor box
        ((AbstractCommandBlockScreenAccessor)this.parent).getConsoleCommandTextField().setText(editor.getCommand(true));
    }

    protected void commitAndClose() {
        this.close();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // This is done here because we do not want to call the parent function if it happens
        // since there is already functionality for tab and we DO NOT want that functionality to execute
        if (keyCode == 258) {
               this.editor.addTab();
               return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        // Intro will not trigger a close, since that just adds a newline in the editor.
        // Esc, on the other hand, will
        } else if (keyCode != 335) {
            return false;
        } else {
            // TODO: Should Esc really commit and close instead of cancel? Not sure, review
            this.commitAndClose();
            return true;
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.editor.render(matrices, mouseX, mouseY, delta);
    }
}
