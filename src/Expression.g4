grammar Expression;

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
    |   <assoc=right> expr fun='^' expr
    |   '+' expr                // No operator tagged, just pass through the expression
    |   fun='-' expr
    |   fun='!' expr
    |   expr fun=('*'|'/'|'%') expr
    |   expr fun=('+'|'-') expr
    |   expr fun=('<=' | '>=' | '<' | '>') expr
    |   expr fun=('==' | '!=') expr
    |   expr fun='&&' expr
    |   expr fun='||' expr
    |   fun='if' '(' test=expr ',' valueIfTrue=expr ',' valueIfFalse=expr ')'
    |   fun='isNull' '(' expr ')'
    |   expr '.' fun='except' '(' expr ')'
    |   expr '.' fun='period' '(' from=expr (',' to=expr (',' fromYear=expr (',' toYear=expr )? )? )? ')'
    |   expr '.' fun='ouLevel' '(' from=expr (',' to=expr )? ')'
    |   expr '.' fun='ouAncestor' '(' (from=expr (',' to=expr )? )? ')'
    |   expr '.' fun='ouDescendant' '(' (from=expr (',' to=expr )? )? ')'
    |   expr '.' fun='ouPeer' '(' (from=expr (',' to=expr )? )? ')'
    |   expr '.' fun='ouGroup' '(' arg=expr  (',' arg=expr? )* ')'
    |   expr '.' fun='last'
    |   expr '.' fun='count' ('(' 'last' last=expr ')')?
    |   expr '.' fun='sum' ('(' 'last' last=expr ')')?
    |   expr '.' fun='max' ('(' 'last' last=expr ')')?
    |   expr '.' fun='min' ('(' 'last' last=expr ')')?
    |   expr '.' fun='average' ('(' 'last' last=expr ')')?
    |   expr '.' fun='stddev' ('(' 'last' last=expr ')')?
    |   expr '.' fun='variance' ('(' 'last' last=expr ')')?
    |   expr '.' fun='median' ('(' 'last' last=expr ')')?
    |   expr '.' fun='percentile' '(' arg=expr ')' ('(' 'last' last=expr ')')?
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
    :   '#{' UID ('.' UID)? '}'
    ;

programDataElement
    :   'D{' UID ('.' UID)? '}'
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

PERIOD: 'period';
OU_LEVEL: 'ouLevel';
OU_ANCESTOR : 'ouAncestor';
OU_DESCENDANT: 'ouDescendant';
OU_PEER: 'ouPeer';
OU_GROUP: 'ouGroup';

SUM: 'sum';
AVERAGE: 'average';
LAST: 'last';
COUNT: 'count';
STDDEV: 'stddev';
VARIANCE: 'variance';
MIN: 'min';
MAX: 'max';
MEDIAN: 'median';
PERCENTILE: 'percentile';

IF: 'if';
IS_NULL: 'isNull';
EXCEPT: 'except';

// -----------------------------------------------------------------------------
// Lexer rules
// -----------------------------------------------------------------------------

NUMERIC_LITERAL
    :   '0' '.'?
    |   [1-9] [0-9]* Exponent?
    |   [1-9] [0-9]* '.' [0-9]* Exponent?
    |   '0'? '.' [0-9]+ Exponent?
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
