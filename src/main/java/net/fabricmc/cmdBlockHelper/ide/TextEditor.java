package net.fabricmc.cmdBlockHelper.ide;

import net.fabricmc.cmdBlockHelper.ide.intellisense.CommandBlockIntellisense;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

record EditorSnapshot(int cursor, int line, String text) {
}

public class TextEditor implements Element, Selectable {
    final static int lineHeight = 13;
    final static int maxUndos = 100;
    final static int scrollMult = 2;

    private enum Directions{
        UP,
        DOWN,
        RIGHT,
        LEFT
    }
    private final TextFieldWidget background;
    private final List<TextFieldLine> lines;
    private final int x, y;
    private int width;
    private int height;
    private final TextRenderer textRenderer;
    private int focusedLine;
    private int maxLines;
    private int lineShift;
    private int cursorShift;
    private StringBuilder fullText;
    private final Stack<EditorSnapshot> undos;
    private final Stack<EditorSnapshot> redos;
    private boolean wasThereUndoUpdate;
    private int lastKey;

    public TextEditor(TextRenderer textRenderer, int x, int y, int width, int height)
    {
        this.lines = new ArrayList<>();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textRenderer = textRenderer;
        this.background = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal(""));
        this.background.setEditable(false);
        this.background.setText("");
        this.focusedLine = -1;
        this.maxLines = Math.floorDiv(height - 10, lineHeight);
        this.lineShift = 0;
        this.cursorShift = 0;
        this.undos = new Stack<>();
        this.redos = new Stack<>();
        this.wasThereUndoUpdate = false;
        lastKey = -1;
        this.fullText = new StringBuilder();

