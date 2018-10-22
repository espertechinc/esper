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

public class ExprEnumTakeWhileAndWhileLast {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumTakeWhileEvents());
        execs.add(new ExprEnumTakeWhileScalar());
        return execs;
    }

    private static class ExprEnumTakeWhileEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3".split(",");
            String epl = "@name('s0') select " +
                "contained.takeWhile(x => x.p00 > 0) as val0," +
                "contained.takeWhile( (x, i) => x.p00 > 0 and i<2) as val1," +
                "contained.takeWhileLast(x => x.p00 > 0) as val2," +
                "contained.takeWhileLast( (x, i) => x.p00 > 0 and i<2) as val3" +
                " from SupportBean_ST0_Container";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2,E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E1,E2,E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E2,E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,0", "E2,2", "E3,3"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E2,E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E2,E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,0", "E3,3"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,0"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E1");
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

    private static class ExprEnumTakeWhileScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3".split(",");
            String epl = "@name('s0') select " +
                "strvals.takeWhile(x => x != 'E1') as val0," +
                "strvals.takeWhile( (x, i) => x != 'E1' and i<2) as val1," +
                "strvals.takeWhileLast(x => x != 'E1') as val2," +
                "strvals.takeWhileLast( (x, i) => x != 'E1' and i<2) as val3" +
                " from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val2", "E2", "E3", "E4");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val3", "E3", "E4");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }
}
