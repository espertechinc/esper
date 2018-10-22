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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

public class ExprEnumTakeAndTakeLast {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumTakeEvents());
        execs.add(new ExprEnumTakeScalar());
        return execs;
    }

    private static class ExprEnumTakeEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
            String epl = "@name('s0') select " +
                "contained.take(2) as val0," +
                "contained.take(1) as val1," +
                "contained.take(0) as val2," +
                "contained.take(-1) as val3," +
                "contained.takeLast(2) as val4," +
                "contained.takeLast(1) as val5" +
                " from SupportBean_ST0_Container";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class, Collection.class, Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val4", "E2,E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val5", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val4", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val5", "E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val4", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val5", "E1");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, "");
            }
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, null);
            }
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumTakeScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3".split(",");
            String epl = "@name('s0') select " +
                "strvals.take(2) as val0," +
                "strvals.take(1) as val1," +
                "strvals.takeLast(2) as val2," +
                "strvals.takeLast(1) as val3" +
                " from SupportCollection";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1", "E2");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val2", "E2", "E3");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val3", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString("E1,E2"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1", "E2");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val2", "E1", "E2");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val3", "E2");
            env.listener("s0").reset();

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(env, fields);

            env.undeployAll();
        }
    }
}
