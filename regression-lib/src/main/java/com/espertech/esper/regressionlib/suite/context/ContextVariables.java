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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionVariableState;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.variable.VariableNotFoundException;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.context.SupportSelectorById;
import org.junit.Assert;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ContextVariables {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextVariablesSegmentedByKey());
        execs.add(new ContextVariablesOverlapping());
        execs.add(new ContextVariablesIterateAndListen());
        execs.add(new ContextVariablesGetSetAPI());
        execs.add(new ContextVariablesInvalid());
        return execs;
    }

    private static class ContextVariablesSegmentedByKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "mycontextvar".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtx as " +
                "partition by theString from SupportBean, p00 from SupportBean_S0", path);
            env.compileDeploy("context MyCtx create variable int mycontextvar = 0", path);
            env.compileDeploy("context MyCtx on SupportBean(intPrimitive > 0) set mycontextvar = intPrimitive", path);

            env.compileDeploy("@name('s0') context MyCtx select mycontextvar from SupportBean_S0", path).addListener("s0");

            env.sendEventBean(new SupportBean("P1", 0));   // allocate partition P1
            env.sendEventBean(new SupportBean("P1", 10));   // set variable
            env.sendEventBean(new SupportBean_S0(1, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});

            env.milestone(0);

            env.sendEventBean(new SupportBean("P2", 11));   // allocate and set variable partition E2
            env.sendEventBean(new SupportBean_S0(2, "P2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(3, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});
            env.sendEventBean(new SupportBean_S0(4, "P2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(5, "P3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0});

            env.sendEventBean(new SupportBean("P3", 12));
            env.sendEventBean(new SupportBean_S0(6, "P3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{12});

            env.undeployAll();
        }
    }

    private static class ContextVariablesOverlapping implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "mycontextvar".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtx as " +
                "initiated by SupportBean_S0 s0 terminated by SupportBean_S1(p10 = s0.p00)", path);
            env.compileDeploy("context MyCtx create variable int mycontextvar = 5", path);
            env.compileDeploy("context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive", path);
            env.compileDeploy("context MyCtx on SupportBean(intPrimitive < 0) set mycontextvar = intPrimitive", path);

            env.compileDeploy("@name('s0') context MyCtx select mycontextvar from SupportBean_S2(p20 = context.s0.p00)", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "P1"));    // allocate partition P1

            env.milestone(1);

            env.sendEventBean(new SupportBean_S2(1, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(0, "P2"));    // allocate partition P2

            env.milestone(3);

            env.sendEventBean(new SupportBean("P2", 10));

            env.milestone(4);

            env.sendEventBean(new SupportBean_S2(2, "P2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});

            // set all to -1
            env.sendEventBean(new SupportBean("P2", -1));

            env.milestone(5);

            env.sendEventBean(new SupportBean_S2(2, "P2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{-1});

            env.milestone(6);

            env.sendEventBean(new SupportBean_S2(2, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{-1});

            env.milestone(7);

            env.sendEventBean(new SupportBean("P2", 20));

            env.milestone(8);

            env.sendEventBean(new SupportBean("P1", 21));

            env.milestone(9);

            env.sendEventBean(new SupportBean_S2(2, "P2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20});

            env.milestone(10);

            env.sendEventBean(new SupportBean_S2(2, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{21});

            // terminate context partitions
            env.sendEventBean(new SupportBean_S1(0, "P1"));
            env.sendEventBean(new SupportBean_S1(0, "P2"));

            env.milestone(11);

            env.sendEventBean(new SupportBean_S0(0, "P1"));    // allocate partition P1
            env.sendEventBean(new SupportBean_S2(1, "P1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5});

            env.undeployAll();

            // test module deployment and undeployment
            String epl = "@Name(\"context\")\n" +
                "create context MyContext\n" +
                "initiated by distinct(theString) SupportBean as input\n" +
                "terminated by SupportBean(theString = input.theString);\n" +
                "\n" +
                "@Name(\"ctx variable counter\")\n" +
                "context MyContext create variable integer counter = 0;\n";
            env.compileDeploy(epl).undeployAll();
        }
    }

    private static class ContextVariablesIterateAndListen implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyCtx as initiated by SupportBean_S0 s0 terminated after 24 hours", path);

            String[] fields = "mycontextvar".split(",");
            env.compileDeploy("@name('var') context MyCtx create variable int mycontextvar = 5", path);

            env.milestone(0);

            env.compileDeploy("@name('upd') context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive", path);
            env.addListener("var").addListener("upd");

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(0, "P1"));    // allocate partition P1

            env.milestone(2);

            env.sendEventBean(new SupportBean("P1", 100));    // update
            EPAssertionUtil.assertProps(env.listener("upd").assertOneGetNewAndReset(), fields, new Object[]{100});
            EPAssertionUtil.assertPropsPerRow(EPAssertionUtil.iteratorToArray(env.iterator("upd")), fields, new Object[][]{{100}});
            EPAssertionUtil.assertProps(env.listener("var").assertGetAndResetIRPair(), fields, new Object[]{100}, new Object[]{5});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(0, "P2"));    // allocate partition P1

            env.milestone(4);

            env.sendEventBean(new SupportBean("P2", 101));    // update
            EPAssertionUtil.assertProps(env.listener("upd").assertOneGetNewAndReset(), fields, new Object[]{101});
            EPAssertionUtil.assertPropsPerRow(EPAssertionUtil.iteratorToArray(env.iterator("upd")), fields, new Object[][]{{100}, {101}});

            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("var"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(events, fields, new Object[][]{{100}, {101}});
            EPAssertionUtil.assertProps(env.listener("var").assertGetAndResetIRPair(), fields, new Object[]{101}, new Object[]{5});

            env.undeployAll();
        }
    }

    private static class ContextVariablesGetSetAPI implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtx as initiated by SupportBean_S0 s0 terminated after 24 hours", path);
            env.compileDeploy("@name('var') context MyCtx create variable int mycontextvar = 5", path);
            env.compileDeploy("context MyCtx on SupportBean(theString = context.s0.p00) set mycontextvar = intPrimitive", path);
            DeploymentIdNamePair namePairVariable = new DeploymentIdNamePair(env.deploymentId("var"), "mycontextvar");

            env.sendEventBean(new SupportBean_S0(0, "P1"));    // allocate partition P1
            assertVariableValues(env, 0, 5);

            env.runtime().getVariableService().setVariableValue(Collections.singletonMap(namePairVariable, 10), 0);
            assertVariableValues(env, 0, 10);

            env.sendEventBean(new SupportBean_S0(0, "P2"));    // allocate partition P2
            assertVariableValues(env, 1, 5);

            env.runtime().getVariableService().setVariableValue(Collections.singletonMap(namePairVariable, 11), 1);
            assertVariableValues(env, 1, 11);

            // global variable - trying to set via context partition selection
            env.compileDeploy("@name('globalvar') create variable int myglobarvar = 0");
            DeploymentIdNamePair nameGlobalVar = new DeploymentIdNamePair(env.deploymentId("globalvar"), "myglobarvar");
            try {
                env.runtime().getVariableService().setVariableValue(Collections.singletonMap(nameGlobalVar, 11), 0);
                fail();
            } catch (VariableNotFoundException ex) {
                assertEquals("Variable by name 'myglobarvar' is a global variable and not context-partitioned", ex.getMessage());
            }

            // global variable - trying to get via context partition selection
            try {
                env.runtime().getVariableService().getVariableValue(Collections.singleton(nameGlobalVar), new SupportSelectorById(1));
                fail();
            } catch (VariableNotFoundException ex) {
                assertEquals("Variable by name 'myglobarvar' is a global variable and not context-partitioned", ex.getMessage());
            }

            env.undeployAll();
        }
    }

    private static class ContextVariablesInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyCtxOne as partition by theString from SupportBean", path);
            env.compileDeploy("create context MyCtxTwo as partition by p00 from SupportBean_S0", path);
            env.compileDeploy("context MyCtxOne create variable int myctxone_int = 0", path);

            // undefined context
            tryInvalidCompile(env, path, "context MyCtx create variable int mycontext_invalid1 = 0",
                "Context by name 'MyCtx' could not be found");

            // wrong context uses variable
            tryInvalidCompile(env, path, "context MyCtxTwo select myctxone_int from SupportBean_S0",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' is not available for use with context 'MyCtxTwo'");

            // variable use outside of context
            tryInvalidCompile(env, path, "select myctxone_int from SupportBean_S0",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");
            tryInvalidCompile(env, path, "select * from SupportBean_S0#expr(myctxone_int > 5)",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");
            tryInvalidCompile(env, path, "select * from SupportBean_S0#keepall limit myctxone_int",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");
            tryInvalidCompile(env, path, "select * from SupportBean_S0#keepall limit 10 offset myctxone_int",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");
            tryInvalidCompile(env, path, "select * from SupportBean_S0#keepall output every myctxone_int events",
                "Error in the output rate limiting clause: Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");
            tryInvalidCompile(env, path, "@Hint('reclaim_group_aged=myctxone_int') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Variable 'myctxone_int' defined for use with context 'MyCtxOne' can only be accessed within that context");

            env.undeployAll();
        }
    }

    private static void assertVariableValues(RegressionEnvironment env, int agentInstanceId, int expected) {
        DeploymentIdNamePair namePairVariable = new DeploymentIdNamePair(env.deploymentId("var"), "mycontextvar");
        Map<DeploymentIdNamePair, List<ContextPartitionVariableState>> states = env.runtime().getVariableService().getVariableValue(Collections.singleton(namePairVariable), new SupportSelectorById(agentInstanceId));
        assertEquals(1, states.size());
        List<ContextPartitionVariableState> list = states.get(namePairVariable);
        assertEquals(1, list.size());
        Assert.assertEquals(expected, list.get(0).getState());
    }
}
