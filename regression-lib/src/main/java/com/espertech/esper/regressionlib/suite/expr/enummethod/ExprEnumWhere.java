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

public class ExprEnumWhere {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumWhereEvents());
        execs.add(new ExprEnumWhereString());
        return execs;
    }

    private static class ExprEnumWhereEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select " +
                "contained.where(x => p00 = 9) as val0," +
                "contained.where((x, i) => x.p00 = 9 and i >= 1) as val1 from SupportBean_ST0_Container";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0,val1".split(","), new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,9", "E2,1", "E3,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,9"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E3");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", null);
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", null);
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumWhereString implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.where(x => x not like '%1%') as val0, " +
                "strvals.where((x, i) => x not like '%1%' and i > 1) as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");


            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E2", "E3");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString("E4,E2,E1"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E4", "E2");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", new String[0]);
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString(""));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", new String[0]);
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", new String[0]);
            env.listener("s0").reset();

            env.undeployAll();

            // test boolean
            eplFragment = "@name('s0') select " +
                "boolvals.where(x => x) as val0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportCollection.makeBoolean("true,true,false"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", true, true);
            env.listener("s0").reset();

            env.undeployAll();
        }
    }
}
