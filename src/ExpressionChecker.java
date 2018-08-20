import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.pow;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;

/**
 * Checks parsed experssions for data type compatibility. Also provides much
 * of the base functionality for expression evaluation.
 */
public class ExpressionChecker extends ExpressionBaseVisitor<Object>
{
    //TODO: allow both numeric and String values in valueMap
    //TODO: implicit type conversions when comparing/concatenating numeric with String
    //TODO: implement missingValueStrategy
    
    private final static Double ONE = Double.valueOf( 1. );

    private int orgUnitLevel = 3; // Reporting orgUnit level for demonstration

    private String currentPeriod = "201808"; //TODO: Replace with real DHIS2 period type.

    private String currentOrgUnit = "ABC.XYZ"; //TODO: Replace with real DHIS2 orgUnits.

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public Object visitExpr( ExpressionParser.ExprContext ctx )
    {
        if ( ctx.fun != null )
        {
            return function( ctx );
        }
        else if ( ctx.expr( 0 ) != null ) // pass through the expression
        {
            return visit( ctx.expr( 0 ) );
        }
        else // pass through the entire subtree
        {
            return visit( ctx.getChild( 0 ) );
        }
    }

    @Override
    public Object visitProgramIndicatorVariable( ExpressionParser.ProgramIndicatorVariableContext ctx )
    {
        throw new ParsingException( "Program indicator variable is not valid in this expression." );
    }

    @Override
    public Object visitProgramIndicatorFunction( ExpressionParser.ProgramIndicatorFunctionContext ctx )
    {
        throw new ParsingException( "Program indicator function is not valid in this expression." );
    }

    @Override
    public Object visitDimensionItemObject( ExpressionParser.DimensionItemObjectContext ctx )
    {
        //TODO: for data elmenets, return object type based on data element type
        //TODO: for non-typed DimensionItemObjects, return a Double constant.
        return ONE;
    }

    @Override
    public Object visitConstant( ExpressionParser.ConstantContext ctx )
    {
        return ONE;
    }

    @Override
    public Object visitOrgUnitCount( ExpressionParser.OrgUnitCountContext ctx )
    {
        return ONE ;
    }

    @Override
    public Object visitReportingRate( ExpressionParser.ReportingRateContext ctx )
    {
        return ONE;
    }

    @Override
    public Object visitDays( ExpressionParser.DaysContext ctx )
    {
        return ONE;
    }

    @Override
    public Object visitNumericLiteral( ExpressionParser.NumericLiteralContext ctx )
    {
        return Double.valueOf( ctx.getText() );
    }

    @Override
    public Object visitStringLiteral(ExpressionParser.StringLiteralContext ctx)
    {
        return unescapeJava( ctx.getText().substring( 1, ctx.getText().length() - 1 ) );
    }

    @Override
    public Object visitBooleanLiteral(ExpressionParser.BooleanLiteralContext ctx)
    {
        return Boolean.valueOf( ctx.getText() );
    }

    // -------------------------------------------------------------------------
    // Protected methods
    // -------------------------------------------------------------------------

