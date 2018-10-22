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
package com.espertech.esper.common.internal.epl.expression.time.eval;

import junit.framework.TestCase;

public class TestTimePeriodComputeConstGivenDelta extends TestCase {

    public void testComputeDelta() {
        TimePeriodComputeConstGivenDeltaEval delta500 = new TimePeriodComputeConstGivenDeltaEval(500);
        assertEquals(500, delta500.deltaAdd(0, null, true, null));
        assertEquals(500, delta500.deltaSubtract(0, null, true, null));

        TimePeriodComputeConstGivenDeltaEval delta10k = new TimePeriodComputeConstGivenDeltaEval(10000);
        assertEquals(10000, delta10k.deltaAdd(0, null, true, null));
        assertEquals(10000, delta10k.deltaSubtract(0, null, true, null));

        // With current=2300, ref=1000, and interval=500, expect 2500 as next interval and 200 as solution
        // the reference will stay the same since the computation is cheap without updated reference
        TimePeriodDeltaResult result = delta500.deltaAddWReference(2300, 1000, null, true, null);
        assertEquals(200, result.getDelta());
        assertEquals(1000, result.getLastReference());

        result = delta500.deltaAddWReference(2300, 4200, null, true, null);
        assertEquals(400, result.getDelta());
        assertEquals(4200, result.getLastReference());

        result = delta500.deltaAddWReference(2200, 4200, null, true, null);
        assertEquals(500, result.getDelta());
        assertEquals(4200, result.getLastReference());

        result = delta500.deltaAddWReference(2200, 2200, null, true, null);
        assertEquals(500, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaAddWReference(2201, 2200, null, true, null);
        assertEquals(499, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaAddWReference(2600, 2200, null, true, null);
        assertEquals(100, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaAddWReference(2699, 2200, null, true, null);
        assertEquals(1, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaAddWReference(2699, 2700, null, true, null);
        assertEquals(1, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaAddWReference(2699, 2700, null, true, null);
        assertEquals(1, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaAddWReference(2700, 2700, null, true, null);
        assertEquals(10000, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaAddWReference(2700, 6800, null, true, null);
        assertEquals(4100, result.getDelta());
        assertEquals(6800, result.getLastReference());

        result = delta10k.deltaAddWReference(23050, 16800, null, true, null);
        assertEquals(3750, result.getDelta());
        assertEquals(16800, result.getLastReference());
    }
}
