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

    //**********************************************************
    //************************** CORE **************************
    //**********************************************************

    private CommandBlockIntellisense(String rawText){
        symbols = new ArrayList<>();
        scopeStack = new Stack<>();
        this.rawText = rawText;
        currentLine = 0;
        currentCursorPos = 0;
        this.parseSymbols();
    }

    // This is a singleton because I have to be able to use the stuff inside of the class in the highlight function
    // which has to be static
    public static CommandBlockIntellisense getInstance(){
        return instance;
    }

    public static void resetInstance(String rawText){
        instance = new CommandBlockIntellisense(rawText);
    }

    public void refresh(boolean textChanged, String text, int cursorLine, int cursorPos){
        // If the refresh includes a change in text, we recreate the symbols
        if (textChanged){
            this.rawText = text;
            this.parseSymbols();
        }
        // In all cases we update the cursor
        this.setCursorPos(cursorLine, cursorPos);
    }

    private void parseSymbols() {
        // We reset all variables
        this.symbols.clear();
        this.scopeStack.clear();
        boolean inString = false;
        int start = 0;
        currentLine = 0;
        lineStart = start;
        // String of chars that will create single char symbols
        String breakingChars = ",:=.";
        for (int i = 0; i < rawText.length(); i++){
            char chr = rawText.charAt(i);
            // Every newline will create a symbol so that symbols are enclosed to their lines only
            if (chr == '\n'){
                // Strings can happen between multiple lines, we want the symbols to be aware of that
                start = addSymbol(start, i, currentLine, inString, true) + 1;
                lineStart = start;
                currentLine++;
            }
            // If we are inside of a string we want to ignore everything but double quotes
            else if (inString){
                if (chr == '"'){
                    start = this.addSymbol(start, i + 1, currentLine);
                    inString = false;
                }
            }
            // If we are not inside a string a double quote will start one
            else if (chr =='"'){
                start = this.addSymbol(start, i, currentLine);
                inString = true;
            }
            // If we are in the outermost scope every space means a new argument
            else if (this.scopeStack.empty() && chr == ' '){
                start = this.addSymbol(start, i, currentLine);
            }
            // Breaking characters will end the current symbol and create a new one with one character only
            else if (breakingChars.indexOf(chr) != -1){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
            }
            // Scope opening will also process the stack
            else if (chr == '[' || chr == '{'){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
                this.addElemToStack(chr);
            }
            // Scope closing will also process the stack
            else if (chr == ']' || chr == '}'){
                start = this.addSymbol(start, i, currentLine);
                start = this.addSymbol(start, i + 1, currentLine);
                this.popElemFromStack(chr);
            }
        }
        // When we finish we create the symbol arguments
        this.generateArgs();
    }

    //***********************************************************
    //********************* SYMBOL HANDLING *********************
    //***********************************************************

    private int addSymbol(int start, int end, int line){
        return this.addSymbol(start, end, line, false, false);
    }

    private int addSymbol(int start, int end, int line, boolean isString, boolean sentByIntro){
        // We don't want to add empty symbols
        if (start == end) return end;
        // We prune the symbol content from the command
        String symbolText = rawText.substring(start, end);
        // We expand the list of lines until we get the line we're in
        while (line >= this.symbols.size()) symbols.add(new ArrayList<>());
        if (symbolText.strip().equals("")){
            if (sentByIntro) {
                if (!symbols.get(line).isEmpty()) return end;
            }
            else return start;
        }
        // Add the new symbol
        symbols.get(line).add(new CommandParserNode(symbols.get(line).size(), line, start - lineStart, isString, symbolText, this));
        return end;
    }

    private void addElemToStack(char chr){
       // Should never happen, but just in case
       if (chr != '[' && chr != '{') return;
       // We get the type of scope, this is important to detect errors in the command syntax
       ScopeType type = chr == '[' ? ScopeType.BRACKET : ScopeType.BRACE;
       // We add the element to the stack
       this.scopeStack.push(new StackElement(type, this.getSymbol(this.symbols.get(currentLine).size() - 1)));
    }

    private void popElemFromStack(char chr){
        // Should never happen, but just in case
        if (chr != ']' && chr != '}') return;
        // We get the type of scope, this is important to detect errors in the command syntax
        ScopeType type = chr == ']' ? ScopeType.BRACKET : ScopeType.BRACE;
        // We get the last symbol of the stack
        CommandParserNode symbol = this.getSymbol(this.symbols.get(currentLine).size() - 1);
        // If the stack is empty, it means there was a syntax error, so we set the scope symbol as error
        if (scopeStack.empty()){
            symbol.setError(true);
            return;
        }
        StackElement elem = scopeStack.pop();
        // If types do not match, there was a syntax error in the command, we set the symbols as error
        if (elem.type() != type){
            scopeStack.push(elem);
            elem.relatedNode().setError(true);
            symbol.setError(true);
            return;
        }
        // If everything went alright, we set the nodes as sibling scopes
        elem.relatedNode().setSibling(symbol);
        symbol.setSibling(elem.relatedNode());
    }

    private void generateArgs(){
        // First pass will set independent arguments
        for (List<CommandParserNode> lineSymbols : symbols){
            for (CommandParserNode symbol : lineSymbols){
                symbol.generateArgsFirstPass();
            }
        }
        // Second pass will set arguments that depend on the first passes
        for (List<CommandParserNode> lineSymbols : symbols){
            for (CommandParserNode symbol : lineSymbols){
                symbol.generateArgsSecondPass();
            }
        }
    }

    //***********************************************************
    //************************* GETTERS *************************
    //***********************************************************

    public CommandParserNode getSymbol(int index){
        return getSymbol(index, currentLine);
    }

    public CommandParserNode getSymbol(int index, int line){
        if (line < 0 || line >= this.symbols.size()) return null;
        // Negative indexes will lookup the previous lines
        while (index < 0){
            // We only look up a previous line if it exists
            if (line > 1){
                line--;
                index = this.symbols.get(line).size() + index;
            }
            else return null;
        }
        // Indexes bigger than the max size of the line will look up next lines
        while (index >= this.symbols.get(line).size()){
            // But only if said line exists
            if (line < this.symbols.size() - 1){
                index = index - this.symbols.get(line).size();
                line++;
            }
            else return null;
        }
        return this.symbols.get(line).get(index);
    }

    //***********************************************************
    //************************* SETTERS *************************
    //***********************************************************

    public void setCurrentLine(int line){
        this.currentLine = line;
    }

    public void setCursorPos(int line, int cursorPos){
        this.currentCursorLine = line;
        this.currentCursorPos = cursorPos;
    }

    //***********************************************************
    //********************** PROVIDED FUNC **********************
    //***********************************************************

    public static OrderedText highlight(ParseResults<CommandSource> parse, String original, int firstCharacterIndex){
        List<OrderedText> list = new ArrayList<>();
        CommandBlockIntellisense intelli = CommandBlockIntellisense.getInstance();
        // Error control to avoid exceptions
        if (intelli.currentLine < 0 || intelli.currentLine >= intelli.symbols.size())
            return OrderedText.concat(list);
        // Obtain the relevant styled symbol and add it to the list, skipping empty ones
        for (int i = 0; i < intelli.symbols.get(intelli.currentLine).size(); i++){
            OrderedText symbolText = intelli.getSymbol(i).getAppropriateStyle(
                    intelli.currentCursorLine,  // Needed to know if scopes have to be made italic
                    intelli.currentCursorPos,   // Same as above
                    firstCharacterIndex,        // This will prune letters that are to the left of the target string
                    firstCharacterIndex + original.length()); // This will make sure only the necessary text is displayed
            if (symbolText != null) list.add(symbolText);
        }
        return OrderedText.concat(list);
    }
}