    /**
     * Evaluates common functions and operators.
     *
     * @param ctx expression context
     * @return evaluated expression object
     */
    protected Object function( ExpressionParser.ExprContext ctx )
    {
        switch ( ctx.fun.getType() )
        {
            // -----------------------------------------------------------------
            // Arithmetic Operators (return Double)
            // -----------------------------------------------------------------

            case ExpressionParser.MINUS:
                if ( ctx.expr( 1 ) != null ) // Subtract operator
                {
                    return castDouble( visit( ctx.expr( 0 ) ) )
                        - castDouble( visit( ctx.expr( 1 ) ) );
                }
                else // Unary Negative operator
                {
                    return -castDouble( visit( ctx.expr( 0 ) ) );
                }

            case ExpressionParser.PLUS: // String concatenation or numeric addition
                Object left = visit( ctx.expr( 0 ) );
                Object right = visit( ctx.expr( 1 ) );

                if ( left.getClass() == String.class )
                {
                    return castString( left )
                        + castString( right );
                }

                return castDouble( left )
                    + castDouble( right );

            case ExpressionParser.POWER:
                return pow( castDouble( visit( ctx.expr( 0 ) ) ),
                    castDouble( visit( ctx.expr( 1 ) ) ) );

            case ExpressionParser.MUL:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    * castDouble( visit( ctx.expr( 1 ) ) );

            case ExpressionParser.DIV:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    / castDouble( visit( ctx.expr( 1 ) ) );

            case ExpressionParser.MOD:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    % castDouble( visit( ctx.expr( 1 ) ) );

            // -----------------------------------------------------------------
            // Logical Operators (return Boolean)
            // -----------------------------------------------------------------

            case ExpressionParser.NOT:
                return !castBoolean( visit( ctx.expr( 0 ) ) );

            case ExpressionParser.LEQ:
                return compare( ctx ) <= 0;

            case ExpressionParser.GEQ:
                return compare( ctx ) >= 0;

            case ExpressionParser.LT:
                return compare( ctx ) < 0;

            case ExpressionParser.GT:
                return compare( ctx ) > 0;

            case ExpressionParser.EQ:
                return compare( ctx ) == 0;

            case ExpressionParser.NE:
                return compare( ctx ) != 0;

            case ExpressionParser.AND:
                return functionAnd( ctx );

            case ExpressionParser.OR:
                return functionOr( ctx );

            // -----------------------------------------------------------------
            // Logical functions
            // -----------------------------------------------------------------

            case ExpressionParser.IF:
                return functionIf( ctx );

            case ExpressionParser.EXCEPT:
                return castBoolean( visit( ctx.a1().expr() ) )
                    ? null
                    : visit( ctx.expr( 0 ) );

            case ExpressionParser.IS_NULL:
                return visit( ctx.expr( 0 ) ) == null;

            case ExpressionParser.COALESCE:
                return functionCoalesce( ctx );

            // -----------------------------------------------------------------
            // Aggregation functions
            // -----------------------------------------------------------------

            case ExpressionParser.LAST:
                return last( ctx );

            case ExpressionParser.COUNT:
                return evalAll( ctx ).size();

            case ExpressionParser.SUM:
                return StatUtils.sum( getDoubles( ctx ) );

            case ExpressionParser.MAX:
                return StatUtils.max( getDoubles( ctx ) );

            case ExpressionParser.MIN:
                return StatUtils.min( getDoubles( ctx ) );

            case ExpressionParser.AVERAGE:
                return StatUtils.mean( getDoubles( ctx ) );

            case ExpressionParser.STDDEV:
                StandardDeviation stdDev = new StandardDeviation();
                return stdDev.evaluate( getDoubles( ctx ) );

            case ExpressionParser.VARIANCE:
                return StatUtils.variance( getDoubles( ctx ) );

            case ExpressionParser.MEDIAN:
                return StatUtils.percentile( getDoubles( ctx ), 50 );

            case ExpressionParser.PERCENTILE:
                return StatUtils.percentile( getDoubles( ctx ), castDouble( visit( ctx.a1().expr() ) ) );

            case ExpressionParser.RANK_HIGH:
                return rankHigh( getDoubles( ctx ), ctx );

            case ExpressionParser.RANK_LOW:
                return rankLow( getDoubles( ctx ), ctx );

            case ExpressionParser.RANK_PERCENTILE_HIGH:
                double[] vals = getDoubles( ctx );
                return vals.length == 0 ? 0 : (int)Math.round( 100.0 * rankHigh( vals, ctx ) / vals.length );

            case ExpressionParser.RANK_PERCENTILE_LOW:
                double[] values = getDoubles( ctx );
                return values.length == 0 ? 0 : (int)Math.round( 100.0 * rankLow( values, ctx ) / values.length );

            // -----------------------------------------------------------------
            // Aggregation scope functions
            // -----------------------------------------------------------------

            case ExpressionParser.PERIOD:
                return iteratePeriods( ctx );

            case ExpressionParser.OU_LEVEL:
                return iterateOuLevel( ctx );

            case ExpressionParser.OU_ANCESTOR:
                return ouAncestor( ctx );

            case ExpressionParser.OU_DESCENDANT:
                return iterateOuDescendant( ctx );

            case ExpressionParser.OU_PEER:
                return iterateOuPeer( ctx );

            case ExpressionParser.OU_GROUP:
                return iterateOuGroup( ctx );

            default: // (Shouldn't happen, mismatch between expression grammer and here.)
                throw new ParsingException( "fun=" + ctx.fun.getType() + " not recognized." );
        }
    }

    /**
     * Finds the logical AND of two boolean expressions.
     * <p/>
     * When checking the expression, make sure we evaluate both
     * expressions whether we need to or not. For actual expression
     * evaluation (not here) this can be optimized so the second
     * expression is only evaluated if necessary.
     *
     * @param ctx the parsing context.
     * @return the logical AND.
     */
    protected Object functionAnd( ExpressionParser.ExprContext ctx )
    {
        Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
        Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

        return leftBool && rightBool;
    }

