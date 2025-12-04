package lexer

enum class TokenType(val symbols: Set<String>?  = null) {
    // --------------------
    // Keywords
    // --------------------
    IF_KEYWORD(setOf("if")),
    ELSE_KEYWORD(setOf("else")),
    EXPLORE_KEYWORD(setOf("explore")),
    DEFINE_KEYWORD(setOf("define")),
    RETURN_KEYWORD(setOf("return")),
    PRINT_KEYWORD(setOf("print")),
    CONST_KEYWORD(setOf("const")),
    SAFARI_ZONE(setOf("SafariZone")),
    TEAM(setOf("Team")),
    WHILE_KEYWORD(setOf("while")),
    FOR_KEYWORD(setOf("for")),
    IN_KEYWORD(setOf("in")),
    TO_KEYWORD(setOf("to")),
    BREAK_KEYWORD(setOf("break")),
    CONTINUE_KEYWORD(setOf("continue")),

    // --------------------
    // Literals
    // --------------------
    NULL_LITERAL(setOf("null")),
    STRING_LITERAL,
    NUMERIC_LITERAL,
    BOOLEAN_LITERAL,
    IDENTIFIER,
    VAR_KEYWORD(setOf("var")),

    // --------------------
    // Operators
    // --------------------
    PLUS(setOf("+")),
    MINUS(setOf("-")),
    MULTIPLY(setOf("*")),
    DIVIDE(setOf("/")),
    MODULO(setOf("%")),
    ASSIGN(setOf("=")),
    EQUAL_EQUAL(setOf("==")),
    NOT_EQUAL(setOf("!=")),
    LESS_THAN(setOf("<")),
    GREATER_THAN(setOf(">")),
    LESS_EQUAL(setOf("<=")),
    GREATER_EQUAL(setOf(">=")),
    NOT(setOf("!")),
    AND(setOf("&&")),
    OR(setOf("||")),

    // --------------------
    // Delimiters / Punctuation
    // --------------------
    LEFT_PAREN(setOf("(")),
    RIGHT_PAREN(setOf(")")),
    LEFT_BRACE(setOf("{")),
    RIGHT_BRACE(setOf("}")),
    LEFT_BRACKET(setOf("[")),
    RIGHT_BRACKET(setOf("]")),
    COMMA(setOf(",")),
    DOT(setOf(".")),
    SEMICOLON(setOf(";")),
    ARROW(setOf("->")),

    EOF
}

