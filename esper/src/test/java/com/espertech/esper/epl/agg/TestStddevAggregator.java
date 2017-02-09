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

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.aggregator.AggregatorStddev;
import junit.framework.TestCase;

public class TestStddevAggregator extends TestCase {
    public void testAggregateFunction() {
        AggregationMethod agg = new AggregatorStddev();

        assertNull(agg.getValue());

        agg.enter(10);
        assertNull(agg.getValue());

        agg.enter(8);
        double result = (Double) agg.getValue();
        assertEquals("1.4142", Double.toString(result).substring(0, 6));

        agg.enter(5);
        result = (Double) agg.getValue();
        assertEquals("2.5166", Double.toString(result).substring(0, 6));

        agg.enter(9);
        result = (Double) agg.getValue();
        assertEquals("2.1602", Double.toString(result).substring(0, 6));

        agg.leave(10);
        result = (Double) agg.getValue();
        assertEquals("2.0816", Double.toString(result).substring(0, 6));
    }

    public void testAllOne() {
        AggregationMethod agg = new AggregatorStddev();
        agg.enter(1);
        agg.enter(1);
        agg.enter(1);
        agg.enter(1);
        agg.enter(1);
        assertEquals(0.0d, agg.getValue());
    }

}