        CommandBlockIntellisense.resetInstance("");
    }

    @Override
    public SelectionType getType() {
        // The highest returned type by a line will be the one returned: FOCUSED > HOVERED > NONE
        SelectionType type = SelectionType.NONE;
        for (TextFieldLine line : lines){
            SelectionType tmpType = line.getType();
            if (type == SelectionType.NONE) type = tmpType;
            else if (tmpType == SelectionType.FOCUSED) return tmpType;
        }
        return type;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        for (TextFieldLine line : lines){
            line.appendNarrations(builder);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (TextFieldLine line : lines){
            line.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean ret = false;
        int i = 0;
        for (TextFieldLine line : lines){
            boolean lineRet = line.mouseClicked(mouseX, mouseY, button);
            if (lineRet){
                // The line that actually was clicked will return true, thus we store its index
                this.setFocusedLine(i);
            }
            ret = ret || lineRet;
            i++;
        }
        // In case the user clicks below the last line, the last line will be focused
        TextFieldLine lastLine = this.lines.get(Math.min(this.lines.size(),this.maxLines + this.lineShift) - 1);
        if (mouseY > lastLine.getY() + lastLine.getHeight()){
            this.jumpTo(this.lines.size() - 1, -1);
        }
        //Always update the lines in case of
        this.updateLines(false);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean ret = false;
        for (TextFieldLine line : lines){
            boolean lineRet = line.mouseReleased(mouseX, mouseY, button);
            ret = ret || lineRet;
        }
        return ret;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean ret = false;
        for (TextFieldLine line : lines){
            boolean lineRet = line.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            ret = ret || lineRet;
        }
        return ret;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Move the lines up and down, amount is substracted because to go down the lineshift must increase
        this.updateLineShift((int) (this.lineShift - amount * scrollMult));
        this.updateLines(false);
        return true;
    }

    private boolean processKeys(int keyCode, int scanCode, int modifiers){
        if (keyCode == 90 && modifiers == 2){ // Ctrl + Z
            this.popUndo();
            return true;
        }
        if (keyCode == 89 && modifiers == 2){ // Ctrl + Y
            this.popRedo();
            return true;
        }
        if (focusedLine < 0 || focusedLine > this.lines.size() - 1) return false;
        if (keyCode == 257) { //Intro
            this.saveUndoSnapshot(false);
            this.processIntro();
            return true;
        }
        if (keyCode == 259) { //Backspace
            // Only save an undo snapshot if the last key was not Delete or Backspace
            if (lastKey != 259)
            {
                this.saveUndoSnapshot(false);
                lastKey = 259;
            }
            // This function will only return false if it removed a newline,
            // otherwise we must let the TextFieldWidget function handle it
            if (this.ProcessBackslash(false)) return true;
        }
        // Same as backspace but we set the "del" flag as true
        if (keyCode == 261){ //Delete
            if (lastKey != 259)
            {
                this.saveUndoSnapshot(false);
                lastKey = 259;
            }
            if (this.ProcessBackslash(true)) return true;
        }
        if (keyCode == 265){ //Up
            this.processVerticalArrows(Directions.UP);
            return false;
        }
        if (keyCode == 264){ //Down
            this.processVerticalArrows(Directions.DOWN);
            return false;
        }
        if (keyCode == 263){
            if (this.processHorizontalArrows(Directions.LEFT)) return true;
        }
        if (keyCode == 262){
            if (this.processHorizontalArrows(Directions.RIGHT)) return true;
        }
        return lines.get(focusedLine).keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ret = this.processKeys(keyCode, scanCode, modifiers);

        this.updateLines(ret);
        return ret;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focusedLine < 0 || focusedLine > this.lines.size() - 1) return false;
        return lines.get(focusedLine).keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focusedLine < 0 || focusedLine > this.lines.size() - 1) return false;
        // Any modification after undoing or redoing something must trigger an undo save
        if (this.wasThereUndoUpdate) {
            this.saveUndoSnapshot(false);
            this.wasThereUndoUpdate = false;
        }
        TextFieldLine line = lines.get(focusedLine);
        int initLength = line.getText().length();
        // Process the class handler
        boolean ret = line.charTyped(chr, modifiers);
        String text = line.getText();
        // If a character was inserted we must check if the user opened a scope and close it automatically if true
        if (initLength < text.length()){
            int cursor = line.getCursor();
            if (text.charAt(cursor - 1) == '[') {
                line.setText(text.substring(0, cursor) + ']' + text.substring(cursor), cursorShift);
                this.jumpTo(cursor);
            } else if (text.charAt(cursor - 1) == '{') {
                line.setText(text.substring(0, cursor) + '}' + text.substring(cursor), cursorShift);
                this.jumpTo(cursor);
            }
        }
        lastKey = chr;
        this.updateLines(true);
        return ret;
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
        if (this.focusedLine != -1){
            this.setFocusedLine(0);
            return false;
        }
        this.setFocusedLine(-1);
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    //**********************************************************
    //************************** CORE **************************
    //**********************************************************

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delt){
        background.render(matrices, mouseX, mouseY, delt);
        // We only render from the first visible line to the last
        // *lineshift* acts as a pointer to the first visible line
        // *maxLines* contains the number of lines that fit in the background box
        for (int i = this.lineShift; i < this.lines.size() && i < this.maxLines + this.lineShift; i++){
            CommandBlockIntellisense.getInstance().setCurrentLine(i);
            this.lines.get(i).render(matrices, mouseX, mouseY, delt);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.maxLines = Math.floorDiv(height, lineHeight);
        this.updateLines(false);
    }

    private OrderedText provideRenderText(String original, int firstCharacterIndex) {
        return CommandBlockIntellisense.highlight(null, original, firstCharacterIndex);
    }

    //*************************************************************
    //*********************** TEXT HANDLING ***********************
    //*************************************************************

    public void updateCommand(String text, boolean format) {
        if (format){
            text = this.autoFormat(text);
        }
        // Divide the string into a list of lines
        List<String> cmdLines = new ArrayList<>(List.of(text.split("\n", -1)));
        // Remove a possible trailing empty line. We only want to remove the last one!
        if (Objects.equals(cmdLines.get(cmdLines.size() - 1), "")){
            cmdLines.remove(cmdLines.size() - 1);
        }
        // Reset and inject the lines into the editor
        lines.clear();
        for (String line : cmdLines){
            this.addLine(line);
        }
        this.updateLines(true);
    }

    public String getCommand(boolean inline){
        StringBuilder command = new StringBuilder();
        // Combine all lines into a single string
        for (TextFieldLine line : lines){
            command.append(line.getText()).append("\n");
        }
        // inLine flag denotes if we want to transform it into a compacted 1 liner command
        // since minecraft only understands inline commands
        if (!inline) return command.toString();
        return this.makeInline(command.toString());
    }

    public void updateFullText(){
        // This function updates the string containing the whole text
        fullText = new StringBuilder();
        for (TextFieldLine line : lines){
            fullText.append(line.getText()).append("\n");
        }
    }

    public String autoFormat(String text){
        // We start with the inline version of the command
        StringBuilder str = new StringBuilder(this.makeInline(text));
        int stack = 0;
        // Add intros in between scopes and after commas
        // Brackets between quotes must be treated specially, and so are scopes before commas
        // Remove possible repeated newlines
        // Add spaces between an equal operator
        // Finally, strip starting and ending whitespace
        str = new StringBuilder(str.toString()
                .replaceAll("}", "\n}\n")
                .replaceAll("]", "\n]\n")
                .replaceAll("\\{", "\n{\n")
                .replaceAll("\\[", "\n[\n")
                .replaceAll(",", ",\n")
                .replaceAll("\n{2,}", "\n")
                .replaceAll("'\n\\[", "\n'[")
                .replaceAll("'\n\\{", "\n'{")
                .replaceAll("\"\n\\[", "\n\"[")
                .replaceAll("\"\n\\{", "\n\"{")
                .replaceAll("]\n'", "]'")
                .replaceAll("}\n'", "}'")
                .replaceAll("]\n,", "],")
                .replaceAll("}\n,", "},")
                .replaceAll("\n{2,}", "\n")
                .replaceAll("\\{\n}", "{\n\n}")
                .replaceAll("\\[\n]", "[\n\n]")
                .replaceAll("=", " = ")
                .strip());

        // Add tabs within scopes to create proper indentation
        for (int i = 0; i < str.length(); i++){
            char currentChar = str.charAt(i);
            if (currentChar == '{' || currentChar == '['){
                stack++;
            }
            else if (currentChar == '\n'){
                char nextChar = str.charAt(i + 1);
                // We want to reduce the scope before closing brackets so that the closing
                // and opening brackets are aligned
                if (nextChar == ']' || nextChar == '}'){
                    stack--;
                }
                for (int j = 0; j < stack; j++){
                    // Tab length depends on a variable
                    str.insert(i + 1, new String(new char[TextFieldLine.tabLength]).replace('\0', ' '));
                    i += lineShift;
                }
            }
        }
        return str.toString();
    }

    private int getStackValueAtLine(int lineNum){
        int stack = 0;
        int i = 0;
        // If the desired line has a closing scope symbol the value is one less
        if (0 < lines.get(focusedLine).getText().length() &&
               (lines.get(focusedLine).getText().charAt(0) == ']' ||
                lines.get(focusedLine).getText().charAt(0) == '}'))
                    stack--;
        for (TextFieldLine line : lines) {
            String text = line.getText();

            if (i == lineNum) {
                return stack;
            }
            for (int j = 0; j < text.length(); j++) {
                if (text.charAt(j) == '[' || text.charAt(j) == '{') stack++;
                if (text.charAt(j) == ']' || text.charAt(j) == '}') stack--;
            }
            i++;
        }
        return stack;
    }

    private void addLine(String text){
        this.addLine(text, -1);
    }

    private void addLine(String text, int pos){
        if (pos == -1) pos = this.lines.size();
        TextFieldLine newLine = new TextFieldLine(textRenderer, x + 3, y + 5 + pos * lineHeight, this.width - 20, lineHeight, text);
        newLine.setRenderTextProvider(this::provideRenderText);
        newLine.setMaxLength(32500);
        newLine.setEditable(true);
        newLine.setDrawsBackground(false);
        newLine.setText(text, this.lineShift);
        lines.add(pos, newLine);
        // If we are going to add a lot of lines in one go, it doesn't make sense to update with every line
        // Thus, we can set the flag to skip the lineUpdate
    }

    public void addTab() {
        if (focusedLine < 0 || focusedLine >= this.lines.size()) return;
        this.lines.get(focusedLine).addTab();
        this.updateLines(true);
    }

    private void removeLine(int pos){
        lines.remove(pos);
        this.updateLines(true);
    }

    private String makeInline(String cmd){
        // Replace any groups of whitespace into one space character
        StringBuilder formatted = new StringBuilder(cmd.strip()
                .replaceAll("\\s+", " "));
        int stack = 0;
        // Iterate through every character of the string. We do not iterate the first nor last chars
        for (int i = 1; i < formatted.length() - 1; i++){
            char curr = formatted.charAt(i);
            // Stack handling
            if (curr == '{' || curr == '[') stack++;
            else if (curr == '}' || curr == ']') stack--;

            char prev = formatted.charAt(i - 1);
            char next = formatted.charAt(i + 1);

            String specialChars = "{}[]=,";
            // Remove spaces around scope symbols, commas or equal signs
            if (curr == ' ' && (specialChars.indexOf(prev) != -1 || specialChars.indexOf(next) != -1)){
                // If we are at the outermost part of the command (arguments are separated by spaces here)
                // we want to make sure brackets are stuck to the previous argument, otherwise minecraft will not like it
                // THIS IS ONLY FOR NUMBERS. Minecraft formatting is very picky :(
                if (stack == 0 && next != '[') continue;
                formatted.deleteCharAt(i);
                i--;
            }
        }
        return formatted.toString();
    }

    //*************************************************************
    //********************** CURSOR HANDLING **********************
    //*************************************************************

    private void setFocusedLine(int newFocusedLine){
        if (newFocusedLine < 0 || newFocusedLine > this.lines.size() - 1) {
            if (this.focusedLine < 0 || this.focusedLine > this.lines.size() -1) lines.get(focusedLine).setFocus(false);
            this.focusedLine = -1;
            return;
        }
        // Early exit
        if (newFocusedLine == focusedLine) return;
        // If the focused line is above the first visible line, update the shift
        if (newFocusedLine < this.lineShift){
            this.updateLineShift(newFocusedLine);
        }
        // If the focused line is below the last visible line, update the shift
        else if (newFocusedLine >= this.lineShift + this.maxLines){
            this.updateLineShift(newFocusedLine - this.maxLines + 1);
        }
        // Change the focus so minecraft knows which is the active line
        if (!(focusedLine < 0 || focusedLine > this.lines.size() - 1))
            this.lines.get(focusedLine).setFocus(false);
        this.focusedLine = newFocusedLine;
        this.lines.get(focusedLine).setFocus(true);
    }

    // IMPORTANT: This function will reset any cursor, make sure to update cursor AFTER calling it, not before
    private void updateLines(boolean textChanged){
        int i = 0;
        int cursor = -1;
        // If there is a cursor we want to keep it, since updating text causes the lines to lose it
        if (focusedLine >= 0 && focusedLine <= this.lines.size() - 1)
            cursor  = this.lines.get(focusedLine).getCursor();
        // We update how much we have to shift horizontally so everything stays aligned
        this.calculateCursorShift();
        for (TextFieldLine line : lines){
            // Position in the visible box
            int pos = i - this.lineShift;
            // We update the new position in the box
            line.setY(y + 5 + pos * lineHeight);
            // For the line counter at the left
            line.setLineNum(i + 1);
            // So the line knows how bigh the line counter has to be
            line.setMaxLines(lines.size());
            // We also update the horizontal shift so all lines are aligned
            line.setFirstCharacterIndex(cursorShift);
            // We update the line counter prefix
            line.updatePrefix();
            i++;
        }
        // If there was a cursor, restore it
        if (cursor != -1) this.jumpTo(cursor);
        if (textChanged) this.updateFullText();
        CommandBlockIntellisense.getInstance().refresh(textChanged, fullText.toString(), focusedLine, cursor);
    }
    private void calculateCursorShift(){
        if (focusedLine < 0 || focusedLine > this.lines.size() - 1) return;
        this.cursorShift = lines.get(focusedLine).getFirstCharacterIndex();
    }

    private void updateLineShift(int lineShift){
        // The first visible line can at most be the last line and, obviously, at least the first line
        this.lineShift = Math.max(0, Math.min(this.lines.size() - 1, lineShift));
    }

    private void jumpTo(int cursor){
        this.jumpTo(focusedLine, cursor);
    }

    private void jumpTo(int line, int cursor){
        if (focusedLine != line)
            this.setFocusedLine(line);
        if (focusedLine == -1) return;
        // We can make the cursor wrap around so -1 means the end of the string, -2 is one more to the left, and so on
        if (cursor < 0)
            cursor = lines.get(focusedLine).getText().length() - (-cursor - 1);
        this.lines.get(focusedLine).setCursor(cursor);
    }

    //**********************************************************
    //************************** UNDO **************************
    //**********************************************************

    public void setInitialUndo() {
        this.saveUndoSnapshot(false);
    }

    private void saveUndoSnapshot(EditorSnapshot snap, boolean fromRedo) {
        // If the undo stack is full, we remove the oldest one
        if (this.undos.size() == maxUndos) this.undos.remove(0);
        this.undos.push(snap);
        this.wasThereUndoUpdate = true;
        // Sometimes a redo can trigger an undo, this flag prevents the redo stack from being emptied unintentionally
        if (!fromRedo)
            this.clearRedo();
    }

    private EditorSnapshot createSnapshot(){
        // We make sure the fulltext is up to date
        this.updateFullText();
        boolean isThereFocus = !(focusedLine < 0 || focusedLine > this.lines.size() - 1);
        // We create a snapshot with current parameters and save it
        return new EditorSnapshot(isThereFocus ? this.lines.get(focusedLine).getCursor() : 0, focusedLine, this.fullText.toString());
    }
    private void saveUndoSnapshot(boolean fromRedo) {
        EditorSnapshot snap = createSnapshot();
        this.saveUndoSnapshot(snap, fromRedo);
    }

    private void saveRedoSnapshot() {
        EditorSnapshot snap = createSnapshot();
        this.saveRedoSnapshot(snap);
    }

    private void saveRedoSnapshot(EditorSnapshot snap){
        // If the redo stack is full, we remove the oldest one
        if (this.redos.size() == maxUndos) this.redos.remove(0);
        this.redos.push(snap);
    }

    private void popUndo(){
        // If empty, we can't pop
        if (this.undos.empty()) return;
        EditorSnapshot snap = this.undos.pop();
        // Every undo transforms into a redo
        this.saveRedoSnapshot();
        // We paste the snapshot code and update focused line
        this.updateCommand(snap.text(), false);
        this.jumpTo(snap.line(), snap.cursor());
        this.wasThereUndoUpdate = true;
        // After a pop, any valid key should trigger an undo
        this.lastKey = -1;
        this.updateLines(true);
    }

    private void popRedo(){
        // If empty, we can't pop
        if (this.redos.empty()) return;
        this.saveUndoSnapshot(true);
        EditorSnapshot snap = this.redos.pop();
        // We paste the snapshot code and update focused line
        this.updateCommand(snap.text(), false);
        this.jumpTo(snap.line(), snap.cursor());
        // After a pop, any valid key should trigger an undo
        this.lastKey = -1;
        this.updateLines(true);
    }

    private void clearRedo(){
        // Made for abstraction in case something extra has to be done in the future
        this.redos.clear();
    }

    //***********************************************************
    //************************** INPUT **************************
    //***********************************************************

    private void insertIntro(){
        TextFieldLine line = this.lines.get(focusedLine);
        int cursor = line.getCursor();
        // Get the text from the cursor till the end of the line
        String text = line.getText().substring(cursor);
        // We set the line text to be the part we didn't get
        line.setText(line.getText().substring(0, cursor), cursorShift);
        // Create new line below the original one with the text we extracted earlier
        // We strip leading spaces because we are going to manually set the tabs later
        this.addLine(text.stripLeading(), focusedLine + 1);
        this.jumpTo(focusedLine + 1, 0);
        // We get how many tabs we have to put in the new line
        int stack = getStackValueAtLine(focusedLine);
        for (int i = 0; i < stack; i++){
            this.addTab();
        }
    }

    private void insertScopedIntros(){
        int cursor = this.lines.get(focusedLine).getCursor();
        String text = this.lines.get(focusedLine).getText();
        // Put the opening symbol in a newLine only if there is additional text in the line currently
        if (!text.substring(0, cursor - 1).trim().equals("")) {
            // Move one back to get before the opening symbol, then intro, then get back to the middle
            this.jumpTo(this.lines.get(focusedLine).getCursor() - 1);
            this.insertIntro();
            this.jumpTo(this.lines.get(focusedLine).getCursor() + 1);
        }
        // Two intros to leave an empty line inside, then add one tab to the middle line to get proper indents
        this.insertIntro();
        this.insertIntro();
        this.jumpTo(focusedLine - 1, -1);
        this.addTab();
    }

    private void processIntro() {
        TextFieldLine line = this.lines.get(focusedLine);
        int cursor = line.getCursor();
        // If at the moment of pressing intro, there are opening and closing scope symbols
        // to the left and right of the cursor respectively, that means the player is adding a newline
        // to an empty scope. In that case we want to put the scope symbols in their own lines with
        // an empty one in the middle
        if (cursor > 0 && cursor < line.getText().length()){
            char prev = line.getText().charAt(cursor - 1);
            char next = line.getText().charAt(cursor);
            if ((prev == '[' && next == ']') || (prev == '{' && next == '}')){
                this.insertScopedIntros();
                this.updateLines(true);
                return;
            }
        }
        this.insertIntro();
        this.updateLines(true);
    }

    private boolean ProcessBackslash(boolean del){
        TextFieldLine line = this.lines.get(focusedLine);
        int cursor = line.getCursor();
        // If the Delete button was the one being pressed and it's the end of the line,
        // advance to the beginning of the next line
        if (del){
            if (cursor == line.getText().length() && focusedLine < this.lines.size() - 1){
                this.jumpTo(focusedLine + 1, 0);
                line = this.lines.get(focusedLine);
                cursor = 0;
            }
            else{
                return false;
            }
        }
        // If a line is not about to be deleted, signal it so the handling is left to something else
        if (cursor > 0) {
            return false;
        }
        // We cannot delete the first line
        if (focusedLine == 0){
            return true;
        }
        // Get previous line and set the cursor to the end of the line
        TextFieldLine prevLine = this.lines.get(focusedLine - 1);
        cursor = prevLine.getText().length();
        // Add contents of the current line to the previous line
        prevLine.setText(prevLine.getText() + line.getText(), cursorShift);
        // Delete the current line and change focus to the previous one
        this.removeLine(focusedLine);
        this.jumpTo(focusedLine - 1, cursor);
        this.updateLines(true);
        return true;
    }

    private void processVerticalArrows(Directions dir){
        int offset = 0;
        // Make proper checks for each direction and set the offset accordingly
        if (dir == Directions.UP){
            if (focusedLine == 0) return;
            offset = -1;
        }
        else if (dir == Directions.DOWN){
            if (focusedLine == this.lines.size() - 1) return;
            offset = 1;
        }
        // Get cursor position and change focus
        int cursor = this.lines.get(focusedLine).getCursor();
        this.jumpTo(focusedLine + offset, cursor);
        this.updateLines(false);
    }

    private boolean processHorizontalArrows(Directions dir){
        int cursor = this.lines.get(focusedLine).getCursor();
        // If we are not managing a change of line, we leave the handling to TextFieldWidget
        if (dir == Directions.LEFT &&
                (cursor != 0 || focusedLine == 0) ||
            dir == Directions.RIGHT &&
                (cursor != this.lines.get(focusedLine).getText().length() || focusedLine == this.lines.size() - 1))
                return false;
        if (dir == Directions.LEFT) this.jumpTo(focusedLine - 1, -1);
        else if (dir == Directions.RIGHT) this.jumpTo(focusedLine + 1, 0);
        this.updateLines(false);
        return true;
    }
}