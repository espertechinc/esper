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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.option.StatementUserObjectContext;
import com.espertech.esper.compiler.client.option.StatementUserObjectOption;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunction;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunctionTwo;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.fail;

public class ClientExtendSingleRowFunction {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendSRFEventBeanFootprint());
        execs.add(new ClientExtendSRFPropertyOrSingleRowMethod());
        execs.add(new ClientExtendSRFChainMethod());
        execs.add(new ClientExtendSRFSingleMethod());
        execs.add(new ClientExtendSRFFailedValidation());
        return execs;
    }

    private static class ClientExtendSRFEventBeanFootprint implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test select-clause
            String[] fields = new String[]{"c0", "c1"};
            String text = "@name('s0') select isNullValue(*, 'theString') as c0," +
                ClientExtendSingleRowFunction.class.getSimpleName() + ".localIsNullValue(*, 'theString') as c1 from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("a", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.sendEventBean(new SupportBean(null, 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});
            env.undeployAll();

            // test pattern
            String textPattern = "@name('s0') select * from pattern [a=SupportBean -> b=SupportBean(theString=getValueAsString(a, 'theString'))]";
            env.compileDeploy(textPattern).addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.intPrimitive,b.intPrimitive".split(","), new Object[]{1, 2});
            env.undeployAll();

            // test filter
            String textFilter = "@name('s0') select * from SupportBean('E1'=getValueAsString(*, 'theString'))";
            env.compileDeploy(textFilter).addListener("s0");
            env.sendEventBean(new SupportBean("E2", 1));
            env.sendEventBean(new SupportBean("E1", 2));
            Assert.assertEquals(1, env.listener("s0").getAndResetLastNewData().length);
            env.undeployAll();

            // test "first"
            String textAccessAgg = "@name('s0') select * from SupportBean#keepall having 'E2' = getValueAsString(last(*), 'theString')";
            env.compileDeploy(textAccessAgg).addListener("s0");
            env.sendEventBean(new SupportBean("E2", 1));
            env.sendEventBean(new SupportBean("E1", 2));
            Assert.assertEquals(1, env.listener("s0").getAndResetLastNewData().length);
            env.undeployAll();

            // test "window"
            String textWindowAgg = "@name('s0') select * from SupportBean#keepall having eventsCheckStrings(window(*), 'theString', 'E1')";
            env.compileDeploy(textWindowAgg).addListener("s0");
            env.sendEventBean(new SupportBean("E2", 1));
            env.sendEventBean(new SupportBean("E1", 2));
            Assert.assertEquals(1, env.listener("s0").getAndResetLastNewData().length);
            env.undeployAll();
        }
    }

    private static class ClientExtendSRFPropertyOrSingleRowMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select surroundx('test') as val from SupportBean";
            env.compileDeploy(text).addListener("s0");

            String[] fields = new String[]{"val"};
            env.sendEventBean(new SupportBean("a", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"XtestX"});

            env.undeployAll();
        }
    }

    private static class ClientExtendSRFChainMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select chainTop().chainValue(12,intPrimitive) as val from SupportBean";

            env.compileDeploy(text).addListener("s0");
            tryAssertionChainMethod(env);

            env.eplToModelCompileDeploy(text).addListener("s0");
            tryAssertionChainMethod(env);
        }
    }

    private static class ClientExtendSRFSingleMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select power3(intPrimitive) as val from SupportBean";

            env.compileDeploy(text).addListener("s0");
            tryAssertionSingleMethod(env);

            env.eplToModelCompileDeploy(text).addListener("s0");
            tryAssertionSingleMethod(env);

            text = "@name('s0') select power3(2) as val from SupportBean";
            env.compileDeploy(text).addListener("s0");
            tryAssertionSingleMethod(env);

            // test passing a context as well
            text = "@Name('s0') select power3Context(intPrimitive) as val from SupportBean";
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setStatementUserObject(new StatementUserObjectOption() {
                public Serializable getValue(StatementUserObjectContext env) {
                    return "my_user_object";
                }
            });
            EPCompiled compiled = env.compile(text, args);
            env.deploy(compiled).addListener("s0");

            SupportSingleRowFunction.getMethodInvocationContexts().clear();
            tryAssertionSingleMethod(env);
            EPLMethodInvocationContext context = SupportSingleRowFunction.getMethodInvocationContexts().get(0);
            Assert.assertEquals("s0", context.getStatementName());
            Assert.assertEquals(env.runtime().getURI(), context.getRuntimeURI());
            Assert.assertEquals(-1, context.getContextPartitionId());
            Assert.assertEquals("power3Context", context.getFunctionName());
            Assert.assertEquals("my_user_object", context.getStatementUserObject());

            env.undeployAll();

            // test exception behavior
            // logged-only
            env.compileDeploy("@name('s0') select throwExceptionLogMe() from SupportBean").addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));
            env.undeployAll();

            // rethrow
            env.compileDeploy("@Name('s0') select throwExceptionRethrow() from SupportBean").addListener("s0");
            try {
                env.sendEventBean(new SupportBean("E1", 1));
                fail();
            } catch (EPException ex) {
                Assert.assertEquals("java.lang.RuntimeException: Unexpected exception in statement 's0': Invocation exception when invoking method 'throwexception' of class '" + SupportSingleRowFunction.class.getName() + "' passing parameters [] for statement 's0': RuntimeException : This is a 'throwexception' generated exception", ex.getMessage());
                env.undeployAll();
            }

            // NPE when boxed is null
            env.compileDeploy("@Name('s0') select power3Rethrow(intBoxed) from SupportBean").addListener("s0");
            try {
                env.sendEventBean(new SupportBean("E1", 1));
                fail();
            } catch (EPException ex) {
                Assert.assertEquals("java.lang.RuntimeException: Unexpected exception in statement 's0': NullPointerException invoking method 'computePower3' of class '" + SupportSingleRowFunction.class.getName() + "' in parameter 0 passing parameters [null] for statement 's0': The method expects a primitive int value but received a null value", ex.getMessage());
            }

            env.undeployAll();
        }
    }

    private static class ClientExtendSRFFailedValidation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select singlerow('a', 'b') from SupportBean",
                "Failed to validate select-clause expression 'singlerow(\"a\",\"b\")': Could not find static method named 'testSingleRow' in class '" + SupportSingleRowFunctionTwo.class.getName() + "' with matching parameter number and expected parameter type(s) 'String, String' (nearest match found was 'testSingleRow' taking type(s) 'String, int')");
        }
    }

    private static void tryAssertionChainMethod(RegressionEnvironment env) {
        String[] fields = new String[]{"val"};
        env.sendEventBean(new SupportBean("a", 3));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{36});

        env.undeployAll();
    }

    private static void tryAssertionSingleMethod(RegressionEnvironment env) {
        String[] fields = new String[]{"val"};
        env.sendEventBean(new SupportBean("a", 2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{8});
        env.undeployAll();
    }

    public static boolean localIsNullValue(EventBean event, String propertyName) {
        return event.get(propertyName) == null;
    }
}
