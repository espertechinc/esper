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
package com.espertech.esper.regressionlib.suite.epl.variable;

import com.espertech.esper.common.client.variable.VariableNotFoundException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.EPUndeployPreconditionException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLVariablesDestroy {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableManageDependency());
        execs.add(new EPLVariableDestroyReCreateChangeType());
        return execs;
    }

    private static class EPLVariableDestroyReCreateChangeType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@Name('ABC') create variable long varDRR = 2";
            env.compileDeploy(text);

            assertEquals(2L, env.runtime().getVariableService().getVariableValue(env.deploymentId("ABC"), "varDRR"));

            String deploymentIdABC = env.deploymentId("ABC");
            env.undeployModuleContaining("ABC");

            assertNotFound(env, deploymentIdABC, "varDRR");

            text = "@Name('CDE') create variable string varDRR = 'a'";
            env.compileDeploy(text);

            assertEquals("a", env.runtime().getVariableService().getVariableValue(env.deploymentId("CDE"), "varDRR"));

            String deploymentIdCDE = env.deploymentId("CDE");
            env.undeployModuleContaining("CDE");
            assertNotFound(env, deploymentIdCDE, "varDRR");
        }
    }

    private static class EPLVariableManageDependency implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // single variable
            env.compileDeploy("@Name('S0') create variable boolean var2vmd = true", path);
            env.compileDeploy("@Name('S1') select * from SupportBean(var2vmd)", path);
            assertEquals(true, env.runtime().getVariableService().getVariableValue(env.deploymentId("S0"), "var2vmd"));

            try {
                env.deployment().undeploy(env.deploymentId("S0"));
                fail();
            } catch (EPUndeployException ex) {
                // expected
            }

            env.undeployModuleContaining("S1");
            assertEquals(true, env.runtime().getVariableService().getVariableValue(env.deploymentId("S0"), "var2vmd"));

            String deploymentIdS0 = env.deploymentId("S0");
            env.undeployModuleContaining("S0");
            assertNotFound(env, deploymentIdS0, "var2vmd");

            // multiple variable
            path.clear();
            env.compileDeploy("@Name('T0') create variable boolean v1 = true", path);
            env.compileDeploy("@Name('T1') create variable long v2 = 1", path);
            env.compileDeploy("@Name('T2') create variable string v3 = 'a'", path);
            env.compileDeploy("@Name('TX') select * from SupportBean(v1, v2=1, v3='a')", path);
            env.compileDeploy("@Name('TY') select * from SupportBean(v2=2)", path);
            env.compileDeploy("@Name('TZ') select * from SupportBean(v3='A', v1)", path);

            assertCannotUndeploy(env, "T0,T1,T2");
            env.undeployModuleContaining("TX");
            assertCannotUndeploy(env, "T0,T1,T2");

            env.undeployModuleContaining("TY");
            env.undeployModuleContaining("T1");
            assertCannotUndeploy(env, "T0,T2");

            env.undeployModuleContaining("TZ");
            env.undeployModuleContaining("T0");
            env.undeployModuleContaining("T2");

            env.undeployAll();
        }
    }

    private static void assertNotFound(RegressionEnvironment env, String deploymentId, String var) {
        try {
            env.runtime().getVariableService().getVariableValue(deploymentId, var);
            fail();
        } catch (VariableNotFoundException ex) {
            // expected
        }
    }

    private static void assertCannotUndeploy(RegressionEnvironment env, String statementNames) {
        String[] names = statementNames.split(",");
        for (String name : names) {
            try {
                env.deployment().undeploy(env.deploymentId(name));
                fail();
            } catch (EPUndeployPreconditionException ex) {
                // expected
            } catch (EPUndeployException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}