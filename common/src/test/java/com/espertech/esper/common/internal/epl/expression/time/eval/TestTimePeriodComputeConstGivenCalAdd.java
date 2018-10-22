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

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusMilliseconds;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdder;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdderMonth;
import junit.framework.TestCase;

import java.util.TimeZone;

public class TestTimePeriodComputeConstGivenCalAdd extends TestCase {

    public void testComputeDelta() {
        TimePeriodComputeConstGivenCalAddEval addMonth = new TimePeriodComputeConstGivenCalAddEval();
        addMonth.setTimeZone(TimeZone.getDefault());
        addMonth.setTimeAbacus(TimeAbacusMilliseconds.INSTANCE);
        TimePeriodAdder[] adders = new TimePeriodAdder[1];
        adders[0] = new TimePeriodAdderMonth();
        addMonth.setAdders(adders);
        addMonth.setAdded(new int[]{1});
        addMonth.setIndexMicroseconds(-1);

        assertEquals(28 * 24 * 60 * 60 * 1000L, addMonth.deltaAdd(parse("2002-02-15T09:00:00.000"), null, true, null));
        assertEquals(28 * 24 * 60 * 60 * 1000L, addMonth.deltaSubtract(parse("2002-03-15T09:00:00.000"), null, true, null));

        TimePeriodDeltaResult result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("2002-02-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-03-15T09:00:00.000"), parse("2002-02-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-04-15T09:00:00.000") - parse("2002-03-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-03-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-04-15T09:00:00.000"), parse("2002-03-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-05-15T09:00:00.000") - parse("2002-04-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-04-15T09:00:00.000"), result.getLastReference());

        // try future reference
        result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("2900-03-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        // try old reference
        result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("1980-03-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        // try different-dates
        result = addMonth.deltaAddWReference(
                parse("2002-02-18T09:00:00.000"), parse("1980-03-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-18T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-02-11T09:00:00.000"), parse("2980-03-15T09:00:00.000"), null, true, null);
        assertEquals(parse("2002-02-15T09:00:00.000") - parse("2002-02-11T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-01-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-04-05T09:00:00.000"), parse("2002-02-11T09:01:02.003"), null, true, null);
        assertEquals(parse("2002-04-11T09:01:02.003") - parse("2002-04-05T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-03-11T09:01:02.003"), result.getLastReference());
    }

    private long parse(String date) {
        return DateTime.parseDefaultMSec(date);
    }
}
