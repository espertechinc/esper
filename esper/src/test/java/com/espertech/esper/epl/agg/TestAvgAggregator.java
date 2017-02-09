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

import com.espertech.esper.epl.agg.aggregator.AggregatorAvg;
import junit.framework.TestCase;

public class TestAvgAggregator extends TestCase {
    public void testResult() {
        AggregatorAvg agg = new AggregatorAvg();
        agg.enter(100);
        assertEquals(100d, agg.getValue());
        agg.enter(150);
        assertEquals(125d, agg.getValue());
        agg.enter(200);
        assertEquals(150d, agg.getValue());
        agg.leave(100);
        assertEquals(175d, agg.getValue());
    }

}
