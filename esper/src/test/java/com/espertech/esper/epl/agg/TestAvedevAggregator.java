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
package com.espertech.esper.epl.agg;

import com.espertech.esper.epl.agg.aggregator.AggregatorAvedev;
import junit.framework.TestCase;

public class TestAvedevAggregator extends TestCase {
    public void testAggregateFunction() {
        AggregatorAvedev agg = new AggregatorAvedev();

        assertNull(agg.getValue());

        agg.enter(82);
        assertEquals(0D, agg.getValue());

        agg.enter(78);
        assertEquals(2D, agg.getValue());

        agg.enter(70);
        double result = (Double) agg.getValue();
        assertEquals("4.4444", Double.toString(result).substring(0, 6));

        agg.enter(58);
        assertEquals(8D, agg.getValue());

        agg.enter(42);
        assertEquals(12.8D, agg.getValue());

        agg.leave(82);
        assertEquals(12D, agg.getValue());

        agg.leave(58);
        result = (Double) agg.getValue();
        assertEquals("14.2222", Double.toString(result).substring(0, 7));
    }

}
