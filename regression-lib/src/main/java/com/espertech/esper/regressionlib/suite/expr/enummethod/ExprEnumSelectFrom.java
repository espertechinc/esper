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
import java.util.Map;

public class ExprEnumSelectFrom {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumNew());
        execs.add(new ExprEnumSelect());
        execs.add(new ExprEnumEventSelectWIndex());
        execs.add(new ExprEnumScalarSelectWIndex());
        return execs;
    }

    private static class ExprEnumScalarSelectWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            String eplFragment = "@name('s0') select strvals.selectFrom( (v, i) => v || '_' || Integer.toString(i)) as c0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "c0", "E1_0", "E2_1", "E3_2");
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString(""));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "c0", new String[0]);
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumEventSelectWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplFragment = "@name('s0') select " +
                "contained.selectFrom( (v, i) => new {c0=v.id,c1=i}) as val0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E1", 0}, {"E2", 1}, {"E3", 2}});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("E4,0,1"));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E4", 0}});

            env.sendEventBean(SupportBean_ST0_Container.make3Value(null));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","), null);

            env.sendEventBean(SupportBean_ST0_Container.make3Value());
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[0][]);

            env.undeployAll();
        }
    }

    private static class ExprEnumNew implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select " +
                "contained.selectFrom(x => new {c0 = id||'x', c1 = key0||'y'}) as val0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E1x", "12y"}, {"E2x", "11y"}, {"E3x", "2y"}});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("E4,0,1"));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E4x", "0y"}});

            env.sendEventBean(SupportBean_ST0_Container.make3Value(null));
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","), null);

            env.sendEventBean(SupportBean_ST0_Container.make3Value());
            EPAssertionUtil.assertPropsPerRow(toMapArray(env.listener("s0").assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[0][]);

            env.undeployAll();
        }
    }

    private static class ExprEnumSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select " +
                "contained.selectFrom(x => id) as val0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Collection.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E3,2"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1", "E2", "E3");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", null);
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", new String[0]);
            env.listener("s0").reset();
            env.undeployAll();

            // test scalar-coll with lambda
            String[] fields = "val0".split(",");
            String eplLambda = "@name('s0') select " +
                "strvals.selectFrom(v => extractNum(v)) as val0 " +
                "from SupportCollection";
            env.compileDeploy(eplLambda).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", 2, 1, 5, 4);
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString("E1"));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", 1);
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString(null));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", null);
            env.listener("s0").reset();

            env.sendEventBean(SupportCollection.makeString(""));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0");

            env.undeployAll();
        }
    }

    private static Map[] toMapArray(Object result) {
        if (result == null) {
            return null;
        }
        Collection<Map> val = (Collection<Map>) result;
        return val.toArray(new Map[val.size()]);
    }
}
