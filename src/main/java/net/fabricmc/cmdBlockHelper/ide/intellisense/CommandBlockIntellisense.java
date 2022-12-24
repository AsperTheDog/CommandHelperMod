package net.fabricmc.cmdBlockHelper.ide.intellisense;

import com.mojang.brigadier.ParseResults;
import net.minecraft.command.CommandSource;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

enum ScopeType{
    BRACKET,
    BRACE
}

record StackElement(ScopeType type, CommandParserNode relatedNode) {
}

public class CommandBlockIntellisense {
    private static CommandBlockIntellisense instance;
    String rawText;
    List<List<CommandParserNode>> symbols;
    Stack<StackElement> scopeStack;
    private int currentLine, currentCursorLine, currentCursorPos, lineStart;

    private CommandBlockIntellisense(String rawText){
        symbols = new ArrayList<>();
        scopeStack = new Stack<>();
        this.rawText = rawText;
        currentLine = 0;
        currentCursorPos = 0;
        this.parseSymbols();
    }

    public static CommandBlockIntellisense getInstance(){
        return instance;
    }

    public static void resetInstance(String rawText){
        instance = new CommandBlockIntellisense(rawText);
    }
    public void refresh(boolean textChanged, String text, int cursorLine, int cursorPos){
        if (textChanged){
            this.rawText = text;
            this.parseSymbols();
        }
        this.setCursorPos(cursorLine, cursorPos);
    }

    private int addSymbol(int start, int end, int line){
        return this.addSymbol(start, end, line, false);
    }
    private int addSymbol(int start, int end, int line, boolean isString){
        if (start == end) return end;
        String symbolText = rawText.substring(start, end);
        while (line >= this.symbols.size()) symbols.add(new ArrayList<>());
        symbols.get(line).add(new CommandParserNode(symbols.get(line).size(), line, start - lineStart, isString, symbolText, this));
        return end;
    }
    private void parseSymbols() {
        this.symbols.clear();
        this.scopeStack.clear();
        boolean inString = false;
        int start = 0;
        currentLine = 0;
        lineStart = start;
        String breakingChars = ",:=";
        for (int i = 0; i < rawText.length(); i++){
            char chr = rawText.charAt(i);
            if (chr == '\n'){
                start = addSymbol(start, i, currentLine, inString) + 1;
                lineStart = start;
                currentLine++;
            }
            else if (inString){
                if (chr == '"'){
                    start = this.addSymbol(start, i + 1, currentLine);
                    inString = false;
                }
            }
            else if (chr =='"'){
                start = this.addSymbol(start, i, currentLine);
                inString = true;
            }
            else if (this.scopeStack.empty() && chr == ' '){
                start = this.addSymbol(start, i, currentLine);
            }
            else if (breakingChars.indexOf(chr) != -1){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
            }
            else if (chr == '[' || chr == '{'){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
                this.addElemToStack(chr);
            }
            else if (chr == ']' || chr == '}'){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
                this.popElemFromStack(chr);
            }
            else if (chr == '.'){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
            }
        }
        this.generateArgs();
    }

    public CommandParserNode getSymbol(int index){
        return getSymbol(index, currentLine);
    }

    public CommandParserNode getSymbol(int index, int line){
        if (line < 0 || line >= this.symbols.size()) return null;
        while (index < 0){
            if (line > 1){
                line--;
                index = this.symbols.get(line).size() + index;
            }
            else return null;
        }
        while (index >= this.symbols.get(line).size()){
            if (line < this.symbols.size() - 1){
                index = index - this.symbols.get(line).size();
                line++;
            }
            else return null;
        }
        return this.symbols.get(line).get(index);
    }

    private void addElemToStack(char chr){
       if (chr != '[' && chr != '{') return;
       ScopeType type = chr == '[' ? ScopeType.BRACKET : ScopeType.BRACE;
       this.scopeStack.push(new StackElement(type, this.getSymbol(this.symbols.get(currentLine).size() - 1)));
    }

    private void popElemFromStack(char chr){
        if (chr != ']' && chr != '}') return;
        ScopeType type = chr == ']' ? ScopeType.BRACKET : ScopeType.BRACE;
        CommandParserNode symbol = this.getSymbol(this.symbols.get(currentLine).size() - 1);
        if (scopeStack.empty()){
            symbol.setError(true);
            return;
        }
        StackElement elem = scopeStack.pop();
        if (elem.type() != type){
            scopeStack.push(elem);
            symbol.setError(true);
            return;
        }
        elem.relatedNode().setSibling(symbol);
        symbol.setSibling(elem.relatedNode());
    }

    private void generateArgs(){
        for (List<CommandParserNode> lineSymbols : symbols){
            for (CommandParserNode symbol : lineSymbols){
                symbol.generateArgsFirstPass();
            }
            for (CommandParserNode symbol : lineSymbols){
                symbol.generateArgsSecondPass();
            }
        }
    }

    public void setCurrentLine(int line){
        this.currentLine = line;
    }

    public void setCursorPos(int line, int cursorPos){
        this.currentCursorLine = line;
        this.currentCursorPos = cursorPos;
    }

    public static OrderedText highlight(ParseResults<CommandSource> parse, String original, int firstCharacterIndex){
        List<OrderedText> list = new ArrayList<>();
        CommandBlockIntellisense intelli = CommandBlockIntellisense.getInstance();
        if (intelli.currentLine < 0 || intelli.currentLine >= intelli.symbols.size())
            return OrderedText.concat(list);
        for (int i = 0; i < intelli.symbols.get(intelli.currentLine).size(); i++){
            OrderedText symbolText = intelli.getSymbol(i).getAppropriateStyle(
                    intelli.currentCursorLine,
                    intelli.currentCursorPos,
                    firstCharacterIndex,
                    firstCharacterIndex + original.length());
            if (symbolText != null) list.add(symbolText);
        }
        return OrderedText.concat(list);
    }
}