    /**
     * Finds the logical OR of two boolean expressions.
     * <p/>
     * When checking the expression, make sure we evaluate both
     * expressions whether we need to or not. For actual expression
     * evaluation (not here) this can be optimized so the second
     * expression is only evaluated if necessary.
     *
     * @param ctx the parsing context.
     * @return the logical OR.
     */
    protected Object functionOr( ExpressionParser.ExprContext ctx )
    {
        Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
        Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

        return leftBool || rightBool;
    }

    /**
     * If the test expression is true, returns the first expression value,
     * else returns the second expression value.
     * <p/>
     * When checking the expression, make sure we evaluate both
     * expressions whether we need to or not. For actual expression
     * evaluation (not here) this can be optimized so only one expression
     * is evaluated depending on the boolean value of the test expression.
     *
     * @param ctx the parsing context.
     * @return the first or second expression value, depending on the test.
     */
    protected Object functionIf( ExpressionParser.ExprContext ctx )
    {
        Object o1 = visit( ctx.a2().expr( 0 ) );
        Object o2 = visit( ctx.a2().expr( 1 ) );

        return castBoolean( visit( ctx.expr( 0 ) ) )
            ? o1
            : o2;
    }

    /**
     * Returns the first non-null argument.
     * <p/>
     * When checking the expression, evaluate every argument. For
     * actual expression evaluation (not here) this can be optimized so that
     * arguments are evaluated only until the first non-null argument.
     *
     * @param ctx the parsing context.
     * @return the first non-null argument.
     */
    protected Object functionCoalesce( ExpressionParser.ExprContext ctx )
    {
        Object returnVal = null;

        for ( ExpressionParser.ExprContext c : ctx.a1_n().expr() )
        {
            Object val = visit( c );
            if ( returnVal == null && val != null )
            {
                returnVal = val;
            }
        }
        return returnVal;
    }

    /**
     * Returns the high rank of the argument within the set of multiple
     * values. 1 is the rank of the highest value, and n is the rank of
     * the lowest value, where n is the number of values.
     * <p/>
     * For example, if the multiple values are 60, 50, 50, 40, then the rankHigh
     * value of 60, 50, 50, or 40 would be 1, 2, 2, or 4.
     * @param values the values to rank amongst.
     * @param ctx parsing context with a single argument of the value to rank.
     * @return the rank of the argument within the multiple values.
     */
    protected Integer rankHigh( double[] values, ExpressionParser.ExprContext ctx )
    {
        double test = castDouble( visit( ctx.a1().expr() ) );

        Integer rankHigh = 0;

        for ( double d : values )
        {
            if ( d <= test )
            {
                rankHigh++;
            }
        }

        return rankHigh;
    }

    /**
     * Returns the low rank of the argument within the set of multiple
     * values. n is the rank of the highest value, and 1 is the rank of
     * the lowest value, where n is the number of values.
     * <p/>
     * For example, if the multiple values are 60, 50, 50, 40, then the rankLow
     * value of 60, 50, 50, or 40 would be 4, 3, 3, or 1.
     * @param values the values to rank amongst.
     * @param ctx parsing context with a single argument of the value to rank.
     * @return the rank of the argument within the multiple values.
     */
    protected Integer rankLow( double[] values, ExpressionParser.ExprContext ctx )
    {
        double test = castDouble( visit( ctx.a1().expr() ) );

        Integer rankHigh = 0;

        Integer rankLow = 1;

        for ( double d : values )
        {
            if ( d > test )
            {
                rankLow++;
            }
        }

        return rankLow;
    }

