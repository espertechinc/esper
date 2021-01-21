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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

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
            env.assertListenerNotInvoked("s0");

            sendMarketDataEvent(env, "CSCO");
            env.assertListenerInvoked("s0");

            sendMarketDataEvent(env, "IBM");
            env.assertListenerNotInvoked("s0");

            sendMarketDataEvent(env, "CSCO");
            env.assertListenerInvoked("s0");

            // invalid return type for filter during compilation time
            env.tryInvalidCompile(
                "Select theString From SupportBean#time(30 seconds) where intPrimitive group by theString",
                "Failed to validate expression: The where-clause filter expression must return a boolean value");

            // invalid return type for filter at eventService
            epl = "select * From MapEventWithCriteriaBool#time(30 seconds) where criteria";
            env.compileDeploy(epl);

            env.assertThat(() -> {
                try {
                    env.sendEventMap(Collections.singletonMap("criteria", 15), "MapEventWithCriteriaBool");
                    fail(); // ensure exception handler rethrows
                } catch (EPException ex) {
                    // fine
                }
            });
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
            env.assertListenerNotInvoked("s0");

            sendSupportBeanEvent(env, 2, 2, 2, 2);
            env.assertEventNew("s0", event -> {
                assertEquals(Long.class, event.getEventType().getPropertyType("p1"));
                assertEquals(4L, event.get("p1"));
                assertEquals(Double.class, event.getEventType().getPropertyType("p2"));
                assertEquals(4d, event.get("p2"));
                assertEquals(Double.class, event.getEventType().getPropertyType("p3"));
                assertEquals(1d, event.get("p3"));
            });

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
