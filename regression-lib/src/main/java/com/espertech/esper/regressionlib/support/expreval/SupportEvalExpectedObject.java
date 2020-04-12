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
package com.espertech.esper.regressionlib.support.expreval;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;

public class SupportEvalExpectedObject extends SupportEvalExpected {
    private final Object expected;

    public SupportEvalExpectedObject(Object expected) {
        this.expected = expected;
    }

    public void assertValue(String message, Object actual) {
        EPAssertionUtil.assertEqualsAllowArray(message, expected, actual);
    }
}
