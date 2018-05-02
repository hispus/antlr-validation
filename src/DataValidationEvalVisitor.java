import java.util.Map;

import static java.lang.Math.pow;

public class DataValidationEvalVisitor extends DataValidationBaseVisitor<Object>
{
    //TODO: allow both numeric and String values in valueMap
    //TODO: implicit type conversions when comparing/concatenating numeric with String
    //TODO: implement missingValueStrategy

    private boolean prepass;

    private Map<String, Double> valueMap;

    private Map<String, Double> constantMap;

    final static Double ONE = Double.valueOf( 1. );

    //TODO: This goes away when we finish the TODOs.
    final static Double PLACEHOLDER = Double.valueOf( 2. );

    public DataValidationEvalVisitor()
    {
        prepass = true;
    }

    public DataValidationEvalVisitor( Map<String, Double> valueMap, Map<String, Double> constantMap )
    {
        this.valueMap = valueMap;
        this.constantMap = constantMap;
        prepass = false;
    }

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public Object visitExpr( DataValidationParser.ExprContext ctx )
    {
        if ( ctx.uop != null )
        {
            return visitUninaryOperator( ctx );
        }
        else if ( ctx.op != null )
        {
            return visitBinaryOperator( ctx );
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
    public Object visitDimensionItemObject( DataValidationParser.DimensionItemObjectContext ctx )
    {
        //TODO: for data elmenets, return object type based on data element type
        //TODO: for non-typed DimensionItemObjects, return Double on prepass.
        return prepass ? ONE : valueMap.get( ctx.getChild( 0 ).getText() );
    }

    @Override
    public Object visitConstant( DataValidationParser.ConstantContext ctx )
    {
        return prepass ? ONE : constantMap.get( ctx.getToken( DataValidationParser.UID, 0 ).getText() );
    }

    @Override
    public Object visitOrgUnitCount(DataValidationParser.OrgUnitCountContext ctx)
    {
        //TODO: write the real OrgUnitCount code
        return prepass ? ONE : PLACEHOLDER;
    }

    @Override
    public Object visitReportingRate(DataValidationParser.ReportingRateContext ctx)
    {
        //TODO: write the real ReportingRate code
        return prepass ? ONE : PLACEHOLDER;
    }

    @Override
    public Object visitDays(DataValidationParser.DaysContext ctx)
    {
        //TODO: write the real Days-in-the-month code
        return prepass ? ONE : 31.;
    }

    @Override
    public Object visitNumericLiteral(DataValidationParser.NumericLiteralContext ctx)
    {
        return Double.valueOf( ctx.getText() );
    }

    @Override
    public Object visitStringLiteral(DataValidationParser.StringLiteralContext ctx)
    {
        return ctx.getText().substring( 1, ctx.getText().length() - 1 ); // Strip off quotes
    }

    @Override
    public Object visitBooleanLiteral(DataValidationParser.BooleanLiteralContext ctx)
    {
        return Boolean.valueOf( ctx.getText() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Object visitUninaryOperator( DataValidationParser.ExprContext ctx )
    {
        switch ( ctx.uop.getType() )
        {
            case DataValidationParser.NOT:
                return ! castBoolean( visit( ctx.expr( 0 ) ) );

            case DataValidationParser.MINUS:
                return - castDouble( visit( ctx.expr( 0 ) ) );

            default: // (Shouldn't happen)
                throw new RuntimeException( "grammar unary operator uop=" + ctx.uop.getType() + " not recognized." );
        }
    }

    private Object visitBinaryOperator( DataValidationParser.ExprContext ctx )
    {
        switch ( ctx.op.getType() )
        {
            case DataValidationParser.PLUS: // String concatenation or numeric addition
                Object left = visit( ctx.expr( 0 ) );
                Object right = visit( ctx.expr( 1 ) );

                if ( left.getClass() == String.class )
                {
                    return castString( left )
                        + castString( right );
                }

                return castDouble( left )
                    + castDouble( right );

            case DataValidationParser.MINUS:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    - castDouble( visit( ctx.expr( 1 ) ) );

            case DataValidationParser.POWER:
                return pow( castDouble( visit( ctx.expr( 0 ) ) ),
                    castDouble( visit( ctx.expr( 1 ) ) ) );

            case DataValidationParser.MUL:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    * castDouble( visit( ctx.expr( 1 ) ) );

            case DataValidationParser.DIV:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    / castDouble( visit( ctx.expr( 1 ) ) );

            case DataValidationParser.MOD:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    % castDouble( visit( ctx.expr( 1 ) ) );

            case DataValidationParser.LEQ:
                return compare( ctx ) <= 0;

            case DataValidationParser.GEQ:
                return compare( ctx ) >= 0;

            case DataValidationParser.LT:
                return compare( ctx ) < 0;

            case DataValidationParser.GT:
                return compare( ctx ) > 0;

            case DataValidationParser.EQ:
                return equals( ctx );

            case DataValidationParser.NE:
                return ! equals( ctx );

            case DataValidationParser.AND:
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

            case DataValidationParser.OR:
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

            default: // (Shouldn't happen)
                throw new RuntimeException( "grammar binary operator op=" + ctx.uop.getType() + " not recognized." );
        }
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
            throw new RuntimeException( "number expected at: '" + object.toString() + "'" );
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
            throw new RuntimeException( "boolean expected at: '" + object.toString() + "'" );
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
            throw new RuntimeException( "string expected at: '" + object.toString() + "'" );
        }
    }

    private int compare( DataValidationParser.ExprContext ctx )
    {
        Object left = visit( ctx.expr( 0 ) );
        Object right = visit( ctx.expr( 1 ) );

        if ( left.getClass() == Double.class )
        {
            return ( (Double) left).compareTo( castDouble( right ) );
        }
        else if ( left.getClass() == String.class )
        {
            return ( (String) left).compareTo( castString( right ) );
        }
        else
        {
            throw new RuntimeException( "magnitude of " + left.getClass().getSimpleName() + " cannot be compared at: '" + left.toString() + "'" );
        }
    }

    private boolean equals( DataValidationParser.ExprContext ctx )
    {
        Object left = visit( ctx.expr( 0 ) );
        Object right = visit( ctx.expr( 1 ) );

        return left.equals( right );
    }
}
