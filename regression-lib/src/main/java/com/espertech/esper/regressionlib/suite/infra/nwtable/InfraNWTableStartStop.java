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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class InfraNWTableStartStop {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraStartStopConsumer(true));
        execs.add(new InfraStartStopConsumer(false));
        execs.add(new InfraStartStopInserter(true));
        execs.add(new InfraStartStopInserter(false));
        return execs;
    }

    private static class InfraStartStopInserter implements RegressionExecution {
        private final boolean namedWindow;

        public InfraStartStopInserter(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // create window
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table MyInfra(a string primary key, b int primary key)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsertOne = "@name('insert') insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // create consumer
            String[] fields = new String[]{"a", "b"};
            String stmtTextSelect = "@name('select') select a, b from MyInfra as s1";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            // send 1 event
            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
                EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            }
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            // stop inserter
            env.undeployModuleContaining("insert");

            sendSupportBean(env, "E2", 2);
            assertFalse(env.listener("create").isInvoked());
            assertFalse(env.listener("select").isInvoked());

            // start inserter
            env.compileDeploy(stmtTextInsertOne, path);

            // consumer receives the next event
            sendSupportBean(env, "E3", 3);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
                EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
                EPAssertionUtil.assertPropsPerRow(env.iterator("select"), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

            // destroy inserter
            env.undeployModuleContaining("insert");

            sendSupportBean(env, "E4", 4);
            assertFalse(env.listener("create").isInvoked());
            assertFalse(env.listener("select").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraStartStopConsumer implements RegressionExecution {
        private final boolean namedWindow;

        public InfraStartStopConsumer(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table MyInfra(a string primary key, b int primary key)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // create consumer
            String[] fields = new String[]{"a", "b"};
            String stmtTextSelect = "@Name('select') select a, b from MyInfra as s1";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            // send 1 event
            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
                EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            }
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            // stop consumer
            SupportListener selectListenerTemp = env.listener("select");
            env.undeployModuleContaining("select");
            sendSupportBean(env, "E2", 2);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            }
            assertFalse(selectListenerTemp.isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            // start consumer: the consumer has the last event even though he missed it
            env.compileDeploy(stmtTextSelect, path).addListener("select");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("select"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            // consumer receives the next event
            sendSupportBean(env, "E3", 3);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
                EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
                EPAssertionUtil.assertPropsPerRow(env.iterator("select"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            // destroy consumer
            selectListenerTemp = env.listener("select");
            env.undeployModuleContaining("select");
            sendSupportBean(env, "E4", 4);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E4", 4});
            }
            assertFalse(selectListenerTemp.isInvoked());

            env.undeployAll();
        }
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }
}
