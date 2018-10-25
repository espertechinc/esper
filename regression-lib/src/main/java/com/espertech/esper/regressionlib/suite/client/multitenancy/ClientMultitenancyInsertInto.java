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
package com.espertech.esper.regressionlib.suite.client.multitenancy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerPath;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class ClientMultitenancyInsertInto {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientMultitenancyInsertIntoSingleModuleTwoStatements());
        execs.add(new ClientMultitenancyInsertIntoTwoModule());
        return execs;
    }

    public static class ClientMultitenancyInsertIntoSingleModuleTwoStatements implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('s0') insert into SomeStream select theString, intPrimitive from SupportBean;" +
                    "@name('s1') select theString, intPrimitive from SomeStream(intPrimitive = 0)";
            EPCompiled compiled = env.compile(epl);

            env.deploy(compiled).addListener("s1").milestone(0);

            sendAssert(env, "E1", 0, true);
            sendAssert(env, "E2", 1, false);

            env.milestone(1);

            sendAssert(env, "E3", 1, false);
            sendAssert(env, "E4", 0, true);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String theString, int intPrimitive, boolean received) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            if (received) {
                EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "theString,intPrimitive".split(","), new Object[]{theString, intPrimitive});
            } else {
                assertFalse(env.listener("s1").isInvoked());
            }
        }
    }

    public static class ClientMultitenancyInsertIntoTwoModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC);
            EPCompiled first = env.compile("@name('s0') insert into SomeStream select theString as a, intPrimitive as b from SupportBean", args);
            EPCompiled second = env.compile("@name('s1') select a, b from SomeStream", new CompilerArguments(env.getConfiguration()).setPath(new CompilerPath().add(first)));

            env.deploy(first).milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));

            env.deploy(second).addListener("s1").milestone(1);

            sendAssert(env, "E2", 2);
            sendAssert(env, "E3", 3);

            env.milestone(2);

            sendAssert(env, "E4", 4);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String theString, int intPrimitive) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "a,b".split(","), new Object[]{theString, intPrimitive});
        }
    }

}
