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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithLongArray;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumMinMax {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumMinMaxScalarWithLambda());
        execs.add(new ExprEnumMinMaxEvents());
        execs.add(new ExprEnumMinMaxScalar());
        execs.add(new ExprEnumMinMaxScalarChain());
        execs.add(new ExprEnumInvalid());
        return execs;
    }

    private static class ExprEnumMinMaxScalarChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select coll.max().minus(1 minute) >= coll.min() as c0 from SupportEventWithLongArray");
            env.addListener("s0");
            String[] fields = "c0".split(",");

            env.sendEventBean(new SupportEventWithLongArray("E1", new long[]{150000, 140000, 200000, 190000}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(new SupportEventWithLongArray("E2", new long[]{150000, 139999, 200000, 190000}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.undeployAll();
        }
    }

    private static class ExprEnumMinMaxScalarWithLambda implements RegressionExecution {
        public void run(RegressionEnvironment env) {


            String[] fields = "val0,val1,val2,val3".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.min(v => extractNum(v)) as val0, " +
                "strvals.max(v => extractNum(v)) as val1, " +
                "strvals.min(v => v) as val2, " +
                "strvals.max(v => v) as val3 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class, String.class, String.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 5, "E1", "E5"});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 1, "E1", "E1"});

            env.sendEventBean(SupportCollection.makeString(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.sendEventBean(SupportCollection.makeString(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumMinMaxEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "contained.min(x => p00) as val0, " +
                "contained.max(x => p00) as val1 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Integer.class});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 12});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,12", "E2,0", "E2,2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, 12});

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumMinMaxScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.min() as val0, " +
                "strvals.max() as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");


            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class, String.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E5"});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});

            env.sendEventBean(SupportCollection.makeString(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.sendEventBean(SupportCollection.makeString(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.min() from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.min()': Invalid input for built-in enumeration method 'min' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "'");
        }
    }

    public static class MyService {
        public static int extractNum(String arg) {
            return Integer.parseInt(arg.substring(1));
        }

        public static BigDecimal extractBigDecimal(String arg) {
            return new BigDecimal(arg.substring(1));
        }
    }
}
