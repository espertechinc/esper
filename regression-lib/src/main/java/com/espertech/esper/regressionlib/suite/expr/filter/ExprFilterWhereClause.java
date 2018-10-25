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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.fail;

public class ExprFilterWhereClause {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterWhereClauseSimple());
        executions.add(new ExprFilterWhereClauseNumericType());
        return executions;
    }

    private static class ExprFilterWhereClauseSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportMarketDataBean#length(3) where symbol='CSCO'";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendMarketDataEvent(env, "IBM");
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendMarketDataEvent(env, "CSCO");
            TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());

            sendMarketDataEvent(env, "IBM");
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendMarketDataEvent(env, "CSCO");
            TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());

            // invalid return type for filter during compilation time
            SupportMessageAssertUtil.tryInvalidCompile(env,
                "Select theString From SupportBean#time(30 seconds) where intPrimitive group by theString",
                "Error validating expression: The where-clause filter expression must return a boolean value");

            // invalid return type for filter at eventService
            epl = "select * From MapEventWithCriteriaBool#time(30 seconds) where criteria";
            env.compileDeploy(epl);

            try {
                env.sendEventMap(Collections.singletonMap("criteria", 15), "MapEventWithCriteriaBool");
                fail(); // ensure exception handler rethrows
            } catch (EPException ex) {
                // fine
            }
            env.undeployAll();
        }
    }

    private static class ExprFilterWhereClauseNumericType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                " intPrimitive + longPrimitive as p1," +
                " intPrimitive * doublePrimitive as p2," +
                " floatPrimitive / doublePrimitive as p3" +
                " from SupportBean#length(3) where " +
                "intPrimitive=longPrimitive and intPrimitive=doublePrimitive and floatPrimitive=doublePrimitive";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSupportBeanEvent(env, 1, 2, 3, 4);
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendSupportBeanEvent(env, 2, 2, 2, 2);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            Assert.assertEquals(Long.class, theEvent.getEventType().getPropertyType("p1"));
            Assert.assertEquals(4L, theEvent.get("p1"));
            Assert.assertEquals(Double.class, theEvent.getEventType().getPropertyType("p2"));
            Assert.assertEquals(4d, theEvent.get("p2"));
            Assert.assertEquals(Double.class, theEvent.getEventType().getPropertyType("p3"));
            Assert.assertEquals(1d, theEvent.get("p3"));

            env.undeployAll();
        }
    }

    private static void sendMarketDataEvent(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive, long longPrimitive, float floatPrimitive, double doublePrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setFloatPrimitive(floatPrimitive);
        theEvent.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(theEvent);
    }
}
