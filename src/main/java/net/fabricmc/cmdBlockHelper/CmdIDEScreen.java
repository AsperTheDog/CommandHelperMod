package net.fabricmc.cmdBlockHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class CmdIDEScreen extends Screen {
    protected final Screen parent;
    protected final String text;

    protected ButtonWidget doneButton;
    protected ButtonWidget cancelButton;
    protected TextFieldWidget commandTextField;
    ChatInputSuggestor commandSuggestor;

    public CmdIDEScreen(Screen parent, String text) {
        super(Text.literal("IDE"));
        this.parent = parent;
        this.text = text;
    }
    protected void init() {
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
            this.commitAndClose();
        }).dimensions(this.width - 80, this.height - 60, 70, 20).build());
        this.cancelButton = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
            this.close();
        }).dimensions(this.width - 80, this.height - 30, 70, 20).build());

        this.commandTextField = new TextFieldWidget(this.textRenderer, 10, 10, this.width - 100, this.height - 20, Text.literal("Test"));
        this.commandTextField.setMaxLength(32500);
        this.commandTextField.setEditable(true);
        this.commandTextField.setText("-");
        this.addSelectableChild(this.commandTextField);
        this.commandTextField.setChangedListener(this::onCommandChanged);

        this.commandSuggestor = new ChatInputSuggestor(this.client, this, this.commandTextField, this.textRenderer, true, true, 0, 7, false, -2147483648);
        this.commandSuggestor.setWindowActive(true);
        this.commandSuggestor.refresh();
    }

    public void resize(MinecraftClient client, int width, int height) {
        String string = this.commandTextField.getText();
        this.init(client, width, height);
        this.commandTextField.setText(string);
        this.commandSuggestor.refresh();
    }

    public void close() {
        this.client.setScreen(this.parent);
    }

    protected void commitAndClose() {
        this.close();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            return false;
        } else {
            this.commitAndClose();
            return true;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.commandSuggestor.mouseScrolled(amount) || super.mouseScrolled(mouseX, mouseY, amount);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.commandSuggestor.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    private void onCommandChanged(String text) {
        this.commandSuggestor.refresh();
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.commandTextField.render(matrices, mouseX, mouseY, delta);
        this.commandSuggestor.render(matrices, mouseX, mouseY);
    }
}