    /**
     * Iterates through periods according to arguments. There must be at least
     * one argument and there may be any greater number. The arguments are
     * interpreted as follows:
     * <ul>
     *     <li>1. The number of periods before (negative) or after (positive)
     *     the current period. If there is only one argument, then only a
     *     single value is returned, not multiple values</li>
     *     <li>2. If a second argument is present, the first argument is the
     *     offset (negative for past, positive for future) for the start of
     *     a range of periods, and the second argument is the offset for the
     *     end of the period range. Multiple values are returned and must be
     *     processed by an aggregtion funciton (even if the start and end
     *     offsets are the same.) </li>
     *     <li>3. If a third argument is present, it is an offset in years
     *     (negative for past, positive for future). The period(s) specified by
     *     the first two arguments are shifted this number of years into the
     *     past or the future.</li>
     *     <li>4. If a fourth argument is present, the third and forth
     *     arguments define the offsets for the start and end of a range of
     *     years in which the periods from the first two arguments are
     *     evaluated.</li>
     *     <li>5+. If more than four arguments are present, each subsequent
     *     four arguments specify an additional range of periods to evaluate
     *     in addition to the range specified by arguments 1-4.</li>
     * </ul>
     * @param ctx the parsing context.
     * @return a single value (1 argument) or multiple values (>1 argument).
     */
    protected Object iteratePeriods( ExpressionParser.ExprContext ctx )
    {
        String savedPeriod = currentPeriod;

        Object returnVal;

        if ( ctx.a1_n().expr().size() == 1 ) // Single period shift returns single value.
        {
            returnVal = periodShiftValue( ctx, savedPeriod, evalIntDefault( ctx.a1_n().expr( 0 ), 0 ), 0 );
        }
        else
        {
            MultiPeriodValues values = new MultiPeriodValues();

            for ( int i = 0; i < ctx.a1_n().expr().size() / 4 + 1; i++ )
            {
                int periodShiftFrom = evalIntDefault( ctx.a1_n().expr( i ), 0 );
                int periodShiftTo = evalIntDefault( ctx.a1_n().expr( i + 1 ), periodShiftFrom );
                int yearShiftFrom = evalIntDefault( ctx.a1_n().expr( i + 2 ), 0 );
                int yearShiftTo = evalIntDefault( ctx.a1_n().expr( i + 3 ), yearShiftFrom );

                for ( int yearShift = yearShiftFrom; yearShift <= yearShiftTo; yearShift++ )
                {
                    for ( int periodShift = periodShiftFrom; periodShift <= periodShiftTo; periodShift++ )
                    {
                        Object value = periodShiftValue( ctx, savedPeriod, periodShift, yearShift );

                        values.addPeriodValue( value, currentPeriod );
                    }
                }
            }

            returnVal = values;
        }

        currentPeriod = savedPeriod;

        return returnVal;
    }

    /**
     * Evaluates an expression within a period shifted relative to a base period.
     *
     * @param ctx the parsing context.
     * @param basePeriod the base period to shift from.
     * @param periodShift the number of periods to shift from the base.
     * @param yearShift the number of years to shift from the base.
     * @return the value of the expression shifted in time.
     */
    protected Object periodShiftValue( ExpressionParser.ExprContext ctx, String basePeriod, int periodShift, int yearShift )
    {
        //TODO: change to real code for DHIS2 periods. For this prototype, just shifts months.

        int months = Integer.parseInt( basePeriod.substring( 0, 4 ) ) * 12 + Integer.parseInt( basePeriod.substring( 4 ) ) - 1
            + periodShift + ( yearShift * 12 );

        int m = ( months % 12 ) + 1;

        currentPeriod = Integer.toString( months / 12 ) + ( m < 10 ? "0" : "" ) + Integer.toString( m );

        return visit( ctx.expr( 0 ) );
    }

    /**
     * Interates over all orgUnits in the system at a given level.
     *
     * @param ctx the parsing context, containing the orgUnit level.
     * @return the multiple values from the orgUnits.
     */
    protected Object iterateOuLevel( ExpressionParser.ExprContext ctx )
    {
        //TODO: change to real DHIS2 orgUnit logic.

        String savedOrgUnit = currentOrgUnit;

        MultiValues values = new MultiValues();

        values.addValue( orgUnitValue( ctx, "ABC.DEF" ) );
        values.addValue( orgUnitValue( ctx, "DEF.GHI" ) );

        currentOrgUnit = savedOrgUnit;

        return values;
    }

    /**
     * Returns the expression value, evaluated at the orgUnit's ancestor.
     *
     * @param ctx the parsing context, containing the ancestor level
     *            (1=parent, 2=grandparent, etc.)
     * @return the single value from the ancestor.
     */
    protected Object ouAncestor( ExpressionParser.ExprContext ctx )
    {
        //TODO: change to real DHIS2 orgUnit logic.

        String savedOrgUnit = currentOrgUnit;

        Object value = orgUnitValue( ctx, "ABC" );

        currentOrgUnit = savedOrgUnit;

        return value;
    }

