/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstMsec;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaResult;
import junit.framework.TestCase;

public class TestExprTimePeriodEvalDeltaConstMsec extends TestCase {

    public void testComputeDelta()
    {
        ExprTimePeriodEvalDeltaConstMsec delta500 = new ExprTimePeriodEvalDeltaConstMsec(500);
        assertEquals(500, delta500.deltaMillisecondsAdd(0));
        assertEquals(500, delta500.deltaMillisecondsSubtract(0));

        ExprTimePeriodEvalDeltaConstMsec delta10k = new ExprTimePeriodEvalDeltaConstMsec(10000);
        assertEquals(10000, delta10k.deltaMillisecondsAdd(0));
        assertEquals(10000, delta10k.deltaMillisecondsSubtract(0));

        // With current=2300, ref=1000, and interval=500, expect 2500 as next interval and 200 as solution
        // the reference will stay the same since the computation is cheap without updated reference
        ExprTimePeriodEvalDeltaResult result = delta500.deltaMillisecondsAddWReference(2300, 1000);
        assertEquals(200, result.getDelta());
        assertEquals(1000, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2300, 4200);
        assertEquals(400, result.getDelta());
        assertEquals(4200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2200, 4200);
        assertEquals(500, result.getDelta());
        assertEquals(4200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2200, 2200);
        assertEquals(500, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2201, 2200);
        assertEquals(499, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2600, 2200);
        assertEquals(100, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2699, 2200);
        assertEquals(1, result.getDelta());
        assertEquals(2200, result.getLastReference());

        result = delta500.deltaMillisecondsAddWReference(2699, 2700);
        assertEquals(1, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaMillisecondsAddWReference(2699, 2700);
        assertEquals(1, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaMillisecondsAddWReference(2700, 2700);
        assertEquals(10000, result.getDelta());
        assertEquals(2700, result.getLastReference());

        result = delta10k.deltaMillisecondsAddWReference(2700, 6800);
        assertEquals(4100, result.getDelta());
        assertEquals(6800, result.getLastReference());

        result = delta10k.deltaMillisecondsAddWReference(23050, 16800);
        assertEquals(3750, result.getDelta());
        assertEquals(16800, result.getLastReference());
    }
}
