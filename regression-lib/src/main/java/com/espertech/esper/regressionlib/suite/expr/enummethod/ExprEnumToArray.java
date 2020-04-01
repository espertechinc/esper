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

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertProps;

public class ExprEnumToArray {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumToArrayWSelectFromScalar());
        execs.add(new ExprEnumToArrayWSelectFromScalarWIndex());
        execs.add(new ExprEnumToArrayWSelectFromEvent());
        return execs;
    }

    private static class ExprEnumToArrayWSelectFromEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplFragment = "@name('s0') select " +
                "contained.selectFrom(v => v.id).toArray() as c0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "c0".split(","), new Class[]{String[].class});

            sendAssert(env, new String[]{"E1,12", "E2,11", "E3,2"}, new String[]{"E1", "E2", "E3"});
            sendAssert(env, new String[]{"E4,14"}, new String[]{"E4"});
            sendAssert(env, new String[0], new String[0]);
            sendAssert(env, null, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String[] csvlist, String[] expected) {
            env.sendEventBean(SupportBean_ST0_Container.make2Value(csvlist));
            assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
        }
    }

    private static class ExprEnumToArrayWSelectFromScalarWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            String eplFragment = "@name('s0') select strvals.selectfrom((v, i) => v || '-' || Integer.toString(i)).toArray() as c0 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String[].class});

            sendAssert(env, "E1,E2,E3", new String[]{"E1-0", "E2-1", "E3-2"});
            sendAssert(env, "E4", new String[]{"E4-0"});
            sendAssert(env, "", new String[0]);
            sendAssert(env, null, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String csv, String[] expected) {
            env.sendEventBean(SupportCollection.makeString(csv));
            assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
        }
    }

    private static class ExprEnumToArrayWSelectFromScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            String epl = "@name('s0') select strvals.selectfrom(v => Integer.parseInt(v)).toArray() as c0 from SupportCollection";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer[].class});

            sendAssert(env, fields, "1,2,3", new Integer[]{1, 2, 3});
            sendAssert(env, fields, "1", new Integer[]{1});
            sendAssert(env, fields, "", new Integer[]{});
            sendAssert(env, fields, null, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String[] fields, String csvlist, Integer[] expected) {
            env.sendEventBean(SupportCollection.makeString(csvlist));
            assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }
}
