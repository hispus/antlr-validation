import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MultiPeriodValues extends MultiValues
{
    private List<String> periods = new ArrayList<>(); //TODO: Change to DHIS2 periods

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------

    public void addPeriodValue( Object value, String period )
    {
        if ( value != null )
        {
            if ( value instanceof MultiPeriodValues )
            {
                MultiPeriodValues multiPeriodValues = (MultiPeriodValues) value;

                for ( int i = 0; i < multiPeriodValues.periods.size(); i++ )
                {
                    addPeriodValue( multiPeriodValues.getValues().get( i ), multiPeriodValues.getPeriods().get( i ) );
                }
            }
            else if ( value instanceof MultiValues )
            {
                MultiValues multiValues = (MultiValues) value;

                for ( Object val : multiValues.getValues() )
                {
                    addPeriodValue( val, period );
                }
            }
            else
            {
                addValue( value );
                periods.add( period );
            }
        }
    }

    public MultiPeriodValues last( int limit )
    {
        SortedMap<String, List<Object>> sortedValues = new TreeMap<>( Collections.reverseOrder() );

        for ( int i = 0; i < periods.size(); i++ )
        {
            if ( !sortedValues.containsKey( periods.get( i ) ) )
            {
                sortedValues.put( periods.get( i ), new ArrayList<Object>() );
            }

            sortedValues.get( periods.get( i ) ).add( getValues().get( i ) );
        }

        MultiPeriodValues lastValues = new MultiPeriodValues();

        int count = 0;

        for ( String period : sortedValues.keySet() )
        {
            for ( Object o : sortedValues.get( period ) )
            {
                if ( ++count > limit )
                {
                    return lastValues;
                }

                lastValues.addPeriodValue( o, period );
            }
        }

        return lastValues;
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    public List<String> getPeriods()
    {
        return periods;
    }
}
