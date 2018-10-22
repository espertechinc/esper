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
package com.espertech.esper.regressionlib.suite.client.deploy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.option.StatementUserObjectRuntimeContext;
import com.espertech.esper.runtime.client.option.StatementUserObjectRuntimeOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ClientDeployUserObject {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployUserObjectValues());
        execs.add(new ClientDeployUserObjectResolveContext());
        return execs;
    }

    private static class ClientDeployUserObjectResolveContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyUserObjectRuntimeOption.getContexts().clear();
            String epl = "@name('s0') select * from SupportBean";
            EPCompiled compiled = env.compile(epl);
            DeploymentOptions options = new DeploymentOptions();
            options.setStatementUserObjectRuntime(new MyUserObjectRuntimeOption());

            try {
                env.deployment().deploy(compiled, options);
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            StatementUserObjectRuntimeContext ctx = MyUserObjectRuntimeOption.getContexts().get(0);
            assertEquals("s0", ctx.getStatementName());
            assertEquals(env.deploymentId("s0"), ctx.getDeploymentId());
            assertSame(env.statement("s0").getAnnotations(), ctx.getAnnotations());
            assertEquals(epl, ctx.getEpl());

            env.undeployAll();
        }
    }

    private static class ClientDeployUserObjectValues implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('s0') select * from SupportBean");

            AtomicInteger milestone = new AtomicInteger();
            assertDeploy(env, compiled, milestone, null);
            assertDeploy(env, compiled, milestone, "ABC");
            assertDeploy(env, compiled, milestone, new MyUserObject("hello"));
        }
    }

    private static void assertDeploy(RegressionEnvironment env, EPCompiled compiled, AtomicInteger milestone, Object userObject) {
        DeploymentOptions options = new DeploymentOptions();
        options.setStatementUserObjectRuntime(new StatementUserObjectRuntimeOption() {
            public Object getUserObject(StatementUserObjectRuntimeContext env) {
                return userObject;
            }
        });

        try {
            env.deployment().deploy(compiled, options);
        } catch (EPDeployException e) {
            fail(e.getMessage());
        }
        env.undeployAll();
    }

    private static class MyUserObject implements Serializable {
        private final String id;

        public MyUserObject(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyUserObject that = (MyUserObject) o;

            return id.equals(that.id);
        }

        public int hashCode() {
            return id.hashCode();
        }
    }

    private static class MyUserObjectRuntimeOption implements StatementUserObjectRuntimeOption {
        private static List<StatementUserObjectRuntimeContext> contexts = new ArrayList<>();

        public static List<StatementUserObjectRuntimeContext> getContexts() {
            return contexts;
        }

        public Object getUserObject(StatementUserObjectRuntimeContext env) {
            return contexts.add(env);
        }
    }
}
