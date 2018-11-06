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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeCompileReflective;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClientRuntimeItself {
    public final static String TEST_SERVICE_NAME = "TEST_SERVICE_NAME";
    public final static int TEST_SECRET_VALUE = 12345;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeItselfTransientConfiguration());
        execs.add(new ClientRuntimeSPICompileReflective());
        execs.add(new ClientRuntimeWrongCompileMethod());
        return execs;
    }

    private static class ClientRuntimeWrongCompileMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window SomeWindow#keepall as SupportBean", path);

            EPCompiled compiledFAF = env.compileFAF("select * from SomeWindow", path);
            EPCompiled compiledModule = env.compile("select * from SomeWindow", path);

            try {
                env.runtime().getDeploymentService().deploy(compiledFAF);
                fail();
            } catch (EPDeployException ex) {
                assertEquals("Cannot deploy EPL that was compiled as a fire-and-forget query, make sure to use the 'compile' method of the compiler", ex.getMessage());
            }

            try {
                env.runtime().getFireAndForgetService().executeQuery(compiledModule);
                fail();
            } catch (EPException ex) {
                assertEquals("Cannot execute a fire-and-forget query that was compiled as module EPL, make sure to use the 'compileQuery' method of the compiler", ex.getMessage());
            }

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSPICompileReflective implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy(
                "@public create window MyWindow#keepall as SupportBean;\n" +
                    "insert into MyWindow select * from SupportBean;\n");
            env.sendEventBean(new SupportBean("E1", 10));

            EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
            EPRuntimeCompileReflective svc = spi.getReflectiveCompileSvc();
            assertTrue(svc.isCompilerAvailable());

            EPCompiled compiledFAF = svc.compileFireAndForget("select * from MyWindow");
            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiledFAF);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), new String[]{"theString"}, new Object[][]{{"E1"}});

            EPCompiled compiled = svc.compile("@name('s0') select * from MyWindow");
            env.deploy(compiled);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"theString"}, new Object[][]{{"E1"}});

            ExprNode node = svc.compileExpression("1*1", null, null);
            assertEquals(1, node.getForge().getExprEvaluator().evaluate(null, true, null));

            env.undeployAll();
        }
    }

    private static class ClientRuntimeItselfTransientConfiguration implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean");
            MyListener listener = new MyListener();
            env.statement("s0").addListener(listener);

            env.sendEventBean(new SupportBean());
            assertEquals(TEST_SECRET_VALUE, listener.getSecretValue());

            env.undeployAll();
        }
    }

    public static class MyLocalService {
        private final int secretValue;

        public MyLocalService(int secretValue) {
            this.secretValue = secretValue;
        }

        int getSecretValue() {
            return secretValue;
        }
    }

    public static class MyListener implements UpdateListener {
        private int secretValue;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            MyLocalService svc = (MyLocalService) runtime.getConfigurationTransient().get(TEST_SERVICE_NAME);
            secretValue = svc.getSecretValue();
        }

        int getSecretValue() {
            return secretValue;
        }
    }
}
