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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportObjectCtor;

import java.util.*;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertProps;
import static org.junit.Assert.*;

public class ExprCoreNewInstance {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExecCoreNewInstanceKeyword(true));
        executions.add(new ExecCoreNewInstanceKeyword(false));
        executions.add(new ExecCoreNewInstanceStreamAlias());
        executions.add(new ExecCoreNewInstanceInvalid());
        executions.add(new ExecCoreNewInstanceArraySized(false));
        executions.add(new ExecCoreNewInstanceArraySized(true));
        executions.add(new ExecCoreNewInstanceArrayInvalid());
        return executions;
    }

    private static class ExecCoreNewInstanceArraySized implements RegressionExecution {
        boolean soda;

        public ExecCoreNewInstanceArraySized(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "new double[1], " +
                "new Integer[2*2] as c1, " +
                "new java.util.Calendar[intPrimitive] as c2, " +
                "new double[1][2], " +
                "new java.util.Calendar[intPrimitive][intPrimitive] as c4 " +
                "from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");

            EventType out = env.statement("s0").getEventType();
            assertEquals(double[].class, out.getPropertyType("new double[1]"));
            assertEquals(Integer[].class, out.getPropertyType("c1"));
            assertEquals(Calendar[].class, out.getPropertyType("c2"));
            assertEquals(double[][].class, out.getPropertyType("new double[1][2]"));
            assertEquals(Calendar[][].class, out.getPropertyType("c4"));

            env.sendEventBean(new SupportBean("E1", 2));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertProps(event, "new double[1],c1,c2,new double[1][2],c4".split(","),
                new Object[] {new double[1], new Integer[4], new Calendar[2], new double[1][2], new Calendar[2][2]});

            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceArrayInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select new double[] from SupportBean",
                "Incorrect syntax near ']'");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select new double[1, 2, 3] from SupportBean",
                "Incorrect syntax near ',' expecting a right angle bracket ']'");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select new double['a'] from SupportBean",
                "Failed to validate select-clause expression 'new double[\"a\"]': New-keyword with an array-type result requires an Integer-typed dimension but received type 'java.lang.String'");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select new double[1]['a'] from SupportBean", "skip");

            String epl = "@name('s0') select new double[intBoxed] from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (RuntimeException ex) {
                // expected, rethrown
                assertTrue(ex.getMessage().contains("new-array received a null value for dimension"));
            }
            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // try variable
            env.compileDeploy("create constant variable java.util.concurrent.atomic.AtomicInteger cnt = new java.util.concurrent.atomic.AtomicInteger(1)");

            // try shallow invalid cases
            SupportMessageAssertUtil.tryInvalidCompile(env, "select new Dummy() from SupportBean",
                "Failed to validate select-clause expression 'new Dummy()': Failed to resolve new-operator class name 'Dummy'");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select new SupportPrivateCtor() from SupportBean",
                "Failed to validate select-clause expression 'new SupportPrivateCtor()': Failed to find a suitable constructor for class ");

            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceStreamAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select new SupportObjectCtor(sb) as c0 from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");

            SupportBean sb = new SupportBean();
            env.sendEventBean(sb);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertSame(sb, ((SupportObjectCtor) event.get("c0")).getObject());

            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceKeyword implements RegressionExecution {
        private final boolean soda;

        public ExecCoreNewInstanceKeyword(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "new SupportBean(\"A\",intPrimitive) as c0, " +
                "new SupportBean(\"B\",intPrimitive+10), " +
                "new SupportBean() as c2, " +
                "new SupportBean(\"ABC\",0).getTheString() as c3 " +
                "from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");
            Object[][] expectedAggType = new Object[][]{{"c0", SupportBean.class}, {"new SupportBean(\"B\",intPrimitive+10)", SupportBean.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            env.sendEventBean(new SupportBean("E1", 10));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertSupportBean(event.get("c0"), new Object[]{"A", 10});
            assertSupportBean(((Map) event.getUnderlying()).get("new SupportBean(\"B\",intPrimitive+10)"), new Object[]{"B", 20});
            assertSupportBean(event.get("c2"), new Object[]{null, 0});
            assertEquals("ABC", event.get("c3"));

            env.undeployAll();
        }

        private void assertSupportBean(Object bean, Object[] objects) {
            SupportBean b = (SupportBean) bean;
            assertEquals(objects[0], b.getTheString());
            assertEquals(objects[1], b.getIntPrimitive());
        }
    }
}
