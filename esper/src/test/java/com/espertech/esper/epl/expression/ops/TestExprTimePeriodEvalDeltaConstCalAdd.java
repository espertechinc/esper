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

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.*;
import junit.framework.TestCase;

import java.util.TimeZone;

public class TestExprTimePeriodEvalDeltaConstCalAdd extends TestCase {

    public void testComputeDelta() throws ExprValidationException {
        ExprTimePeriod timePeriod = new ExprTimePeriodImpl(TimeZone.getDefault(), false, true, false, false, false, false, false, false, false, TimeAbacusMilliseconds.INSTANCE);
        timePeriod.addChildNode(new ExprConstantNodeImpl(1));
        timePeriod.validate(null);

        ExprTimePeriodEvalDeltaConstGivenCalAdd addMonth = (ExprTimePeriodEvalDeltaConstGivenCalAdd) timePeriod.constEvaluator(null);
        assertEquals(28 * 24 * 60 * 60 * 1000L, addMonth.deltaAdd(parse("2002-02-15T09:00:00.000")));
        assertEquals(28 * 24 * 60 * 60 * 1000L, addMonth.deltaSubtract(parse("2002-03-15T09:00:00.000")));

        ExprTimePeriodEvalDeltaResult result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("2002-02-15T09:00:00.000"));
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-03-15T09:00:00.000"), parse("2002-02-15T09:00:00.000"));
        assertEquals(parse("2002-04-15T09:00:00.000") - parse("2002-03-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-03-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-04-15T09:00:00.000"), parse("2002-03-15T09:00:00.000"));
        assertEquals(parse("2002-05-15T09:00:00.000") - parse("2002-04-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-04-15T09:00:00.000"), result.getLastReference());

        // try future reference
        result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("2900-03-15T09:00:00.000"));
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        // try old reference
        result = addMonth.deltaAddWReference(
                parse("2002-02-15T09:00:00.000"), parse("1980-03-15T09:00:00.000"));
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-15T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        // try different-dates
        result = addMonth.deltaAddWReference(
                parse("2002-02-18T09:00:00.000"), parse("1980-03-15T09:00:00.000"));
        assertEquals(parse("2002-03-15T09:00:00.000") - parse("2002-02-18T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-02-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-02-11T09:00:00.000"), parse("2980-03-15T09:00:00.000"));
        assertEquals(parse("2002-02-15T09:00:00.000") - parse("2002-02-11T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-01-15T09:00:00.000"), result.getLastReference());

        result = addMonth.deltaAddWReference(
                parse("2002-04-05T09:00:00.000"), parse("2002-02-11T09:01:02.003"));
        assertEquals(parse("2002-04-11T09:01:02.003") - parse("2002-04-05T09:00:00.000"), result.getDelta());
        assertEquals(parse("2002-03-11T09:01:02.003"), result.getLastReference());
    }

    private long parse(String date) {
        return DateTime.parseDefaultMSec(date);
    }
}
