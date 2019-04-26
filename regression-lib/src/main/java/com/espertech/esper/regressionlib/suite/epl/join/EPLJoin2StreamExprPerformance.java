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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class EPLJoin2StreamExprPerformance implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String epl;
        AtomicInteger milestone = new AtomicInteger();

        epl = "@name('s0') select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = 'E6750'";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6750);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = 'E6749'";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6749);

        epl = "create variable string myconst = 'E6751';\n" +
            "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = myconst;\n";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6751);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = (id || '6752')";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6752);

        epl = "@name('s0') select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = (id || '6753')";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6753);

        epl = "@name('s0') select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = 'E6754' and sb.intPrimitive=6754";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6754);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = (id || '6755') and sb.intPrimitive=6755";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6755);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between 6756 and 6756";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6756);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= 6757 and intPrimitive <= 6757";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6757);

        epl = "@name('s0') select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= (rangeStart + 1) and intPrimitive <= (rangeEnd - 1)";
        tryAssertion(env, epl, milestone, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "@name('s0') select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= 6001 and intPrimitive <= (rangeEnd - 1)";
        tryAssertion(env, epl, milestone, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "@name('s0') select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between (rangeStart + 1) and (rangeEnd - 1)";
        tryAssertion(env, epl, milestone, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "@name('s0') select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between (rangeStart + 1) and 6004";
        tryAssertion(env, epl, milestone, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "@name('s0') select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive in (6001 : (rangeEnd - 1)]";
        tryAssertion(env, epl, milestone, new SupportBeanRange("R1", 6000, 6005), 6002 + 6003 + 6004);

        epl = "@name('s0') select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = 'E6758' and sb.intPrimitive >= 6758 and intPrimitive <= 6758";
        tryAssertion(env, epl, milestone, new SupportBean_ST0("E", -1), 6758);
    }

    private static void tryAssertion(RegressionEnvironment env, String epl, AtomicInteger milestone, Object theEvent, Object expected) {

        String[] fields = "val".split(",");
        env.compileDeploy(epl).addListener("s0");

        // preload
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean("E" + i, i));
        }

        env.milestoneInc(milestone);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(theEvent);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 2000);
        log.info("delta=" + delta);

        env.undeployAll();
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin2StreamExprPerformance.class);
}
