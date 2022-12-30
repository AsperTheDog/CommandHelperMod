package net.fabricmc.cmdBlockHelper.ide.intellisense;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

enum SymbolTypes {
    // Each symbol type has an associated color for the formatter
    NORMAL(Style.EMPTY.withColor(Formatting.WHITE)),
    STRING(Style.EMPTY.withColor(Formatting.GREEN)),
    EQUAL(Style.EMPTY.withColor(Formatting.YELLOW)),
    LEFT_EQUAL(Style.EMPTY.withColor(Formatting.GOLD)),
    RIGHT_EQUAL(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)),
    SEMICOLON(Style.EMPTY.withColor(Formatting.YELLOW)),
    LEFT_SEMICOLON(Style.EMPTY.withColor(Formatting.GOLD)),
    RIGHT_SEMICOLON(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)),
    COMMA(Style.EMPTY.withColor(Formatting.GRAY)),
    SCOPE(Style.EMPTY.withColor(Formatting.BLUE)),
    LIST(Style.EMPTY.withColor(Formatting.DARK_AQUA)),
    QUOTE(Style.EMPTY.withColor(Formatting.GREEN)),
    NUMBER(Style.EMPTY.withColor(Formatting.AQUA)),
    DOT(Style.EMPTY.withColor(Formatting.YELLOW)),
    SLASH(Style.EMPTY.withColor(Formatting.DARK_GRAY)),
    COORDS(Style.EMPTY.withColor(Formatting.DARK_PURPLE));

    public final Style style;
    SymbolTypes(Style style){
        this.style = style;
    }
}

public class CommandParserNode {
    private SymbolTypes symbolType;
    private final String symbol;
    private final int line, positionInLine, symbolsIndex;
    private boolean hasError;
    private final CommandBlockIntellisense parent;
    private CommandParserNode sibling;
    private boolean isString, isCoords;

    //************************************************************
    //*************************** CORE ***************************
    //************************************************************

    public CommandParserNode(int symbolsIndex, int line, int positionInLine, String symbol, CommandBlockIntellisense parent) {
        this.symbol = symbol;
        this.parent = parent;
        this.symbolsIndex = symbolsIndex;
        this.positionInLine = positionInLine;
        this.symbolType = null;
        sibling = null;
        this.line = line;
        this.isString = false;
        this.isCoords = false;
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
        else if (this.isCoords){
            this.symbolType = SymbolTypes.COORDS;
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
        else if (this.symbol.contains("/")){
            this.symbolType = SymbolTypes.SLASH;
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

        // Do the same for the elements to the right of an equal sign or a semicolon
        CommandParserNode prev = this.parent.getSymbol(symbolsIndex - 1, line);
        i = 2;
        while(prev != null && prev.symbol.strip().equals("")) {
            prev = this.parent.getSymbol(symbolsIndex - i, line);
            i++;
        }
        if (prev != null) {
            if (prev.getSymbolType() == SymbolTypes.EQUAL) {
                this.symbolType = SymbolTypes.RIGHT_EQUAL;
                return;
            }
            if (prev.getSymbolType() == SymbolTypes.SEMICOLON) {
                this.symbolType = SymbolTypes.RIGHT_SEMICOLON;
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

        // If the beginning of the symbol is after the last needed char of the string, skip
        // If the last character of the symbol is before the first needed char of the string, also skip
        if (symbolEnd <= firstChar || lastChar <= symbolStart) return null;
        // Otherwise get the appropriate part of the symbol to print
        int minChar = Math.max(symbolStart, firstChar) - symbolStart;
        int maxChar = Math.min(symbolEnd, lastChar) - symbolStart;
        String text = this.symbol.substring(minChar, maxChar);
        // Apply the correct color depending on what argument type is attached to the symbol
        // or, if the error flag is activated, set the color to red
        Style style = this.hasError ? Style.EMPTY.withColor(Formatting.RED) : this.symbolType.style;
        // If it's a scope and the mouse is on it or on the related scope symbol, add italics
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

    public void setIsString(boolean isString){
        this.isString = isString;
    }

    public void setIsCoords(boolean isCoords){
        this.isCoords = isCoords;
    }
}
