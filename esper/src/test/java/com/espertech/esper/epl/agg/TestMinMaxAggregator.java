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

import com.espertech.esper.epl.agg.aggregator.AggregatorMinMax;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;
import junit.framework.TestCase;

public class TestMinMaxAggregator extends TestCase {
    public void testAggregatorMax() {
        AggregatorMinMax agg = new AggregatorMinMax(MinMaxTypeEnum.MAX);
        assertEquals(null, agg.getValue());
        agg.enter(10);
        assertEquals(10, agg.getValue());
        agg.enter(20);
        assertEquals(20, agg.getValue());
        agg.enter(10);
        assertEquals(20, agg.getValue());
        agg.leave(10);
        assertEquals(20, agg.getValue());
        agg.leave(20);
        assertEquals(10, agg.getValue());
        agg.leave(10);
        assertEquals(null, agg.getValue());
    }

    public void testAggregatorMin() {
        AggregatorMinMax agg = new AggregatorMinMax(MinMaxTypeEnum.MIN);
        assertEquals(null, agg.getValue());
        agg.enter(10);
        assertEquals(10, agg.getValue());
        agg.enter(20);
        assertEquals(10, agg.getValue());
        agg.enter(10);
        assertEquals(10, agg.getValue());
        agg.leave(10);
        assertEquals(10, agg.getValue());
        agg.leave(20);
        assertEquals(10, agg.getValue());
        agg.leave(10);
        assertEquals(null, agg.getValue());
    }
}
