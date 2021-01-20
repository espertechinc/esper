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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEventTwo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EPLSubselectOrderOfEval {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectCorrelatedSubqueryOrder());
        execs.add(new EPLSubselectOrderOfEvaluationSubselectFirst());
        return execs;
    }

    private static class EPLSubselectCorrelatedSubqueryOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from SupportTradeEventTwo#lastevent;\n" +
                "@name('s0') select window(tl.*) as longItems, " +
                "       (SELECT window(ts.*) AS shortItems FROM SupportTradeEventTwo#time(20 minutes) as ts WHERE ts.securityID=tl.securityID) " +
                "from SupportTradeEventTwo#time(20 minutes) as tl " +
                "where tl.securityID = 1000" +
                "group by tl.securityID";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportTradeEventTwo(System.currentTimeMillis(), 1000, 50, 1));
            env.assertEventNew("s0", event -> {
                assertEquals(1, ((Object[]) event.get("longItems")).length);
                assertEquals(1, ((Object[]) event.get("shortItems")).length);
            });

            env.sendEventBean(new SupportTradeEventTwo(System.currentTimeMillis() + 10, 1000, 50, 1));
            env.assertEventNew("s0", event -> {
                assertEquals(2, ((Object[]) event.get("longItems")).length);
                assertEquals(2, ((Object[]) event.get("shortItems")).length);
            });

            env.undeployAll();
        }
    }

    private static class EPLSubselectOrderOfEvaluationSubselectFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') @name('s0')select * from SupportBean(intPrimitive<10) where intPrimitive not in (select intPrimitive from SupportBean#unique(intPrimitive))";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean("E1", 5));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();

            String eplTwo = "@name('s0') select * from SupportBean where intPrimitive not in (select intPrimitive from SupportBean(intPrimitive<10)#unique(intPrimitive))";
            env.compileDeployAddListenerMile(eplTwo, "s0", 1);

            env.sendEventBean(new SupportBean("E1", 5));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

}