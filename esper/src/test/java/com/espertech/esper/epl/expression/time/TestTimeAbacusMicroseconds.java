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
package com.espertech.esper.epl.expression.time;

import junit.framework.TestCase;

public class TestTimeAbacusMicroseconds extends TestCase {
    private TimeAbacus abacus = TimeAbacusMicroseconds.INSTANCE;

    public void testDeltaFor() {
        assertEquals(0, abacus.deltaForSecondsNumber(0));
        assertEquals(1000000, abacus.deltaForSecondsNumber(1));
        assertEquals(5000000, abacus.deltaForSecondsNumber(5));
        assertEquals(123000, abacus.deltaForSecondsNumber(0.123));
        assertEquals(1, abacus.deltaForSecondsNumber(0.000001));
        assertEquals(10, abacus.deltaForSecondsNumber(0.000010));

        assertEquals(0, abacus.deltaForSecondsNumber(0.0000001));
        assertEquals(1, abacus.deltaForSecondsNumber(0.000000999999));
        assertEquals(5000000, abacus.deltaForSecondsNumber(5.0000001));
        assertEquals(5000001, abacus.deltaForSecondsNumber(5.000000999999));

        for (int i = 1; i < 1000000; i++) {
            double d = ((double) i) / 1000000;
            assertEquals((long) i, abacus.deltaForSecondsNumber(d));
            assertEquals((long) i, abacus.deltaForSecondsDouble(d));
        }
    }
}
