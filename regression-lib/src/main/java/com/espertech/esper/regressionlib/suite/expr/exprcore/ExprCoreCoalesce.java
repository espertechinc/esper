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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExprCoreCoalesce {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreCoalesceBeans());
        executions.add(new ExprCoreCoalesceLong());
        executions.add(new ExprCoreCoalesceLongOM());
        executions.add(new ExprCoreCoalesceLongCompile());
        executions.add(new ExprCoreCoalesceDouble());
        executions.add(new ExprCoreCoalesceNull());
        executions.add(new ExprCoreCoalesceInvalid());
        return executions;
    }

    private static class ExprCoreCoalesceBeans implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select coalesce(a.theString, b.theString) as myString, coalesce(a, b) as myBean" +
                " from pattern [every (a=SupportBean(theString='s0') or b=SupportBean(theString='s1'))]";
            env.compileDeploy(epl).addListener("s0");

            SupportBean theEvent = sendEvent(env, "s0");
            EventBean eventReceived = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("s0", eventReceived.get("myString"));
            assertSame(theEvent, eventReceived.get("myBean"));

            theEvent = sendEvent(env, "s1");
            eventReceived = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("s1", eventReceived.get("myString"));
            assertSame(theEvent, eventReceived.get("myBean"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCoalesceLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0')  select coalesce(longBoxed, intBoxed, shortBoxed) as result from SupportBean").addListener("s0");

            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("result"));

            tryCoalesceLong(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCoalesceLongOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select coalesce(longBoxed,intBoxed,shortBoxed) as result" +
                " from SupportBean#length(1000)";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.coalesce(
                "longBoxed", "intBoxed", "shortBoxed"), "result"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName()).addView("length", Expressions.constant(1000))));
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("result"));

            tryCoalesceLong(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCoalesceLongCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select coalesce(longBoxed,intBoxed,shortBoxed) as result" +
                " from SupportBean#length(1000)";

            env.eplToModelCompileDeploy(epl).addListener("s0");
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("result"));

            tryCoalesceLong(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCoalesceDouble implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select coalesce(null, byteBoxed, shortBoxed, intBoxed, longBoxed, floatBoxed, doubleBoxed) as result from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendEventWithDouble(env, null, null, null, null, null, null);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, null, Short.parseShort("2"), null, null, null, 1d);
            assertEquals(2d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, null, null, null, null, null, 100d);
            assertEquals(100d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, null, null, null, null, 10f, 100d);
            assertEquals(10d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, null, null, 1, 5L, 10f, 100d);
            assertEquals(1d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, Byte.parseByte("3"), null, null, null, null, null);
            assertEquals(3d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEventWithDouble(env, null, null, null, 5L, 10f, 100d);
            assertEquals(5d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCoalesceInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryCoalesceInvalid(env, "coalesce(intPrimitive)");
            tryCoalesceInvalid(env, "coalesce(intPrimitive, string)");
            tryCoalesceInvalid(env, "coalesce(intPrimitive, xxx)");
            tryCoalesceInvalid(env, "coalesce(intPrimitive, booleanBoxed)");
            tryCoalesceInvalid(env, "coalesce(charPrimitive, longBoxed)");
            tryCoalesceInvalid(env, "coalesce(charPrimitive, string, string)");
            tryCoalesceInvalid(env, "coalesce(string, longBoxed)");
            tryCoalesceInvalid(env, "coalesce(null, longBoxed, string)");
            tryCoalesceInvalid(env, "coalesce(null, null, boolBoxed, 1l)");
        }
    }

    private static class ExprCoreCoalesceNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select coalesce(null, null) as result from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(null, env.statement("s0").getEventType().getPropertyType("result"));

            env.sendEventBean(new SupportBean());
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static void tryCoalesceInvalid(RegressionEnvironment env, String coalesceExpr) {
        String epl = "select " + coalesceExpr + " as result from SupportBean";
        tryInvalidCompile(env, epl, "skip");
    }

    private static void tryCoalesceLong(RegressionEnvironment env) {
        sendEvent(env, 1L, 2, (short) 3);
        assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(env, null, 2, null);
        assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(env, null, null, Short.parseShort("3"));
        assertEquals(3L, env.listener("s0").assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(env, null, null, null);
        assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(env, longBoxed, intBoxed, shortBoxed);
    }

    private static void sendBoxedEvent(RegressionEnvironment env, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEventWithDouble(RegressionEnvironment env, Byte byteBoxed, Short shortBoxed, Integer intBoxed, Long longBoxed, Float floatBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setByteBoxed(byteBoxed);
        bean.setShortBoxed(shortBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(bean);
    }
}
