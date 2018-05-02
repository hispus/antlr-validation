grammar DataValidation;

// -----------------------------------------------------------------------------
// Parser rules
// -----------------------------------------------------------------------------

expr
    :   dimensionItemObject
    |   orgUnitCount
    |   reportingRate
    |   constant
    |   days
    |   numericLiteral
    |   stringLiteral
    |   booleanLiteral
    |   '(' expr ')'
    |   <assoc=right> expr op='^' expr
    |   '+' expr                // No operator tagged, just pass through the expression
    |   uop='-' expr
    |   uop='!' expr
    |   expr op=('*'|'/'|'%') expr
    |   expr op=('+'|'-') expr
    |   expr op=('<=' | '>=' | '<' | '>') expr
    |   expr op=('==' | '!=') expr
    |   expr op='&&' expr
    |   expr op='||' expr
    ;

numericLiteral
    :   NUMERIC_LITERAL
    ;

stringLiteral
    :   STRING_LITERAL
    ;

booleanLiteral
    :   BOOLEAN_LITERAL
    ;

dimensionItemObject
    :   dataElementOperand
    |   programDataElement
    |   programTrackedEntityAttribute
    |   programIndicator
    ;

dataElementOperand
    :   '#{' UID '.' UID '}'
    |   '#{' UID '}'
    ;

programDataElement
    :   'D{' UID '.' UID '}'
    ;

programTrackedEntityAttribute
    :   'A{' UID '.' UID '}'
    ;

programIndicator
    :   'I{' UID '}'
    ;

orgUnitCount
    :   'OUG{' UID '}'
    ;

reportingRate
    :   'R{' UID '.REPORTING_RATE}'
    ;

constant
    :   'C{' UID '}'
    ;

days
    :   '[days]'
    ;

// -----------------------------------------------------------------------------
// Assign token names to parser symbols
// -----------------------------------------------------------------------------

POWER : '^';
MINUS : '-';
PLUS :  '+';
NOT :   '!';
MUL :   '*';
DIV :   '/';
MOD :   '%';
LEQ :   '<=';
GEQ :   '>=';
LT  :   '<';
GT  :   '>';
EQ  :   '==';
NE  :   '!=';
AND :   '&&';
OR  :   '||';

// -----------------------------------------------------------------------------
// Lexer rules
// -----------------------------------------------------------------------------

NUMERIC_LITERAL
    :   '0' '.'?
    |   [1-9] [0-9]* Exponent?
    |   [1-9] [0-9]* '.' [0-9]* Exponent?
    |   '.' [0-9]+ Exponent?
    ;

STRING_LITERAL
    :   '"' (~["\\\r\n] | EscapeSequence)* '"'
    ;

BOOLEAN_LITERAL
    :   [Tt][Rr][Uu][Ee]
    |   [Ff][Aa][Ll][Ss][Ee]
    ;

UID :   Alpha
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
    ;

WS  :   [ \t\n\r]+ -> skip // toss out all whitespace
    ;

// Lexer fragments

fragment Exponent
    : ('e'|'E') ('+'|'-')? [0-9]+
    ;

fragment Alpha
    :    [a-zA-Z]
    ;

fragment AlphaNum
    :   [a-zA-Z0-9]
    ;

fragment EscapeSequence
    : '\\' [btnfr"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigit
    : [0-9a-fA-F]
    ;