    /**
     * Interates over the current orgUnit's descendants.
     *
     * @param ctx the parsing context, containing the descendant level
     *            (1=chilren, 2=grandchildren, etc.)
     * @return the multiple values from the orgUnits.
     */
    protected Object iterateOuDescendant( ExpressionParser.ExprContext ctx )
    {
        //TODO: change to real DHIS2 orgUnit logic.

        String savedOrgUnit = currentOrgUnit;

        MultiValues values = new MultiValues();

        values.addValue( orgUnitValue( ctx, "ABC.XYZ.DEF" ) );
        values.addValue( orgUnitValue( ctx, "ABC.XYZ.GHI" ) );

        currentOrgUnit = savedOrgUnit;

        return values;
    }

    /**
     * Interates over the current orgUnit's peers.
     * <ol>
     *     <li>The number of times removed from the current orgUnit
     *     (0=self, 1=siblings, 2=cousins but not siblings, etc.)</li>
     *     <li>If two arguments are present, the two are a starting
     *     and ending range of times removed. For example, (0,1) is
     *     self plus all siblings (all the parent's children), and (0,2)
     *     is self, all siblings and all cousins (all the grandparent's
     *     grandchildren).</li>
     * </ol>
     *
     * @param ctx the parsing context, one or two arguments.
     * @return the multiple values from the orgUnits.
     */
    protected Object iterateOuPeer( ExpressionParser.ExprContext ctx )
    {
        //TODO: change to real DHIS2 orgUnit logic.

        String savedOrgUnit = currentOrgUnit;

        MultiValues values = new MultiValues();

        values.addValue( orgUnitValue( ctx, "ABC.DEF" ) );
        values.addValue( orgUnitValue( ctx, "ABC.GHI" ) );

        currentOrgUnit = savedOrgUnit;

        return values;
    }

    /**
     * Iterates over one or more orgUnit groups.
     *
     * @param ctx the parsing context, with the group(s) to iterate over.
     * @return the multiple values from the orgUnits.
     */
    protected Object iterateOuGroup( ExpressionParser.ExprContext ctx )
    {
        //TODO: change to real DHIS2 orgUnit logic.

        String savedOrgUnit = currentOrgUnit;

        MultiValues values = new MultiValues();

        values.addValue( orgUnitValue( ctx, "ABCXYZ" ) );
        values.addValue( orgUnitValue( ctx, "DEFXYZ" ) );

        currentOrgUnit = savedOrgUnit;

        return values;
    }

    /**
     * Returns the expression value, evaluated at the given orgUnit.
     *
     * @param ctx
     * @param orgUnit
     * @return
     */
    protected Object orgUnitValue( ExpressionParser.ExprContext ctx, String orgUnit )
    {
        currentOrgUnit = orgUnit;

        return visit( ctx.expr( 0 ) );
    }

    /**
     * Casts object as Integer, or throw exception if we can't.
     *
     * @param object the value to cast as an Integer.
     * @return Integer value.
     */
    protected Integer castInteger( Object object )
    {
        Double d = castDouble( object );

        if ( d == null )
        {
            throw new ParsingException( "null found at: '" + object.toString() + "'" );
        }

        Integer i = (int) (double) d;

        if ( (double) d != i )
        {
            throw new ParsingException( "integer expected at: '" + object.toString() + "'" );
        }

        return i;
    }

