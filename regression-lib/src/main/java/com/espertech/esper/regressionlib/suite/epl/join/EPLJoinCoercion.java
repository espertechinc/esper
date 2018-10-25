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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLJoinCoercion {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinJoinCoercionRange());
        execs.add(new EPLJoinJoinCoercion());
        return execs;
    }

    private static class EPLJoinJoinCoercionRange implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String[] fields = "sbs,sbi,sbri".split(",");
            String epl = "@name('s0') select sb.theString as sbs, sb.intPrimitive as sbi, sbr.id as sbri from SupportBean#length(10) sb, SupportBeanRange#length(10) sbr " +
                "where intPrimitive between rangeStartLong and rangeEndLong";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(SupportBeanRange.makeLong("R1", "G", 100L, 200L));
            env.sendEventBean(new SupportBean("E1", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, "R1"});

            env.sendEventBean(SupportBeanRange.makeLong("R2", "G", 90L, 100L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, "R2"});

            env.sendEventBean(SupportBeanRange.makeLong("R3", "G", 1L, 99L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "R3"});

            env.sendEventBean(SupportBeanRange.makeLong("R4", "G", 2000L, 3000L));
            env.sendEventBean(new SupportBean("E1", 1000));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();

            epl = "@name('s0') select sb.theString as sbs, sb.intPrimitive as sbi, sbr.id as sbri from SupportBean#length(10) sb, SupportBeanRange#length(10) sbr " +
                "where sbr.key = sb.theString and intPrimitive between rangeStartLong and rangeEndLong";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(SupportBeanRange.makeLong("R1", "G", 100L, 200L));
            env.sendEventBean(new SupportBean("G", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G", 101));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G", 101, "R1"});

            env.sendEventBean(SupportBeanRange.makeLong("R2", "G", 90L, 102L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G", 101, "R2"});

            env.sendEventBean(SupportBeanRange.makeLong("R3", "G", 1L, 99L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G", 10, "R3"});

            env.sendEventBean(SupportBeanRange.makeLong("R4", "G", 2000L, 3000L));
            env.sendEventBean(new SupportBean("G", 1000));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLJoinJoinCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select volume from " +
                "SupportMarketDataBean#length(3) as s0," +
                "SupportBean#length(3) as s1 " +
                " where s0.volume = s1.intPrimitive";
            env.compileDeployAddListenerMileZero(joinStatement, "s0");
            sendBeanEvent(env, 100);
            sendMarketEvent(env, 100);
            assertEquals(100L, env.listener("s0").assertOneGetNewAndReset().get("volume"));
            env.undeployAll();
        }
    }

    private static void sendBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendMarketEvent(RegressionEnvironment env, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean("", 0, volume, null);
        env.sendEventBean(bean);
    }
}
