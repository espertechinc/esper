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
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportObjectCtor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExprCoreNewInstance {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExecCoreNewInstanceKeyword(true));
        executions.add(new ExecCoreNewInstanceKeyword(false));
        executions.add(new ExecCoreNewInstanceStreamAlias());
        executions.add(new ExecCoreNewInstanceInvalid());
        return executions;
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
