package net.fabricmc.cmdBlockHelper;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class IDETextEditor implements Element, Selectable {
    final static int lineHeight = 12;

    private enum Directions{
        UP,
        DOWN
    }
    private final TextFieldWidget background;
    private final List<IDETextFieldLine> lines;
    private final int x, y;
    private final int width, height;
    private final TextRenderer textRenderer;
    private final Consumer<String> changedListener;
    private int focusedLine;
    private int stackCount;


    public IDETextEditor(TextRenderer textRenderer, int x, int y, int width, int height, Consumer<String> changedListener)
    {
        this.lines = new ArrayList<>();

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textRenderer = textRenderer;
        this.addLine(0, "Prueba 1");
        this.addLine(1, "Prueba 2");
        this.addLine(2, "Prueba 3");
        this.addLine(3, "Prueba 4");
        this.addLine(4, "Prueba 5");

        this.background = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal(""));
        this.background.setEditable(false);
        this.background.setText("");
        this.changedListener = changedListener;
        this.focusedLine = -1;
        this.stackCount = 0;
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delt){
        background.render(matrices, mouseX, mouseY, delt);
        for (IDETextFieldLine line : lines){
            line.render(matrices, mouseX, mouseY, delt);
        }
    }

    private void addLine(int pos, String text){
        IDETextFieldLine newLine = new IDETextFieldLine(textRenderer, x + 10, y + 10 + pos * lineHeight, this.width, lineHeight, text);
        newLine.setText(text);
        newLine.setChangedListener(this.changedListener);
        newLine.setMaxLength(32500);
        newLine.setEditable(true);
        newLine.setDrawsBackground(false);
        int i = 0;
        for (IDETextFieldLine line : lines){
            if (i >= pos){
                line.setY(line.getY() + lineHeight);
            }
            i++;
        }
        lines.add(pos, newLine);
    }

    private void removeLine(int pos){
        lines.remove(pos);
        int i = 0;
        for (IDETextFieldLine line : lines){
            if (i >= pos){
                line.setY(line.getY() - lineHeight);
            }
            i++;
        }
    }

    public String getInlineCommand(){
        StringBuilder command = new StringBuilder();
        for (IDETextFieldLine line : lines){
            command.append(line.getText()).append(" ");
        }
        return command
                .toString()
                .replaceAll("\n", " ")
                .replaceAll("[ ]{2,}", " ");
    }

    @Override
    public SelectionType getType() {
        SelectionType type = SelectionType.NONE;
        for (IDETextFieldLine line : lines){
            SelectionType tmpType = line.getType();
            if (type == SelectionType.NONE) type = tmpType;
            else if (tmpType == SelectionType.FOCUSED) return tmpType;
        }
        return type;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        for (IDETextFieldLine line : lines){
            line.appendNarrations(builder);
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        for (IDETextFieldLine line : lines){
            line.mouseMoved(mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean ret = false;
        int i = 0;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.mouseClicked(mouseX, mouseY, button);
            if (lineRet){
                focusedLine = i;
            }
            ret = ret || lineRet;
            i++;
        }
        if (!ret){
            focusedLine = -1;
        }
        return ret;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.mouseReleased(mouseX, mouseY, button);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.mouseScrolled(mouseX, mouseY, amount);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) { //Intro
            this.processIntro();
            return true;
        }
        if (keyCode == 259) { //Delete
            if (this.processDelete()) return true;
        }
        if (keyCode == 265){ //Up
            this.processVerticalArrows(Directions.UP);
            return true;
        }
        if (keyCode == 264){ //Down
            this.processVerticalArrows(Directions.DOWN);
            return true;
        }
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.keyPressed(keyCode, scanCode, modifiers);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.keyReleased(keyCode, scanCode, modifiers);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.charTyped(chr, modifiers);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean changeFocus(boolean lookForwards) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.changeFocus(lookForwards);
            ret = ret || lineRet;
        }
        return ret;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        boolean ret = false;
        for (IDETextFieldLine line : lines){
            boolean lineRet = line.isMouseOver(mouseX, mouseY);
            ret = ret || lineRet;
        }
        return ret;
    }

    public void addTab(int modifiers) {
        if (focusedLine < 0 || focusedLine > this.lines.size()) return;
        this.lines.get(focusedLine).addTab(modifiers);
    }

    public void processIntro() {
        if (focusedLine < 0 || focusedLine > this.lines.size()) return;
        IDETextFieldLine line = this.lines.get(focusedLine);
        int cursor = line.getCursor();
        String text = line.getText().substring(cursor);
        line.setText(line.getText().substring(0, cursor));
        this.addLine(focusedLine + 1, text);
        line.changeFocus(false);
        focusedLine++;
        this.lines.get(focusedLine).changeFocus(true);
        this.lines.get(focusedLine).setCursor(0);
    }

    public boolean processDelete(){
        if (focusedLine < 0 || focusedLine > this.lines.size()) return true;
        IDETextFieldLine line = this.lines.get(focusedLine);
        if (line.getCursor() != 0 || focusedLine == 0) return false;
        IDETextFieldLine prevLine = this.lines.get(focusedLine - 1);
        int cursor = prevLine.getText().length();
        prevLine.setText(prevLine.getText() + line.getText());
        prevLine.setCursor(cursor);
        this.removeLine(focusedLine);
        prevLine.changeFocus(true);
        focusedLine--;
        return true;
    }

    public void processVerticalArrows(Directions dir){
        if (focusedLine < 0 || focusedLine > this.lines.size()) return;
        int offset = 0;
        if (dir == Directions.UP){
            if (focusedLine == 0) return;
            offset = -1;
        }
        else if (dir == Directions.DOWN){
            if (focusedLine == this.lines.size() - 1) return;
            offset = 1;
        }

        IDETextFieldLine line = this.lines.get(focusedLine);
        focusedLine += offset;
        IDETextFieldLine nextLine = this.lines.get(focusedLine);
        int cursor = line.getCursor();
        nextLine.setCursor(cursor);

        line.changeFocus(false);
        nextLine.changeFocus(true);
    }
}
