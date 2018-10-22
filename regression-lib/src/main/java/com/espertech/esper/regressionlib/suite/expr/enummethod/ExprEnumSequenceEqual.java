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

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumSequenceEqual {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumSelectFrom());
        execs.add(new ExprEnumTwoProperties());
        execs.add(new ExprEnumInvalid());
        return execs;
    }

    private static class ExprEnumSelectFrom implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select contained.selectFrom(x => key0).sequenceEqual(contained.selectFrom(y => id)) as val0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Boolean.class});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("I1,E1,0", "I2,E2,0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,X4,0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,Y4,0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportBean_ST0_Container.make3Value("I3,I3,0", "Y4,X4,0"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.undeployAll();
        }
    }

    private static class ExprEnumTwoProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select " +
                "strvals.sequenceEqual(strvalstwo) as val0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new Class[]{Boolean.class});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3", "E1,E2,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportCollection.makeString("E1,E3", "E1,E2,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("E1,E3", "E1,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3", "E1,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("E1,E2,null,E3", "E1,E2,null,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportCollection.makeString("E1,E2,E3", "E1,E2,null"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("E1,E2,null", "E1,E2,E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("E1", ""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("", "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString("E1", "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportCollection.makeString("", ""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(SupportCollection.makeString(null, ""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.sendEventBean(SupportCollection.makeString("", null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(SupportCollection.makeString(null, null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.undeployAll();
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select window(*).sequenceEqual(strvals) from SupportCollection#lastevent";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'window(*).sequenceEqual(strvals)': Invalid input for built-in enumeration method 'sequenceEqual' and 1-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type 'SupportCollection'");
        }
    }
}