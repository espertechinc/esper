package com.espertech.esper.example.ohlc;

import java.util.Date;

public class OHLCTick
{
    private String ticker;
    private double price;
    private long timestamp;

    public OHLCTick(String ticker, double price, long timestamp)
    {
        this.ticker = ticker;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getTicker()
    {
        return ticker;
    }

    public double getPrice()
    {
        return price;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String toString()
    {
        return "ticker " + ticker +
               " price " + price +
               " timestamp " + printTime(timestamp);
    }

    private String printTime(long timestamp)
    {
        return new Date(timestamp).toString();
    }
}
