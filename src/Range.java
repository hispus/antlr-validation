public class Range
{
    private int from = 0;

    private int to = 0;

    public Range()
    {
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

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