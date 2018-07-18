import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

/**
 * Visitor to the ANTLR nodes in a parsed expression
 */
public class ExpressionEvalVisitor extends ExpressionBaseVisitor<Object>
{
    //TODO: allow both numeric and String values in valueMap
    //TODO: implicit type conversions when comparing/concatenating numeric with String
    //TODO: implement missingValueStrategy

    private boolean prepass;

    private Map<String, Double> valueMap;

    private Map<String, Double> constantMap;

    private Scopes scopes = new Scopes();

    private Scope scope;

    private Comparator<Object> objectComparator = new ObjectComparator();

    final static Double ONE = Double.valueOf( 1. );

    private int orgUnitLevel = 3; // Reporting orgUnit level for demonstration

    //TODO: This goes away when we finish the TODOs.
    final static Double PLACEHOLDER = Double.valueOf( 2. );

    private SetMap<String, Scope> dimensionItemScopes = new SetMap<>();

    public ExpressionEvalVisitor()
    {
        prepass = true;
        scope = scopes.getCurrentScope();
    }

    public ExpressionEvalVisitor( Map<String, Double> valueMap, Map<String, Double> constantMap )
    {
        this.valueMap = valueMap;
        this.constantMap = constantMap;
        prepass = false;
        scope = scopes.getCurrentScope();
    }

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
    public Object visitDimensionItemObject( ExpressionParser.DimensionItemObjectContext ctx )
    {
        //TODO: for data elmenets, return object type based on data element type
        //TODO: for non-typed DimensionItemObjects, return Double on prepass.

        String item = ctx.getChild( 0 ).getText();

        if ( prepass )
        {
            dimensionItemScopes.putValue( item, scope );

            return ONE;
        }

        String s = scopes.toString();

        if ( ! s.isEmpty() )
        {
            System.out.println("Evaluating " + item + " in scope " + scopes.toString() );
        }
        return valueMap.get( item );
    }

    @Override
    public Object visitConstant( ExpressionParser.ConstantContext ctx )
    {
        return prepass ? ONE : constantMap.get( ctx.getToken( ExpressionParser.UID, 0 ).getText() );
    }

    @Override
    public Object visitOrgUnitCount( ExpressionParser.OrgUnitCountContext ctx )
    {
        //TODO: write the real OrgUnitCount code
        return prepass ? ONE : PLACEHOLDER;
    }

    @Override
    public Object visitReportingRate( ExpressionParser.ReportingRateContext ctx )
    {
        //TODO: write the real ReportingRate code
        return prepass ? ONE : PLACEHOLDER;
    }

    @Override
    public Object visitDays( ExpressionParser.DaysContext ctx )
    {
        //TODO: write the real Days-in-the-month code
        return prepass ? ONE : 31.;
    }

    @Override
    public Object visitNumericLiteral( ExpressionParser.NumericLiteralContext ctx )
    {
        return Double.valueOf( ctx.getText() );
    }

    @Override
    public Object visitStringLiteral(ExpressionParser.StringLiteralContext ctx)
    {
        return ctx.getText().substring( 1, ctx.getText().length() - 1 ); // Strip off quotes
    }

