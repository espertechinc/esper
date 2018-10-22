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

public class ExprEnumReverse {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumReverseEvents());
        execs.add(new ExprEnumReverseScalar());
        return execs;
    }

    private static class ExprEnumReverseEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select contained.reverse() as val from SupportBean_ST0_Container";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val", "E3,E2,E1");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E2,9", "E1,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val", "E1,E2");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1"));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val", "E1");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val", null);
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val", "");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumReverseScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.reverse() as val0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E4", "E5", "E1", "E2");
            env.listener("s0").reset();

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(env, fields);

            env.undeployAll();
        }
    }
}