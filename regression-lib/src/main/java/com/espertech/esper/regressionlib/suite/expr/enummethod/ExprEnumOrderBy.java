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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumOrderBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumOrderByEvents());
        execs.add(new ExprEnumOrderByScalar());
        execs.add(new ExprEnumInvalid());
        return execs;
    }

    private static class ExprEnumOrderByEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
            String eplFragment = "@name('s0') select " +
                "contained.orderBy(x => p00) as val0," +
                "contained.orderBy(x => 10 - p00) as val1," +
                "contained.orderBy(x => 0) as val2," +
                "contained.orderByDesc(x => p00) as val3," +
                "contained.orderByDesc(x => 10 - p00) as val4," +
                "contained.orderByDesc(x => 0) as val5" +
                " from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class, Collection.class, Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E2,E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E2,E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val4", "E1,E2");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val5", "E1,E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val0", "E3,E4,E2,E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E2,E1,E3,E4");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val2", "E3,E2,E4,E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val3", "E2,E1,E3,E4");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val4", "E3,E4,E2,E1");
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val5", "E3,E2,E4,E1");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, null);
            }
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                LambdaAssertionUtil.assertST0Id(env.listener("s0"), field, "");
            }
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumOrderByScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.orderBy() as val0, " +
                "strvals.orderByDesc() as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");


            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1", "E2", "E4", "E5");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E5", "E4", "E2", "E1");
            env.listener("s0").reset();

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(env, fields);
            env.undeployAll();

            // test scalar-coll with lambda
            String eplLambda = "@name('s0') select " +
                "strvals.orderBy(v => extractNum(v)) as val0, " +
                "strvals.orderByDesc(v => extractNum(v)) as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplLambda).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1", "E2", "E4", "E5");
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val1", "E5", "E4", "E2", "E1");
            env.listener("s0").reset();

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(env, fields);

            env.undeployAll();
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.orderBy() from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.orderBy()': Invalid input for built-in enumeration method 'orderBy' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "'");
        }
    }
}
