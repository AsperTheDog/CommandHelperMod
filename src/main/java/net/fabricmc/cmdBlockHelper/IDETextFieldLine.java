package net.fabricmc.cmdBlockHelper;

import net.fabricmc.cmdBlockHelper.mixin.TextFieldWidgetAccessor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;


public class IDETextFieldLine extends TextFieldWidget {
    public static int tabLength = 2;

    private final TextFieldWidget lineCounter;
    private String prefix;
    private int lineNum;
    private int maxLines;
    private final int initX;
    private final int initW;

    public IDETextFieldLine(TextRenderer textRenderer, int x, int y, int width, int height, String text) {
        super(textRenderer, x, y, width, height, Text.literal(text));
        lineCounter = new TextFieldWidget(textRenderer, x, y, 20, height, Text.literal(text + "_lineCount"));
        lineNum = 1;
        prefix = lineNum + " |";
        lineCounter.setEditable(false);
        lineCounter.setDrawsBackground(false);
        maxLines = 1;
        initX = x;
        initW = width;
        this.updatePrefix();
    }

    //**********************************************************
    //************************** CORE **************************
    //**********************************************************

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta){
        lineCounter.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    //***********************************************************
    //********************** TEXT HANDLING **********************
    //***********************************************************

    public void addTab() {
        this.write(new String(new char[tabLength]).replace('\0', ' '));
    }

    public void updatePrefix(){
        // We get the line num and pad spaces till we have enough
        prefix = lineNum + " ".repeat(Math.max(0, Integer.toString(maxLines).length() - Integer.toString(lineNum).length())) + " -";
        lineCounter.setText(prefix);
        // Update the width of the prefix, then shrink and move the main line so everything fits together
        int prefixWidth = 8 * prefix.length();
        lineCounter.setWidth(prefixWidth);
        super.setWidth(this.initW - prefixWidth);
        super.setX(this.initX + prefixWidth);
        // the text will truncate sometimes if we don't reset the cursor
        lineCounter.setCursorToStart();
    }

    //***********************************************************
    //************************* GETTERS *************************
    //***********************************************************

    public int getCursor(){
        return super.getCursor();
    }

    public int getFirstCharacterIndex(){
        // Small wrapper for the mixing Accessor function
        return ((TextFieldWidgetAccessor)this).firstCharacterIndexAccessor();
    }

    //***********************************************************
    //************************* SETTERS *************************
    //***********************************************************

    public void setY(int y){
        lineCounter.setY(y);
        super.setY(y);
    }

    public void setFirstCharacterIndex(int firstCharacterIndex){
        // If FirstCharacterIndex is set to a value outside of the range provided, minecraft will crash
        // since it will try to create a substring with an out of bounds index
        ((TextFieldWidgetAccessor)this).firstCharacterIndexSetter(
                MathHelper.clamp(firstCharacterIndex, 0, this.getText().length())
        );
    }

    public void setFocus(boolean focus){
        // Sets the focus on or off depending on the boolean value
        // For some reason minecraft only provides a toggle function, which is... bad
        if (!focus && this.isFocused()) this.changeFocus(true);
        else if (focus && !this.isFocused()) this.changeFocus(false);
    }

    public void setText(String text, int cursorShift){
        super.setText(text);
        this.setFirstCharacterIndex(cursorShift);
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }
}
