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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalAssertionBuilder;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

import java.io.StringWriter;
import java.util.Collection;

public class LambdaAssertionUtil {

    public static void assertValuesArrayScalar(SupportListener listener, String field, Object... expected) {
        Object result = listener.assertOneGetNew().get(field);
        assertValuesArrayScalar(result, expected);
    }

    public static void assertValuesArrayScalar(Object result, Object... expected) {
        if (expected == null) {
            Assert.assertNull(result);
            return;
        }
        Object[] arr = ((Collection) result).toArray();
        EPAssertionUtil.assertEqualsExactOrder(expected, arr);
    }

    public static void assertST0Id(SupportListener listener, String property, String expectedList) {
        assertST0Id(listener.assertOneGetNew().get(property), expectedList);
    }

    public static void assertST0Id(Object value, String expectedList) {
        SupportBean_ST0[] arr = toArray((Collection<SupportBean_ST0>) value);
        if (expectedList == null && arr == null) {
            return;
        }
        if (expectedList.isEmpty() && arr.length == 0) {
            return;
        }
        String[] expected = expectedList.split(",");
        Assert.assertEquals("Received: " + getIds(arr), expected.length, arr.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], arr[i].getId());
        }
    }

    public static String getIds(SupportBean_ST0[] arr) {
        String delimiter = "";
        StringWriter writer = new StringWriter();
        for (SupportBean_ST0 item : arr) {
            writer.append(delimiter);
            delimiter = ",";
            writer.append(item.getId());
        }
        return writer.toString();
    }

    private static SupportBean_ST0[] toArray(Collection<SupportBean_ST0> it) {
        if (it == null) {
            return null;
        }
        if (it.isEmpty()) {
            return new SupportBean_ST0[0];
        }
        return it.toArray(new SupportBean_ST0[it.size()]);
    }

    public static void assertSingleAndEmptySupportColl(SupportEvalBuilder builder, String[] fields) {
        SupportEvalAssertionBuilder assertionOne = builder.assertion(SupportCollection.makeString("E1"));
        for (String field : fields) {
            assertionOne.verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1"));
        }

        SupportEvalAssertionBuilder assertionTwo = builder.assertion(SupportCollection.makeString(null));
        for (String field : fields) {
            assertionTwo.verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value, null));
        }

        SupportEvalAssertionBuilder assertionThree = builder.assertion(SupportCollection.makeString(""));
        for (String field : fields) {
            assertionThree.verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value));
        }
    }
}
