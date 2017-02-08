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
package com.espertech.esper.supportregression.bean.lambda;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportCollection;
import org.junit.Assert;

import java.io.StringWriter;
import java.util.Collection;

public class LambdaAssertionUtil {

    public static void assertValuesArrayScalar(SupportUpdateListener listener, String field, Object... expected) {
        Object result = listener.assertOneGetNew().get(field);
        if (expected == null) {
            Assert.assertNull(result);
            return;
        }
        Object[] arr = ((Collection) result).toArray();
        EPAssertionUtil.assertEqualsExactOrder(expected, arr);
    }

    public static void assertST0Id(SupportUpdateListener listener, String property, String expectedList) {
        SupportBean_ST0[] arr = toArray((Collection<SupportBean_ST0>) listener.assertOneGetNew().get(property));
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

    public static void assertTypes(EventType type, String[] fields, Class[] classes) {
        int count = 0;
        for (String field : fields) {
            Assert.assertEquals("position " + count, classes[count++], type.getPropertyType(field));
        }
    }

    public static void assertTypesAllSame(EventType type, String[] fields, Class clazz) {
        int count = 0;
        for (String field : fields) {
            Assert.assertEquals("position " + count, clazz, type.getPropertyType(field));
        }
    }

    public static void assertSingleAndEmptySupportColl(EPServiceProvider epService, SupportUpdateListener listener, String[] fields) {
        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field, "E1");
        }
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field, null);
        }
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field);
        }
        listener.reset();
    }
}