    @Override
    public Object visitBooleanLiteral(ExpressionParser.BooleanLiteralContext ctx)
    {
        return Boolean.valueOf( ctx.getText() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Handles expression operators and functions.
     *
     * @param ctx expression context
     * @return evaluated expression object
     */
    private Object function( ExpressionParser.ExprContext ctx )
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
                    return - castDouble( visit( ctx.expr( 0 ) ) );
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
                return ! castBoolean( visit( ctx.expr( 0 ) ) );

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
                if ( prepass ) // Prepass: always evaluate both sides
                {
                    Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
                    Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

                    return leftBool && rightBool;
                }
                else
                {
                    return castBoolean( visit( ctx.expr( 0 ) ) )
                        && castBoolean( visit( ctx.expr( 1 ) ) );
                }

            case ExpressionParser.OR:
                if ( prepass ) // Prepass: always evaluate both sides
                {
                    Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
                    Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

                    return leftBool || rightBool;
                }
                else
                {
                    return castBoolean( visit( ctx.expr( 0 ) ) )
                        || castBoolean( visit( ctx.expr( 1 ) ) );
                }

            // -----------------------------------------------------------------
            // Logical functions
            // -----------------------------------------------------------------

            case ExpressionParser.IF:
                if ( prepass ) // Prepass: always evaluate both possible return values
                {
                    Object o1 = visit( ctx.expr( 1 ) );
                    Object o2 = visit( ctx.expr( 2 ) );

                    return castBoolean( visit( ctx.expr( 0 ) ) )
                        ? o1
                        : o2;
                }

                return castBoolean( visit( ctx.expr( 0 ) ) )
                    ? visit( ctx.expr( 1 ) )
                    : visit( ctx.expr( 2 ) );

            case ExpressionParser.IS_NULL:
                return evalAll( ctx ).size();

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
                return StatUtils.percentile( getDoubles( ctx ), castDouble( visit( ctx.arg ) ) );

            // -----------------------------------------------------------------
            // Aggregation scope functions
            // -----------------------------------------------------------------

            case ExpressionParser.PERIOD:
                scopes.push().withPeriods( getRange( ctx ) ).withYears( getYearRange( ctx ) );
                return visitAndPop( ctx );

            case ExpressionParser.OU_LEVEL:
                scopes.push().withOrgUnitLevels( getRange( ctx ) );
                return visitAndPop( ctx );

            case ExpressionParser.OU_ANCESTOR:
                scopes.push().withOrgUnitLevels( relativeRange( orgUnitLevel, -1, getRange( ctx ) ) );
                return visitAndPop( ctx );

            case ExpressionParser.OU_DESCENDANT:
                scopes.push().withOrgUnitLevels( relativeRange( orgUnitLevel, 1, getRange( ctx ) ) );
                return visitAndPop( ctx );

            case ExpressionParser.OU_PEER:
                scopes.push().withOrgUnitPeers( defaultRange( 1, getRange( ctx ) ) );
                return visitAndPop( ctx );

            case ExpressionParser.OU_GROUP:
                scopes.push().withOrgUnitGroups( getStrings( ctx ) );
                return visitAndPop( ctx );

            default: // (Shouldn't happen, mismatch between expression grammer and here.)
                throw new ParsingException( "fun=" + ctx.fun.getType() + " not recognized." );
        }
    }

    Object visitAndPop( ExpressionParser.ExprContext ctx )
    {
        Object value = visit( ctx.expr( 0 ) );

        scope = scopes.pop();

        return value;
    }

    private Range getRange( ExpressionParser.ExprContext ctx )
    {
        Range range = new Range();

        if ( ctx.from != null )
        {
            range.setFrom( castInteger( visit( ctx.from ) ) );
        }

        if ( ctx.to != null )
        {
            range.setFrom( castInteger( visit( ctx.to ) ) );
        }

        return range;
    }

    private Range getYearRange( ExpressionParser.ExprContext ctx )
    {
        Range years = null;

        if ( ctx.fromYear != null )
        {
            years = new Range();

            years.setFrom( castInteger( visit( ctx.from ) ) );

            if ( ctx.toYear != null )
            {
                years.setFrom( castInteger( visit( ctx.toYear ) ) );
            }

            scope.withYears( years );
        }

        return years;
    }

    private Range defaultRange( Integer defaultFromValue, Range range )
    {
        if ( range.getFrom() == null )
        {
            range.setFrom( defaultFromValue );
        }

        return range;
    }

    private Range relativeRange( Integer fixedValue, Integer polarity, Range range )
    {
        defaultRange( polarity, range );

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

    private Set<String> getStrings( ExpressionParser.ExprContext ctx )
    {
        Set<String> strings = new HashSet<>();

        for ( int i = 1; i < ctx.expr().size(); i++ )
        {
            strings.add( castString( visit( ctx.expr().get( i ) ) ) );
        }

        return strings;
    }

    private double[] getDoubles( ExpressionParser.ExprContext ctx )
    {
        return ArrayUtils.toPrimitive( evalAll( ctx ).stream()
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

        if ( ctx.last != null )
        {
            periodOrder = -1;

            limit = castInteger( visit( ctx.last ) );
        }

        //TODO: iterate through periods and orgUnits in scope

        return Arrays.asList( visit( ctx.expr( 0 ) ) );
    }

    private Object last( ExpressionParser.ExprContext ctx )
    {
        List<Object> objects = evalAll( ctx, -1 );

        return objects.isEmpty() ? null : objects.get( 0 );
    }

    private Integer castInteger( Object object )
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

    private Double castDouble( Object object )
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

    private Boolean castBoolean( Object object )
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

    private String castString( Object object )
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

    private int compare( ExpressionParser.ExprContext ctx )
    {
        return objectComparator.compare( visit( ctx.expr( 0 ) ), visit( ctx.expr( 1 ) ) );
    }

    private class ObjectComparator
        implements Comparator<Object>
    {
        @Override
        public int compare( Object o1, Object o2 )
        {
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
}
