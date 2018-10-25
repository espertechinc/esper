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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EPLSubselectMultirow {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectMultirowSingleColumn());
        execs.add(new EPLSubselectMultirowUnderlyingCorrelated());
        return execs;
    }

    private static class EPLSubselectMultirowSingleColumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // test named window as well as stream
            String epl = "create window SupportWindow#length(3) as SupportBean;\n" +
                "insert into SupportWindow select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            epl = "@name('s0') select p00, (select window(intPrimitive) from SupportBean#keepall sb) as val from SupportBean_S0 as s0;\n";
            env.compileDeploy(epl, path).addListener("s0").milestone(0);

            String[] fields = "p00,val".split(",");

            Object[][] rows = new Object[][]{
                {"p00", String.class},
                {"val", Integer[].class}
            };
            for (int i = 0; i < rows.length; i++) {
                String message = "Failed assertion for " + rows[i][0];
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(message, rows[i][0], prop.getPropertyName());
                Assert.assertEquals(message, rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(new SupportBean("T1", 5));
            env.sendEventBean(new SupportBean("T2", 10));
            env.sendEventBean(new SupportBean("T3", 15));
            env.sendEventBean(new SupportBean("T1", 6));
            env.sendEventBean(new SupportBean_S0(0));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertTrue(event.get("val") instanceof Integer[]);
            EPAssertionUtil.assertProps(event, fields, new Object[]{null, new Integer[]{5, 10, 15, 6}});

            // test named window and late start
            env.undeployModuleContaining("s0");

            epl = "@name('s0') select p00, (select window(intPrimitive) from SupportWindow) as val from SupportBean_S0 as s0";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, new int[]{10, 15, 6}});   // length window 3

            env.sendEventBean(new SupportBean("T1", 5));
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, new int[]{15, 6, 5}});   // length window 3

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowUnderlyingCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select p00, " +
                "(select window(sb.*) from SupportBean#keepall sb where theString = s0.p00) as val " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            Object[][] rows = new Object[][]{
                {"p00", String.class},
                {"val", SupportBean[].class}
            };
            for (int i = 0; i < rows.length; i++) {
                String message = "Failed assertion for " + rows[i][0];
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(message, rows[i][0], prop.getPropertyName());
                Assert.assertEquals(message, rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(new SupportBean_S0(1, "T1"));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            SupportBean sb1 = new SupportBean("T1", 10);
            env.sendEventBean(sb1);
            env.sendEventBean(new SupportBean_S0(2, "T1"));

            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(SupportBean[].class, received.get("val").getClass());
            EPAssertionUtil.assertEqualsAnyOrder((Object[]) received.get("val"), new Object[]{sb1});

            SupportBean sb2 = new SupportBean("T2", 20);
            env.sendEventBean(sb2);
            SupportBean sb3 = new SupportBean("T2", 30);
            env.sendEventBean(sb3);
            env.sendEventBean(new SupportBean_S0(3, "T2"));

            received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsAnyOrder((Object[]) received.get("val"), new Object[]{sb2, sb3});

            env.undeployAll();
        }
    }
}
