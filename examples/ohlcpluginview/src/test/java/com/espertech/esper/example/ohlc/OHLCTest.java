/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.example.ohlc;

import junit.framework.TestCase;

/**
 * Test case for OHLC buckets from a tick stream.
 */
public class OHLCTest extends TestCase {
    public void testSample() {
        OHLCMain main = new OHLCMain();
        main.run("OHLCEngineURI");
    }
}
