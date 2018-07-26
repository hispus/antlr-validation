import java.util.Map;

public class ExpressionEvaluator extends ExpressionChecker
{
    private Map<String, Double> valueMap;

    private Map<String, Double> constantMap;

    //TODO: This goes away when we finish the TODOs.
    private final static Double PLACEHOLDER = Double.valueOf( 2. );

    public ExpressionEvaluator( Map<String, Double> valueMap, Map<String, Double> constantMap )
    {
        this.valueMap = valueMap;
        this.constantMap = constantMap;
    }

    @Override
    public Object visitConstant( ExpressionParser.ConstantContext ctx )
    {
        return constantMap.get( ctx.getToken( ExpressionParser.UID, 0 ).getText() );
    }

    @Override
    public Object visitOrgUnitCount( ExpressionParser.OrgUnitCountContext ctx )
    {
        //TODO: write the real OrgUnitCount code
        return PLACEHOLDER;
    }

    @Override
    public Object visitReportingRate( ExpressionParser.ReportingRateContext ctx )
    {
        //TODO: write the real ReportingRate code
        return PLACEHOLDER;
    }

    @Override
    public Object visitDays( ExpressionParser.DaysContext ctx )
    {
        //TODO: write the real Days-in-the-month code
        return 31.;
    }

    @Override
    protected Object functionAnd( ExpressionParser.ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            && castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionOr( ExpressionParser.ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            || castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionIf( ExpressionParser.ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            ? visit( ctx.a2().expr( 0 ) )
            : visit( ctx.a2().expr( 1 ) );
    }

    @Override
    protected Object functionCoalesce( ExpressionParser.ExprContext ctx )
    {
        for ( ExpressionParser.ExprContext c : ctx.a1_n().expr() )
        {
            Object val = visit( c );
            if ( val != null )
            {
                return val;
            }
        }
        return null;
    }
}
