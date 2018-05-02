import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;

public class Test
{
    static final String[] TEST_EXPRESSIONS =
        {
            "+2",
            "-2",
            "7+2",
            "7-2",
            "(2^3)^2",
            "2^(3^2)",
            "2^3^2",
            "(9/3)/2",
            "9/(3/2)",
            "9/3/2",
            "(1+2)*3",
            "1+(2*3)",
            "1+2*3",
            "\"abc\"",
            "\"abc\" + \"123\"",
            "\"abc\" + 123",
            "123 + \"abc\"",
            "\"abc\\t\\008 \\\"Hello\\\"\"",
            "2 < 2",
            "2 <= 2",
            "1<=5",
            "1>=5",
            "1<5 && 1>5",
            "1<5 || 1>5",
            "!true",
            "- true",
            "true == false",
            "true < false",
            "true && false",
            "true || false",
            "true || 2",
            "1 + true",
            "#{A0000000001} + #{A0000000002} + #{A0000000003}",
            "C{PI000000000}",
            "[days]",
            "C{PI000000000} * [days]",
            "1E1",
            "1.0E-100",
            "1~",
            "1x",
            "1 * "
        };

    private static final HashMap<String, Double> VALUE_MAP = new HashMap<String, Double>()
    {{
        put( "#{A0000000001}", 1. );
        put( "#{A0000000002}", 2. );
        put( "#{A0000000003}", 3. );
    }};

    private static final HashMap<String, Double> CONSTANT_MAP = new HashMap<String, Double>()
    {{
        put( "PI000000000", 3.14159 );
        put( "E0000000000", 2.71828 );
    }};

    public static void main( String[] args )
    {
        for ( String expr : TEST_EXPRESSIONS )
        {
            test( expr );
        }
    }

    private static void test( String expr )
    {
        AntlrErrorListener errorListener = new AntlrErrorListener(); // our error listener

        CharStream input = CharStreams.fromString( expr ); // Form an ANTLR lexer input stream

        DataValidationLexer lexer = new DataValidationLexer( input ); // Create a lexer for the input
        lexer.removeErrorListeners(); // Remove default lexer error listener (prints to console)
        lexer.addErrorListener( errorListener ); // Add our own error listener so we can collect the errors

        CommonTokenStream tokens = new CommonTokenStream( lexer ); // Parse the input into a token stream

        String errors = errorListener.returnErrors(); // Collect errors if any

        if ( errors.length() != 0 )
        {
            System.out.println( expr + " => lexer error: " + errors );
            return;
        }

        DataValidationParser parser = new DataValidationParser( tokens ); // Create a parser for the token stream
        parser.removeErrorListeners(); // Remove default parser error listener (prints to console)
        parser.addErrorListener( errorListener ); // Add our own error listener so we can collect the errors

        ParseTree tree = parser.expr(); // Parse the token stream as an expression

        errors = errorListener.returnErrors(); // Collect errors if any

        if ( errors.length() != 0 )
        {
            System.out.println( expr + " => " + errors );
            return;
        }

        DataValidationVisitor prepass = new DataValidationEvalVisitor();

        DataValidationVisitor eval = new DataValidationEvalVisitor( VALUE_MAP, CONSTANT_MAP );

        try
        {
            prepass.visit( tree ); // For type checking

            Object result = eval.visit( tree );

            System.out.println( expr + " = (" + ( result == null ? "null)" : result.getClass().getSimpleName() + ") " + result.toString() ) );
        }
        catch ( RuntimeException ex )
        {
            System.out.println( expr + " => " + ex.getMessage() );
        }
    }
}
