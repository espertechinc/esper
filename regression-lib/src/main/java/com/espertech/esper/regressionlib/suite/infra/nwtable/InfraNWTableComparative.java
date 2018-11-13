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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

public class InfraNWTableComparative {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        String eplNamedWindow =
            "create window TotalsWindow#unique(theString) as (theString string, total int);" +
                "insert into TotalsWindow select theString, sum(intPrimitive) as total from SupportBean group by theString;" +
                "@Name('s0') select p00 as c0, " +
                "    (select total from TotalsWindow tw where tw.theString = s0.p00) as c1 from SupportBean_S0 as s0;";
        execs.add(new InfraNWTableComparativeGroupByTopLevelSingleAgg("named window", 1000, eplNamedWindow, 1));

        String eplTable =
            "create table varTotal (key string primary key, total sum(int));\n" +
                "into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n" +
                "@Name('s0') select p00 as c0, varTotal[p00].total as c1 from SupportBean_S0;\n";
        execs.add(new InfraNWTableComparativeGroupByTopLevelSingleAgg("table", 1000, eplTable, 1));
        return execs;
    }

    private static class InfraNWTableComparativeGroupByTopLevelSingleAgg implements RegressionExecution {
        private final String caseName;
        private final int numEvents;
        private final String epl;
        private final int numSets;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public InfraNWTableComparativeGroupByTopLevelSingleAgg(String caseName, int numEvents, String epl, int numSets) {
            this.caseName = caseName;
            this.numEvents = numEvents;
            this.epl = epl;
            this.numSets = numSets;
        }

        public void run(RegressionEnvironment env) {
            final String[] fields = "c0,c1".split(",");
            env.compileDeploy(epl).addListener("s0");

            long startLoad = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            long deltaLoad = System.nanoTime() - startLoad;

            long startQuery = System.nanoTime();
            for (int j = 0; j < numSets; j++) {
                for (int i = 0; i < numEvents; i++) {
                    String key = "E" + i;
                    env.sendEventBean(new SupportBean_S0(0, key));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{key, i});
                }
            }
            long deltaQuery = System.nanoTime() - startQuery;

            /** Comment-me-inn:
             System.out.println(caseName + ": Load " + deltaLoad/1000000d +
             " Query " + deltaQuery / 1000000d +
             " Total " + (deltaQuery+deltaLoad) / 1000000d );
             */
            env.undeployAll();
        }
    }
}
