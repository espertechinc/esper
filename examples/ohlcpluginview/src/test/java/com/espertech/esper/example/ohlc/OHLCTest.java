package com.espertech.esper.example.ohlc;

import junit.framework.TestCase;

/**
 * Test case for OHLC buckets from a tick stream.
 */
public class OHLCTest extends TestCase
{
    public void testSample()
    {
        OHLCMain main = new OHLCMain();
        main.run("OHLCEngineURI");
    }
}
