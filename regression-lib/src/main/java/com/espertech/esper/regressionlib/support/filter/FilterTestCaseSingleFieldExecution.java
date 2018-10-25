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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

public class FilterTestCaseSingleFieldExecution implements RegressionExecution {
    private final FilterTestCaseSingleField testCase;
    private final String testCaseName;
    private final String stats;

    public FilterTestCaseSingleFieldExecution(Class originator, FilterTestCaseSingleField testCase, String stats) {
        this.testCase = testCase;
        this.testCaseName = originator.getSimpleName() + " permutation [" + testCase.getFilterExpr() + "]";
        this.stats = stats;
    }

    public String name() {
        return testCaseName;
    }

    public void run(RegressionEnvironment env) {

        // set up statement
        String stmtName = "stmt";
        String expr = "@name('" + stmtName + "') select * from SupportBean" + testCase.getFilterExpr();
        env.compileDeployAddListenerMileZero(expr, stmtName);
        SupportListener initialListener = env.listener(stmtName);

        for (int i = 0; i < testCase.getValues().length; i++) {
            sendBean(env, testCase.getFieldName(), testCase.getValues()[i]);
            Assert.assertEquals("Listener invocation unexpected for " + testCase.getFilterExpr() + " field " + testCase.getFieldName() + "=" + testCase.getValues()[i], testCase.getIsInvoked()[i], env.listener(stmtName).getIsInvokedAndReset());
        }

        env.milestone(1);
        env.undeployModuleContaining(stmtName);
        env.milestone(2);

        for (int i = 0; i < testCase.getValues().length; i++) {
            sendBean(env, testCase.getFieldName(), testCase.getValues()[i]);
            Assert.assertFalse(initialListener.isInvoked());
        }
    }

    private void sendBean(RegressionEnvironment env, String fieldName, Object value) {
        SupportBean theEvent = new SupportBean();
        if (fieldName.equals("theString")) {
            theEvent.setTheString((String) value);
        }
        if (fieldName.equals("boolPrimitive")) {
            theEvent.setBoolPrimitive((Boolean) value);
        }
        if (fieldName.equals("intBoxed")) {
            theEvent.setIntBoxed((Integer) value);
        }
        if (fieldName.equals("longBoxed")) {
            theEvent.setLongBoxed((Long) value);
        }
        env.sendEventBean(theEvent);
    }
}
