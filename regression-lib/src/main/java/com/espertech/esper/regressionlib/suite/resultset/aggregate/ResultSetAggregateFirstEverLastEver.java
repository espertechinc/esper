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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;

import java.util.ArrayList;
import java.util.Collection;

public class ResultSetAggregateFirstEverLastEver {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateFirstLastEver(true));
        execs.add(new ResultSetAggregateFirstLastEver(false));
        execs.add(new ResultSetAggregateFirstLastInvalid());
        execs.add(new ResultSetAggregateOnDelete());
        return execs;
    }

    private static class ResultSetAggregateFirstLastInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select countever(distinct intPrimitive) from SupportBean",
                "Failed to validate select-clause expression 'countever(distinct intPrimitive)': Aggregation function 'countever' does now allow distinct [");
        }
    }

    private static class ResultSetAggregateOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "firsteverstring,lasteverstring,counteverall".split(",");
            String epl = "create window MyWindow#keepall as select * from SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_A delete from MyWindow where theString = id;\n" +
                "@name('s0') select firstever(theString) as firsteverstring, " +
                "lastever(theString) as lasteverstring," +
                "countever(*) as counteverall from MyWindow";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1L});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2", 2L});

            env.sendEventBean(new SupportBean("E3", 30));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

            env.milestone(1);

            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

            env.milestone(2);

            env.sendEventBean(new SupportBean_A("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateFirstLastEver implements RegressionExecution {

        private final boolean soda;

        public ResultSetAggregateFirstLastEver(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select " +
                "firstever(theString) as firsteverstring, " +
                "lastever(theString) as lasteverstring, " +
                "first(theString) as firststring, " +
                "last(theString) as laststring, " +
                "countever(*) as cntstar, " +
                "countever(intBoxed) as cntexpr, " +
                "countever(*,boolPrimitive) as cntstarfiltered, " +
                "countever(intBoxed,boolPrimitive) as cntexprfiltered " +
                "from SupportBean.win:length(2)";
            env.compileDeploy(soda, epl).addListener("s0");

            String[] fields = "firsteverstring,lasteverstring,firststring,laststring,cntstar,cntexpr,cntstarfiltered,cntexprfiltered".split(",");

            env.milestone(0);

            makeSendBean(env, "E1", 10, 100, true);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", "E1", "E1", 1L, 1L, 1L, 1L});

            env.milestone(1);

            makeSendBean(env, "E2", 11, null, true);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2", "E1", "E2", 2L, 1L, 2L, 1L});

            env.milestone(2);

            makeSendBean(env, "E3", 12, 120, false);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", "E2", "E3", 3L, 2L, 2L, 1L});

            env.milestone(3);

            makeSendBean(env, "E4", 13, 130, true);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E4", "E3", "E4", 4L, 3L, 3L, 2L});

            env.undeployAll();
        }
    }

    private static void makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, Integer intBoxed, boolean boolPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setIntBoxed(intBoxed);
        sb.setBoolPrimitive(boolPrimitive);
        env.sendEventBean(sb);
    }
}