    /**
     * Casts object as Double, or throw exception if we can't.
     *
     * @param object the value to cast as a Double.
     * @return Double value.
     */
    protected Double castDouble( Object object )
    {
        try
        {
            if ( object.getClass() == String.class )
            {
                return Double.valueOf( (String) object );
            }

            return (Double) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "number expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as Boolean, or throw exception if we can't.
     *
     * @param object the value to cast as a Boolean.
     * @return Boolean value.
     */
    protected Boolean castBoolean( Object object )
    {
        try
        {
            return (Boolean) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "boolean expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as String, or throw exception if we can't.
     *
     * @param object the value to cast as a String.
     * @return String value.
     */
    protected String castString( Object object )
    {
        try
        {
            return (String) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "string expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as Multivalues, or throws exception if we can't.
     *
     * @param object the value to cast as a Multivalues.
     * @return Multivalues object.
     */
    protected MultiValues castMultiValues( Object object )
    {
        if ( !( object instanceof MultiValues ) )
        {
            throw new ParsingException( "multiple values expected at: '" + object.toString() + "'" );
        }

        return (MultiValues) object;
    }

    /**
     * Casts object as MultiPeriodValues, or throws exception if we can't.
     *
     * @param object the value to cast as a MultiPeriodvalues.
     * @return MultiPeriodvalues object.
     */
    protected MultiPeriodValues castMultiPeriodvalues( Object object )
    {
        if ( ! (object instanceof MultiPeriodValues ) )
        {
            throw new ParsingException( "multiple period values expected at: '" + object.toString() + "'" );
        }

        return (MultiPeriodValues) object;
    }

    /**
     * Gets from the object a Double array
     */
    protected Double[] castDoubleArray( Object object )
    {
        return castMultiValues( object ).getValues().stream()
            .map( v -> castDouble( v ) ).collect( Collectors.toList() ).toArray( new Double[0] );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private int evalIntDefault( ExpressionParser.ExprContext ctx, int defaultValue )
    {
        if ( ctx == null )
        {
            return defaultValue;
        }
        else
        {
            return coalesce( castInteger( visit( ctx ) ), defaultValue );
        }
    }

    private Integer coalesce( Integer... integers )
    {
        for ( Integer i : integers )
        {
            if ( i != null )
            {
                return i;
            }
        }

        return null;
    }

    /**
     * Gets an Integer range from arguments 1 - 2.
     *
     * @param ctx the parsing context.
     * @return the range.
     */
    private Range getRange( ExpressionParser.ExprContext ctx )
    {
        Range range = new Range();

        if ( ctx.a1_2().expr( 1 ) != null )
        {
            range.setFrom( coalesce( castInteger( visit( ctx.a1_2().expr( 1 ) ) ), 0 ) );
        }

        if ( ctx.a1_2().expr( 2 ) != null )
        {
            range.setTo( coalesce( castInteger( visit( ctx.a1_2().expr( 2 ) ) ), range.getFrom() ) );
        }

        return range;
    }

    /**
     * Offsets a range relative to a fixed value.
     *
     * @param fixedValue the fixed value to offset the range from.
     * @param polarity 1 for positive or -1 for negative offset.
     * @param range the range.
     * @return the range offset from the fixed value.
     */
    private Range relativeRange( Integer fixedValue, Integer polarity, Range range )
    {
        if ( range.getFrom() != null )
        {
            range.setFrom( fixedValue + polarity * range.getFrom() );
        }

        if ( range.getTo() != null )
        {
            range.setTo( fixedValue + polarity * range.getTo() );
        }

        return range;
    }

    /**
     * Gets a Set of strings from arguments 1 - n.
     *
     * @param ctx the parsing context.
     * @return the Set of string arguments.
     */
    private Set<String> getStrings( ExpressionParser.ExprContext ctx )
    {
        Set<String> strings = new HashSet<>();

        for ( int i = 1; i < ctx.a1_n().expr().size(); i++ )
        {
            strings.add( castString( visit( ctx.a1_n().expr().get( i ) ) ) );
        }

        return strings;
    }

    /**
     * Returns an array of double values for aggregate function processing.
     *
     * @param ctx the parsing context.
     * @return the array of double values.
     */
    private double[] getDoubles( ExpressionParser.ExprContext ctx )
    {
        return ArrayUtils.toPrimitive( castMultiValues( visit( ctx.expr( 0 ) ) )
            .getValues().stream()
            .map( o -> castDouble( o ) ).collect( Collectors.toList() )
            .toArray( new Double[0] ) );
    }

    private List<Object> evalAll( ExpressionParser.ExprContext ctx )
    {
        return evalAll( ctx, 1 );
    }

    private List<Object> evalAll( ExpressionParser.ExprContext ctx, int periodOrder )
    {
        int limit = 0; // Unlimited

        //TODO: iterate through periods and orgUnits in scope

        return Arrays.asList( visit( ctx.expr( 0 ) ) );
    }

    private Object last( ExpressionParser.ExprContext ctx )
    {
        MultiPeriodValues mpv = castMultiPeriodvalues( visit( ctx.expr( 0 ) ) );

        if ( ctx.a0_1().expr() == null )
        {
            List<Object> values = mpv.last( 1 ).getValues();

            return values.isEmpty() ? null : values.get( 0 );
        }
        else
        {
            return mpv.last( castInteger( visit( ctx.a0_1().expr() ) ) );
        }
    }

    private int compare( ExpressionParser.ExprContext ctx )
    {
        Object o1 = visit( ctx.expr( 0 ) );
        Object o2 = visit( ctx.expr( 1 ) );

        if ( o1.getClass() == Double.class )
        {
            return ( (Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o1.getClass() == String.class )
        {
            return ( (String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1.getClass() == Boolean.class )
        {
            return ( (Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else // (Shouldn't happen)
        {
            throw new ParsingException( "magnitude of " + o1.getClass().getSimpleName() + " cannot be compared at: '" + o2.toString() + "'" );
        }
    }
}
