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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClientMultitenancyProtected {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientMultitenancyProtectedInfra(true));
        execs.add(new ClientMultitenancyProtectedInfra(false));
        execs.add(new ClientMultitenancyProtectedVariable());
        execs.add(new ClientMultitenancyProtectedContext());
        execs.add(new ClientMultitenancyProtectedEventType());
        execs.add(new ClientMultitenancyProtectedExpr());
        return execs;
    }

    private static class ClientMultitenancyProtectedInfra implements RegressionExecution {
        private final boolean namedWindow;

        public ClientMultitenancyProtectedInfra(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String eplInfra = namedWindow ?
                "@name('create') create window MyInfra#keepall as (col1 string, myident string);\n" :
                "@name('create') create table MyInfra(col1 string primary key, myident string);\n";
            String epl = eplInfra +
                "insert into MyInfra select theString as col1, $X as myident from SupportBean;\n";
            String idOne = env.deployGetId(env.compile(epl.replace("$X", "'A'")));
            String idTwo = env.deployGetId(env.compile(epl.replace("$X", "'B'")));

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 0));
            assertRowsNamedWindow(env, idOne, "A");
            assertRowsNamedWindow(env, idTwo, "B");

            env.undeploy(idOne);
            assertRowsNamedWindow(env, idTwo, "B");
            assertNull(env.runtime().getDeploymentService().getStatement(idOne, "create"));

            env.undeploy(idTwo);
            assertNull(env.runtime().getDeploymentService().getStatement(idTwo, "create"));
        }
    }

    private static class ClientMultitenancyProtectedVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create variable int myvar = $X;\n" +
                "on pattern[every timer:interval(10)] set myvar = myvar + 1;\n";
            env.advanceTime(0);
            String idOne = env.deployGetId(env.compile(epl.replace("$X", "10")));
            String idTwo = env.deployGetId(env.compile(epl.replace("$X", "20")));

            assertVariable(env, idOne, 10);
            assertVariable(env, idTwo, 20);

            env.advanceTime(10000);

            assertVariable(env, idOne, 11);
            assertVariable(env, idTwo, 21);

            env.undeploy(idOne);

            env.advanceTime(20000);

            assertNull(env.runtime().getDeploymentService().getStatement(idOne, "create"));
            assertVariable(env, idTwo, 22);

            env.undeploy(idTwo);
            assertNull(env.runtime().getDeploymentService().getStatement(idTwo, "create"));
        }
    }

    private static class ClientMultitenancyProtectedContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create context MyContext start SupportBean(theString=$X) as sb end after 1 year;\n" +
                "@name('s0') context MyContext select count(*) as cnt from SupportBean;\n";
            String idOne = env.deployGetId(env.compile(epl.replace("$X", "'A'")));
            String idTwo = env.deployGetId(env.compile(epl.replace("$X", "'B'")));

            assertContextNoRow(env, idOne);
            assertContextNoRow(env, idTwo);

            env.sendEventBean(new SupportBean("B", 0));

            assertContextNoRow(env, idOne);
            assertContext(env, idTwo, 1);

            env.sendEventBean(new SupportBean("A", 0));
            env.sendEventBean(new SupportBean("A", 0));

            assertContext(env, idOne, 2);
            assertContext(env, idTwo, 3);

            env.undeploy(idOne);

            env.sendEventBean(new SupportBean("X", 0));
            assertContext(env, idTwo, 4);

            env.undeploy(idTwo);
        }
    }

    private static class ClientMultitenancyProtectedEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "create schema MySchema as (col1 string);\n" +
                "insert into MySchema select theString as col1 from SupportBean;\n" +
                "@name('s0') select count(*) as c0 from MySchema;\n";
            String idOne = env.deployGetId(env.compile(eplOne));

            String eplTwo = "create schema MySchema as (totalme int);\n" +
                "insert into MySchema select intPrimitive as totalme from SupportBean;\n" +
                "@name('s0') select sum(totalme) as c0 from MySchema;\n";
            String idTwo = env.deployGetId(env.compile(eplTwo));

            assertSelect(env, idOne, 0L);
            assertSelect(env, idTwo, null);

            env.sendEventBean(new SupportBean("E1", 10));

            assertSelect(env, idOne, 1L);
            assertSelect(env, idTwo, 10);

            env.sendEventBean(new SupportBean("E2", 20));

            assertSelect(env, idOne, 2L);
            assertSelect(env, idTwo, 30);

            env.undeploy(idOne);
            assertNull(env.runtime().getDeploymentService().getStatement(idOne, "s0"));

            env.sendEventBean(new SupportBean("E3", 30));

            assertSelect(env, idTwo, 60);

            env.undeploy(idTwo);
        }
    }

    private static class ClientMultitenancyProtectedExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "create expression my_expression { 1 } ;\n" +
                "@name('s0') select my_expression as c0 from SupportBean#lastevent;\n";
            String idOne = env.deployGetId(env.compile(eplOne));

            String eplTwo = "create expression my_expression { 2 } ;\n" +
                "@name('s0') select my_expression as c0 from SupportBean#lastevent;\n";
            String idTwo = env.deployGetId(env.compile(eplTwo));
            env.sendEventBean(new SupportBean());

            assertSelect(env, idOne, 1);
            assertSelect(env, idTwo, 2);

            env.undeploy(idOne);
            assertNull(env.runtime().getDeploymentService().getStatement(idOne, "s0"));

            assertSelect(env, idTwo, 2);

            env.undeploy(idTwo);
        }
    }

    private static void assertSelect(RegressionEnvironment env, String deploymentId, Object expected) {
        assertEquals(expected, env.runtime().getDeploymentService().getStatement(deploymentId, "s0").iterator().next().get("c0"));
    }

    private static void assertContextNoRow(RegressionEnvironment env, String deploymentId) {
        assertFalse(env.runtime().getDeploymentService().getStatement(deploymentId, "s0").iterator().hasNext());
    }

    private static void assertContext(RegressionEnvironment env, String deploymentId, long expected) {
        assertEquals(expected, env.runtime().getDeploymentService().getStatement(deploymentId, "s0").iterator().next().get("cnt"));
    }

    private static void assertVariable(RegressionEnvironment env, String deploymentId, int expected) {
        assertEquals(expected, env.runtime().getDeploymentService().getStatement(deploymentId, "create").iterator().next().get("myvar"));
    }

    private static void assertRowsNamedWindow(RegressionEnvironment env, String deploymentId, String ident) {
        EPAssertionUtil.assertPropsPerRow(env.runtime().getDeploymentService().getStatement(deploymentId, "create").iterator(),
            "col1,myident".split(","), new Object[][]{{"E1", ident}, {"E2", ident}});
    }
}
