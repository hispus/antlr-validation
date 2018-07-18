public class Range
{
    private Integer from = null;

    private Integer to = null;

    public Range()
    {
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        result = prime * result + ( ( from == null ) ? 0 : from );
        result = prime * result + ( ( to == null ) ? 0 : to );

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        Range other = (Range) object;

        if ( from != other.from
            || to != other.to )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "from " + from
            + " to " + to;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Integer getFrom()
    {
        return from;
    }

    public void setFrom( Integer from )
    {
        this.from = from;
    }

    public Integer getTo()
    {
        return to;
    }

    public void setTo( Integer to )
    {
        this.to = to;
    }
}
