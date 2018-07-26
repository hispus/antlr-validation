import java.util.ArrayList;
import java.util.List;

public class MultiValues
{
    private List<Object> values = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------

    public void addValue( Object value )
    {
        if ( value != null )
        {
            if ( value instanceof MultiValues )
            {
                values.addAll( ( (MultiValues) value ).values );
            }
            else
            {
                values.add( value );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    public List<Object> getValues()
    {
        return values;
    }

}
