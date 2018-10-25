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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOnSelect implements IndexBackingTableInfo {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraNamedWindowOnSelectSimple());
        execs.add(new InfraNamedWindowOnSelectSceneTwo());
        execs.add(new InfraNamedWindowOnSelectWPattern());
        return execs;
    }

    public static class InfraNamedWindowOnSelectSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            RegressionPath path = new RegressionPath();

            String eplCreate = "@Name('create') create window MyWindow.win:keepall() as SupportBean";
            env.compileDeploy(eplCreate, path).addListener("create");

            String eplInsert = "@Name('insert') insert into MyWindow select * from SupportBean";
            env.compileDeploy(eplInsert, path);

            String eplOnExpr = "@Name('delete') on SupportBean_S0 delete from MyWindow where intPrimitive = id";
            env.compileDeploy(eplOnExpr, path);

            env.milestone(0);

            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E1"});

            env.undeployAll();

            env.milestone(2);
        }
    }

    public static class InfraNamedWindowOnSelectSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportQueryPlanIndexHook.reset();
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();

            String epl = "@name('create') create window MyWindow#keepall as select * from SupportBean;\n" +
                "insert into MyWindow select * from SupportBean(theString like 'E%');\n" +
                "@name('select') on SupportBean_A insert into MyStream select mywin.* from MyWindow as mywin order by theString asc;\n" +
                "@name('consumer') select * from MyStream;\n" +
                "insert into MyStream select * from SupportBean(theString like 'I%');\n";
            env.compileDeploy(epl, path).addListener("select").addListener("consumer");
            assertEquals(StatementType.ON_INSERT, env.statement("select").getProperty(StatementProperty.STATEMENTTYPE));

            // send event
            sendSupportBean(env, "E1", 1);
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("consumer").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            // fire trigger
            sendSupportBean_A(env, "A1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(env.listener("consumer").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            // insert via 2nd insert into
            sendSupportBean(env, "I2", 2);
            assertFalse(env.listener("select").isInvoked());
            EPAssertionUtil.assertProps(env.listener("consumer").assertOneGetNewAndReset(), fields, new Object[]{"I2", 2});
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            // send event
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("consumer").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

            // fire trigger
            sendSupportBean_A(env, "A2");
            assertEquals(1, env.listener("select").getNewDataList().size());
            EPAssertionUtil.assertPropsPerRow(env.listener("select").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
            env.listener("select").reset();
            assertEquals(2, env.listener("consumer").getNewDataList().size());
            EPAssertionUtil.assertPropsPerRow(env.listener("consumer").getNewDataListFlattened(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
            env.listener("consumer").reset();

            // check type
            EventType consumerType = env.statement("consumer").getEventType();
            assertEquals(String.class, consumerType.getPropertyType("theString"));
            assertTrue(consumerType.getPropertyNames().length > 10);
            assertEquals(SupportBean.class, consumerType.getUnderlyingType());

            // check type
            EventType onSelectType = env.statement("select").getEventType();
            assertEquals(String.class, onSelectType.getPropertyType("theString"));
            assertTrue(onSelectType.getPropertyNames().length > 10);
            assertEquals(SupportBean.class, onSelectType.getUnderlyingType());

            // delete all from named window
            String stmtTextDelete = "@name('delete') on SupportBean_B delete from MyWindow";
            env.compileDeploy(stmtTextDelete, path);
            sendSupportBean_B(env, "B1");

            // fire trigger - nothing to insert
            sendSupportBean_A(env, "A3");

            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraNamedWindowOnSelectWPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow.win:keepall() as SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean(theString = 'Z')", path);
            env.sendEventBean(new SupportBean("Z", 0));

            String epl = "@Name('s0') on pattern[every e = SupportBean(theString = 'A') -> SupportBean(intPrimitive = e.intPrimitive)] select * from MyWindow";
            env.compileDeploy(epl, path).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("B", 1));
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean_B(RegressionEnvironment env, String id) {
        SupportBean_B bean = new SupportBean_B(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
