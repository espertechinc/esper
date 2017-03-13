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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.epl.agg.aggregator.AggregatorDistinctValue;
import com.espertech.esper.supportunit.epl.SupportAggregator;
import junit.framework.TestCase;

public class TestUniqueValueAggregator extends TestCase {
    private AggregatorDistinctValue agg;

    public void setUp() {
        agg = new AggregatorDistinctValue(new SupportAggregator());
    }

    public void testEnter() {
        agg.enter(1);
        agg.enter(new Integer(10));
        agg.enter(null);
    }

    public void testLeave() {
        agg.enter(1);
        agg.leave(1);
    }

    public void testGetValue() {
        assertEquals(0, agg.getValue());

        agg.enter(10);
        assertEquals(10, agg.getValue());

        agg.enter(10);
        assertEquals(10, agg.getValue());

        agg.enter(2);
        assertEquals(12, agg.getValue());

        agg.leave(10);
        assertEquals(12, agg.getValue());

        agg.leave(10);
        assertEquals(2, agg.getValue());

        agg.leave(2);
        assertEquals(0, agg.getValue());
    }
}
