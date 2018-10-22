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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

public class ExprEnumAggregate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAggregateEvents());
        execs.add(new ExprEnumAggregateScalar());
        return execs;
    }

    private static class ExprEnumAggregateEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"val0", "val1", "val2"};
            String eplFragment = "@name('s0') select " +
                "contained.aggregate(0, (result, item) => result + item.p00) as val0, " +
                "contained.aggregate('', (result, item) => result || ', ' || item.id) as val1, " +
                "contained.aggregate('', (result, item) => result || (case when result='' then '' else ',' end) || item.id) as val2 " +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, String.class, String.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{25, ", E1, E2, E2", "E1,E2,E2"});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{null, null, null});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(new String[0]));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{0, "", ""});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,12"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{12, ", E1", "E1"});

            env.undeployAll();
        }
    }

    private static class ExprEnumAggregateScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.aggregate('', (result, item) => result || '+' || item) as val0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"+E1+E2+E3"});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"+E1"});

            env.sendEventBean(SupportCollection.makeString(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{""});

            env.sendEventBean(SupportCollection.makeString(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.undeployAll();
        }
    }
}
