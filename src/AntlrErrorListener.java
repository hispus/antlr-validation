import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class AntlrErrorListener extends BaseErrorListener
{
    private StringBuilder sb = new StringBuilder();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
        int line, int charPositionInLine, String msg,
        RecognitionException e)
    {
        if ( sb.length() != 0 )
        {
            sb.append( "; " );
        }

        sb.append( msg );
    }

    public String returnErrors()
    {
        String errors = sb.toString();

        sb = new StringBuilder();

        return errors;
    }
}
