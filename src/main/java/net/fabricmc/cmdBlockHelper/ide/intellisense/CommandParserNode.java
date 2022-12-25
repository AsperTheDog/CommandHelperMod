package net.fabricmc.cmdBlockHelper.ide.intellisense;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

enum SymbolTypes {
    NORMAL,
    STRING,
    EQUAL,
    LEFT_EQUAL,
    RIGHT_EQUAL,
    SEMICOLON,
    LEFT_SEMICOLON,
    RIGHT_SEMICOLON,
    COMMA,
    SCOPE,
    LIST,
    QUOTE,
    NUMBER,
    DOT
}

public class CommandParserNode {
    private SymbolTypes symbolType;
    private final String symbol;
    private final int line, positionInLine, symbolsIndex;
    private boolean hasError;
    private final CommandBlockIntellisense parent;
    private CommandParserNode sibling;
    private final boolean isString;

    //************************************************************
    //*************************** CORE ***************************
    //************************************************************

    public CommandParserNode(int symbolsIndex, int line, int positionInLine, boolean isString, String symbol, CommandBlockIntellisense parent) {
        this.symbol = symbol;
        this.parent = parent;
        this.symbolsIndex = symbolsIndex;
        this.positionInLine = positionInLine;
        this.symbolType = null;
        sibling = null;
        this.line = line;
        this.isString = isString;
    }

    //************************************************************
    //********************** STYLE HANDLING **********************
    //************************************************************

    public void generateArgsFirstPass() {
        if (this.symbolType != null) return;
        // This pass will assign the appropriate argument types to the symbols so the system knows what color to apply
        // to them. This is the first pass, so only independent arguments will be applied (that is, arguments that are not
        // relative to other arguments
        if (this.symbol.contains("\"") || this.isString) {
            this.symbolType = SymbolTypes.STRING;
        }
        else if (this.symbol.contains("=")) {
            this.symbolType = SymbolTypes.EQUAL;
        }
        else if (this.symbol.contains(",")) {
            this.symbolType = SymbolTypes.COMMA;
        }
        else if (this.symbol.contains(":")) {
            this.symbolType = SymbolTypes.SEMICOLON;
        }
        else if (this.symbol.contains("'")) {
            this.symbolType = SymbolTypes.QUOTE;
        }
        else if (this.symbol.contains("{") || this.symbol.contains("}")) {
            this.symbolType = SymbolTypes.SCOPE;
        }
        else if (this.symbol.contains("[") || this.symbol.contains("]")) {
            this.symbolType = SymbolTypes.LIST;
        }
        else if(this.symbol.contains(".")){
            this.symbolType = SymbolTypes.DOT;
        }
        else{
            // If the symbol is a numeric pattern
            // Examples are: 0, 1, 101, 12b, 12f, 0f...
            Pattern patt = Pattern.compile("-?[0-9]+[fbBsl]?");
            Matcher match = patt.matcher(this.symbol.strip());
            if (match.find()) this.symbolType = SymbolTypes.NUMBER;
        }
    }

    public void generateArgsSecondPass() {
        if (this.symbolType != null) return;
        // This pass will find the arguments that are to the left of an equal sign or a semicolon and apply a special
        // argument to it
        int i = 2;
        CommandParserNode next = this.parent.getSymbol(symbolsIndex + 1, line);
        while(next != null && next.symbol.strip().equals("")) {
            next = this.parent.getSymbol(symbolsIndex + i, line);
            i++;
        }
        if (next != null) {
            if (next.getSymbolType() == SymbolTypes.EQUAL) {
                this.symbolType = SymbolTypes.LEFT_EQUAL;
                return;
            }
            if (next.getSymbolType() == SymbolTypes.SEMICOLON) {
                this.symbolType = SymbolTypes.LEFT_SEMICOLON;
                return;
            }
        }
        // If no argument has been found at all, it's flagged as a normal symbol
        this.symbolType = SymbolTypes.NORMAL;
    }

    public boolean shouldBoldScope(int cursor, int line){
        // If the mouse is on the specific spot needed and the symbol is a scope
        return  this.line == line &&
                this.positionInLine <= cursor &&
                cursor <= this.positionInLine + this.symbol.length() &&
                (this.symbolType == SymbolTypes.LIST || this.symbolType == SymbolTypes.SCOPE);
    }

    //***********************************************************
    //************************* GETTERS *************************
    //***********************************************************

    public OrderedText getAppropriateStyle(int line, int cursorPos, int firstChar, int lastChar) {
        int symbolStart = positionInLine;
        int symbolEnd = positionInLine + this.symbol.length();

        // If the beginning of the symbol is after the last needed char of the string, we skip
        // If the last character of the symbol is before the first needed char of the string, we also skip
        if (symbolEnd <= firstChar || lastChar <= symbolStart) return null;
        // Otherwise we get the appropriate part of the symbol to print
        int minChar = Math.max(symbolStart, firstChar) - symbolStart;
        int maxChar = Math.min(symbolEnd, lastChar) - symbolStart;
        String text = this.symbol.substring(minChar, maxChar);
        // We apply the correct color depending on what argument type is attached to the symbol
        Style style = Style.EMPTY;
        switch (this.symbolType) {
            case STRING, QUOTE ->  style = style.withColor(Formatting.GREEN);
            case EQUAL, SEMICOLON ->  style = style.withColor(Formatting.YELLOW);
            case LEFT_EQUAL, LEFT_SEMICOLON ->  style = style.withColor(Formatting.GOLD);
            case RIGHT_EQUAL, RIGHT_SEMICOLON ->  style = style.withColor(Formatting.LIGHT_PURPLE);
            case COMMA ->  style = style.withColor(Formatting.GRAY);
            case SCOPE ->  style = style.withColor(Formatting.BLUE);
            case LIST ->  style = style.withColor(Formatting.AQUA);
            case NORMAL ->  style = style.withColor(Formatting.WHITE);
            case DOT -> style = style.withColor(Formatting.GOLD);
            case NUMBER -> style = style.withColor(Formatting.AQUA);
        }
        // If the error flag is activated, override the color to red
        if (this.hasError) style = style.withColor(Formatting.RED);
        // If it's a scope and the mouse is on it or on the related scope symbol, we add italics
        if (shouldBoldScope(cursorPos, line) ||
                (this.sibling != null && this.sibling.shouldBoldScope(cursorPos, line)))
            style = style.withItalic(true);
        return OrderedText.styledForwardsVisitedString(text, style);
    }

    public SymbolTypes getSymbolType() {
        return symbolType;
    }

    //***********************************************************
    //************************* SETTERS *************************
    //***********************************************************

    public void setError(boolean error) {
        this.hasError = error;
    }

    public void setSibling(CommandParserNode sibling) {
        this.sibling = sibling;
    }
}
