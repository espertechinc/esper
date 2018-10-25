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

import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SupportBeanAssertionUtil {

    public static void assertPropsPerRow(Object[] beans, String[] fields, Object[][] expected) {
        assertEquals(beans.length, expected.length);
        for (int i = 0; i < beans.length; i++) {
            assertPropsBean((SupportBean) beans[i], fields, expected[i]);
        }
    }

    public static void assertPropsBean(SupportBean bean, String[] fields, Object[] expected) {
        int count = -1;
        for (String field : fields) {
            count++;
            if (field.equals("theString")) {
                assertEquals(expected[count], bean.getTheString());
            } else if (field.equals("intPrimitive")) {
                assertEquals(expected[count], bean.getIntPrimitive());
            } else if (field.equals("longPrimitive")) {
                assertEquals(expected[count], bean.getLongPrimitive());
            } else {
                fail("unrecognized field " + field);
            }
        }
    }
}
