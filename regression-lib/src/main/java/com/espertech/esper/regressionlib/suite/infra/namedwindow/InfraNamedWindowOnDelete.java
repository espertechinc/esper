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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanTwo;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.util.SupportInfraUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOnDelete {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraFirstUnique());
        execs.add(new InfraStaggeredNamedWindow());
        execs.add(new InfraCoercionKeyMultiPropIndexes());
        execs.add(new InfraCoercionRangeMultiPropIndexes());
        execs.add(new InfraCoercionKeyAndRangeMultiPropIndexes());
        execs.add(new InfraNamedWindowSilentDeleteOnDelete());
        execs.add(new InfraNamedWindowSilentDeleteOnDeleteMany());
        return execs;
    }

    private static class InfraNamedWindowSilentDeleteOnDeleteMany implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('create') create window MyWindow#groupwin(theString)#length(2) as SupportBean;\n" +
                    "insert into MyWindow select * from SupportBean;\n" +
                    "@name('delete') @hint('silent_delete') on SupportBean_S0 delete from MyWindow;\n" +
                    "@name('count') select count(*) as cnt from MyWindow;\n";
            env.compileDeploy(epl).addListener("create").addListener("delete").addListener("count");

            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("A", 2));
            env.sendEventBean(new SupportBean("B", 3));
            env.sendEventBean(new SupportBean("B", 4));

            assertEquals(4L, env.listener("count").getAndResetDataListsFlattened().getFirst()[3].get("cnt"));
            env.listener("create").reset();

            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(0L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            EPAssertionUtil.assertPropsPerRow(env.listener("delete").getAndResetLastNewData(), "theString,intPrimitive".split(","), new Object[][]{
                new Object[]{"A", 1}, new Object[]{"A", 2}, new Object[]{"B", 3}, new Object[]{"B", 4}
            });
            assertFalse(env.listener("create").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraNamedWindowSilentDeleteOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('create') create window MyWindow#length(2) as SupportBean;\n" +
                    "insert into MyWindow select * from SupportBean;\n" +
                    "@name('delete') @hint('silent_delete') on SupportBean_S0 delete from MyWindow where p00 = theString;\n" +
                    "@name('count') select count(*) as cnt from MyWindow;\n";
            env.compileDeploy(epl).addListener("create").addListener("delete").addListener("count");

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(1L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E1", env.listener("create").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean_S0(0, "E1"));
            assertEquals(0L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E1", env.listener("delete").assertOneGetNewAndReset().get("theString"));
            assertFalse(env.listener("create").isInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            assertEquals(1L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E2", env.listener("create").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E3", 3));
            assertEquals(2L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E3", env.listener("create").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E4", 4));
            assertEquals(2L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            EPAssertionUtil.assertProps(env.listener("create").assertPairGetIRAndReset(), "theString".split(","), new Object[]{"E4"}, new Object[]{"E2"});

            env.sendEventBean(new SupportBean_S0(0, "E4"));
            assertEquals(1L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E4", env.listener("delete").assertOneGetNewAndReset().get("theString"));
            assertFalse(env.listener("create").isInvoked());

            env.sendEventBean(new SupportBean_S0(0, "E3"));
            assertEquals(0L, env.listener("count").assertOneGetNewAndReset().get("cnt"));
            assertEquals("E3", env.listener("delete").assertOneGetNewAndReset().get("theString"));
            assertFalse(env.listener("create").isInvoked());

            env.sendEventBean(new SupportBean_S0(0, "EX"));
            assertFalse(env.listener("count").isInvoked());
            assertFalse(env.listener("delete").isInvoked());
            assertFalse(env.listener("create").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraFirstUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"theString", "intPrimitive"};
            String epl = "@name('create') create window MyWindowFU#firstunique(theString) as select * from SupportBean;\n" +
                "insert into MyWindowFU select * from SupportBean;\n" +
                "@name('delete') on SupportBean_A a delete from MyWindowFU where theString=a.id;\n";
            env.compileDeploy(epl).addListener("delete");

            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("A", 2));

            env.sendEventBean(new SupportBean_A("A"));
            EPAssertionUtil.assertProps(env.listener("delete").assertOneGetNewAndReset(), fields, new Object[]{"A", 1});

            env.sendEventBean(new SupportBean("A", 3));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"A", 3}});

            env.sendEventBean(new SupportBean_A("A"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, null);

            env.undeployAll();
        }
    }

    private static class InfraStaggeredNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionStaggered(env, rep);
            }
        }
    }

    private static class InfraCoercionKeyMultiPropIndexes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window
            String stmtTextCreate = "@name('createOne') create window MyWindowCK#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("createOne");

            List<String> deleteStatements = new LinkedList<>();
            String stmtTextDelete = "@name('d1') on SupportBean(theString='DB') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.doubleBoxed";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d1");
            assertEquals(1, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d2') on SupportBean(theString='DP') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.doublePrimitive";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d2");
            assertEquals(1, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d3') on SupportBean(theString='IB') as s0 delete from MyWindowCK where MyWindowCK.intPrimitive = s0.intBoxed";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d3");
            assertEquals(2, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d4') on SupportBean(theString='IPDP') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d4");
            assertEquals(3, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d5') on SupportBean(theString='IPDP2') as s0 delete from MyWindowCK as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d5");
            assertEquals(4, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d6') on SupportBean(theString='IPDPIB') as s0 delete from MyWindowCK as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive and win.intBoxed = s0.intBoxed";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d6");
            assertEquals(5, getIndexCount(env, "createOne", "MyWindowCK"));

            stmtTextDelete = "@name('d7') on SupportBean(theString='CAST') as s0 delete from MyWindowCK as win where win.intBoxed = s0.intPrimitive and win.doublePrimitive = s0.doubleBoxed and win.intPrimitive = s0.intBoxed";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d7");
            assertEquals(6, getIndexCount(env, "createOne", "MyWindowCK"));

            // create insert into
            String stmtTextInsertOne = "insert into MyWindowCK select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed "
                + "from SupportBean(theString like 'E%')";
            env.compileDeploy(stmtTextInsertOne, path);

            sendSupportBean(env, "E1", 1, 10, 100d, 1000d);
            sendSupportBean(env, "E2", 2, 20, 200d, 2000d);
            sendSupportBean(env, "E3", 3, 30, 300d, 3000d);
            sendSupportBean(env, "E4", 4, 40, 400d, 4000d);
            env.listener("createOne").reset();

            String[] fields = new String[]{"theString"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            sendSupportBean(env, "DB", 0, 0, 0d, null);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBean(env, "DB", 0, 0, 0d, 3d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E1"}, {"E2"}, {"E4"}});

            sendSupportBean(env, "DP", 0, 0, 5d, null);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBean(env, "DP", 0, 0, 4d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E4"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E1"}, {"E2"}});

            sendSupportBean(env, "IB", 0, -1, 0d, null);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBean(env, "IB", 0, 1, 0d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}});

            sendSupportBean(env, "E5", 5, 50, 500d, 5000d);
            sendSupportBean(env, "E6", 6, 60, 600d, 6000d);
            sendSupportBean(env, "E7", 7, 70, 700d, 7000d);
            env.listener("createOne").reset();

            sendSupportBean(env, "IPDP", 5, 0, 500d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E5"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}, {"E6"}, {"E7"}});

            sendSupportBean(env, "IPDP2", 6, 0, 600d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E6"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}, {"E7"}});

            sendSupportBean(env, "IPDPIB", 7, 70, 0d, null);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBean(env, "IPDPIB", 7, 70, 700d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E7"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}});

            sendSupportBean(env, "E8", 8, 80, 800d, 8000d);
            env.listener("createOne").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}, {"E8"}});

            sendSupportBean(env, "CAST", 80, 8, 0, 800d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E8"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, new Object[][]{{"E2"}});

            for (String stmtName : deleteStatements) {
                env.undeployModuleContaining(stmtName);
            }
            deleteStatements.clear();

            // late delete on a filled window
            stmtTextDelete = "@name('d0') on SupportBean(theString='LAST') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d0");
            sendSupportBean(env, "LAST", 2, 20, 200, 2000d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fields, null);

            for (String stmtName : deleteStatements) {
                env.undeployModuleContaining(stmtName);
            }
            assertEquals(0, getIndexCount(env, "createOne", "MyWindowCK"));
            env.undeployAll();

            // test single-two-field index reuse
            path = new RegressionPath();
            env.compileDeploy("@name('createTwo') create window WinOne#keepall as SupportBean", path);
            env.compileDeploy("on SupportBean_ST0 select * from WinOne where theString = key0", path);
            assertEquals(1, getIndexCount(env, "createTwo", "WinOne"));

            env.compileDeploy("on SupportBean_ST0 select * from WinOne where theString = key0 and intPrimitive = p00", path);
            assertEquals(2, getIndexCount(env, "createTwo", "WinOne"));

            env.undeployAll();
        }
    }

    private static class InfraCoercionRangeMultiPropIndexes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String stmtTextCreate = "@name('createOne') create window MyWindowCR#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("createOne");

            String stmtText = "insert into MyWindowCR select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
            env.compileDeploy(stmtText, path);
            String[] fields = new String[]{"theString"};

            sendSupportBean(env, "E1", 1, 10, 100d, 1000d);
            sendSupportBean(env, "E2", 2, 20, 200d, 2000d);
            sendSupportBean(env, "E3", 3, 30, 3d, 30d);
            sendSupportBean(env, "E4", 4, 40, 4d, 40d);
            sendSupportBean(env, "E5", 5, 50, 500d, 5000d);
            sendSupportBean(env, "E6", 6, 60, 600d, 6000d);
            env.listener("createOne").reset();

            List<String> deleteStatements = new LinkedList<>();
            String stmtTextDelete = "@name('d0') on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive between s2.doublePrimitiveTwo and s2.doubleBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d0");
            assertEquals(1, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", 0, 0, 0d, null);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBeanTwo(env, "T", 0, 0, -1d, 1d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E1"});

            stmtTextDelete = "@name('d1') on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d1");
            assertEquals(2, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", -2, 2, 0d, 0d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E2"});

            stmtTextDelete = "@name('d2') on SupportBeanTwo as s2 delete from MyWindowCR as win " +
                "where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo and win.doublePrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d2");
            assertEquals(3, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", -3, 3, -3d, 3d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E3"});

            stmtTextDelete = "@name('d3') on SupportBeanTwo as s2 delete from MyWindowCR as win " +
                "where win.doublePrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo and win.intPrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d3");
            assertEquals(4, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", -4, 4, -4, 4d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E4"});

            stmtTextDelete = "@name('d4') on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive <= doublePrimitiveTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d4");
            assertEquals(4, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", 0, 0, 5, 1d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E5"});

            stmtTextDelete = "@name('d5') on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive not between s2.intPrimitiveTwo and s2.intBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d5");
            assertEquals(4, getIndexCount(env, "createOne", "MyWindowCR"));

            sendSupportBeanTwo(env, "T", 100, 200, 0, 0d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E6"});

            // delete
            for (String stmtName : deleteStatements) {
                env.undeployModuleContaining(stmtName);
            }
            deleteStatements.clear();
            assertEquals(0, getIndexCount(env, "createOne", "MyWindowCR"));

            env.undeployAll();
        }
    }

    private static class InfraCoercionKeyAndRangeMultiPropIndexes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = "@name('createOne') create window MyWindowCKR#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("createOne");

            String stmtText = "insert into MyWindowCKR select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
            env.compileDeploy(stmtText, path);
            String[] fields = new String[]{"theString"};

            sendSupportBean(env, "E1", 1, 10, 100d, 1000d);
            sendSupportBean(env, "E2", 2, 20, 200d, 2000d);
            sendSupportBean(env, "E3", 3, 30, 300d, 3000d);
            sendSupportBean(env, "E4", 4, 40, 400d, 4000d);
            env.listener("createOne").reset();

            List<String> deleteStatements = new LinkedList<>();
            String stmtTextDelete = "@name('d0') on SupportBeanTwo delete from MyWindowCKR where theString = stringTwo and intPrimitive between doublePrimitiveTwo and doubleBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d0");
            assertEquals(1, getIndexCount(env, "createOne", "MyWindowCKR"));

            sendSupportBeanTwo(env, "T", 0, 0, 1d, 200d);
            assertFalse(env.listener("createOne").isInvoked());
            sendSupportBeanTwo(env, "E1", 0, 0, 1d, 200d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E1"});

            stmtTextDelete = "@name('d1') on SupportBeanTwo delete from MyWindowCKR where theString = stringTwo and intPrimitive = intPrimitiveTwo and intBoxed between doublePrimitiveTwo and doubleBoxedTwo";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d1");
            assertEquals(2, getIndexCount(env, "createOne", "MyWindowCKR"));

            sendSupportBeanTwo(env, "E2", 2, 0, 19d, 21d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E2"});

            stmtTextDelete = "@name('d2') on SupportBeanTwo delete from MyWindowCKR where intBoxed between doubleBoxedTwo and doublePrimitiveTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d2");
            assertEquals(3, getIndexCount(env, "createOne", "MyWindowCKR"));

            sendSupportBeanTwo(env, "E3", 3, 0, 29d, 34d);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E3"});

            stmtTextDelete = "@name('d3') on SupportBeanTwo delete from MyWindowCKR where intBoxed between intBoxedTwo and intBoxedTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
            env.compileDeploy(stmtTextDelete, path);
            deleteStatements.add("d3");
            assertEquals(4, getIndexCount(env, "createOne", "MyWindowCKR"));

            sendSupportBeanTwo(env, "E4", 4, 40, 0d, null);
            EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetOldAndReset(), fields, new Object[]{"E4"});

            // delete
            for (String stmtName : deleteStatements) {
                env.undeployModuleContaining(stmtName);
            }
            deleteStatements.clear();
            assertEquals(0, getIndexCount(env, "createOne", "MyWindowCKR"));

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, Integer intBoxed,
                                        double doublePrimitive, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(bean);
    }

    private static void sendSupportBeanTwo(RegressionEnvironment env, String theString, int intPrimitive, Integer intBoxed,
                                           double doublePrimitive, Double doubleBoxed) {
        SupportBeanTwo bean = new SupportBeanTwo();
        bean.setStringTwo(theString);
        bean.setIntPrimitiveTwo(intPrimitive);
        bean.setIntBoxedTwo(intBoxed);
        bean.setDoublePrimitiveTwo(doublePrimitive);
        bean.setDoubleBoxedTwo(doubleBoxed);
        env.sendEventBean(bean);
    }

    private static void tryAssertionStaggered(RegressionEnvironment env, EventRepresentationChoice outputType) {

        String[] fieldsOne = new String[]{"a1", "b1"};
        String[] fieldsTwo = new String[]{"a2", "b2"};
        RegressionPath path = new RegressionPath();

        // create window one
        String stmtTextCreateOne = outputType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSTAG.class) + "@name('createOne') create window MyWindowSTAG#keepall as select theString as a1, intPrimitive as b1 from SupportBean";
        env.compileDeploy(stmtTextCreateOne, path).addListener("createOne");
        assertEquals(0, getCount(env, "createOne", "MyWindowSTAG"));
        assertTrue(outputType.matchesClass(env.statement("createOne").getEventType().getUnderlyingType()));

        // create window two
        String stmtTextCreateTwo = outputType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSTAGTwo.class) + " @name('createTwo') create window MyWindowSTAGTwo#keepall as select theString as a2, intPrimitive as b2 from SupportBean";
        env.compileDeploy(stmtTextCreateTwo, path).addListener("createTwo");
        assertEquals(0, getCount(env, "createTwo", "MyWindowSTAGTwo"));
        assertTrue(outputType.matchesClass(env.statement("createTwo").getEventType().getUnderlyingType()));

        // create delete stmt
        String stmtTextDelete = "@name('delete') on MyWindowSTAG delete from MyWindowSTAGTwo where a1 = a2";
        env.compileDeploy(stmtTextDelete, path).addListener("delete");
        assertEquals(StatementType.ON_DELETE, env.statement("delete").getProperty(StatementProperty.STATEMENTTYPE));

        // create insert into
        String stmtTextInsert = "@name('insert') insert into MyWindowSTAG select theString as a1, intPrimitive as b1 from SupportBean(intPrimitive > 0)";
        env.compileDeploy(stmtTextInsert, path);
        stmtTextInsert = "@name('insertTwo') insert into MyWindowSTAGTwo select theString as a2, intPrimitive as b2 from SupportBean(intPrimitive < 0)";
        env.compileDeploy(stmtTextInsert, path);

        sendSupportBean(env, "E1", -10);
        EPAssertionUtil.assertProps(env.listener("createTwo").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E1", -10});
        EPAssertionUtil.assertPropsPerRow(env.iterator("createTwo"), fieldsTwo, new Object[][]{{"E1", -10}});
        assertFalse(env.listener("createOne").isInvoked());
        assertEquals(1, getCount(env, "createTwo", "MyWindowSTAGTwo"));

        sendSupportBean(env, "E2", 5);
        EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetNewAndReset(), fieldsOne, new Object[]{"E2", 5});
        EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fieldsOne, new Object[][]{{"E2", 5}});
        assertFalse(env.listener("createTwo").isInvoked());
        assertEquals(1, getCount(env, "createOne", "MyWindowSTAG"));

        sendSupportBean(env, "E3", -1);
        EPAssertionUtil.assertProps(env.listener("createTwo").assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(env.iterator("createTwo"), fieldsTwo, new Object[][]{{"E1", -10}, {"E3", -1}});
        assertFalse(env.listener("createOne").isInvoked());
        assertEquals(2, getCount(env, "createTwo", "MyWindowSTAGTwo"));

        sendSupportBean(env, "E3", 1);
        EPAssertionUtil.assertProps(env.listener("createOne").assertOneGetNewAndReset(), fieldsOne, new Object[]{"E3", 1});
        EPAssertionUtil.assertPropsPerRow(env.iterator("createOne"), fieldsOne, new Object[][]{{"E2", 5}, {"E3", 1}});
        EPAssertionUtil.assertProps(env.listener("createTwo").assertOneGetOldAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(env.iterator("createTwo"), fieldsTwo, new Object[][]{{"E1", -10}});
        assertEquals(2, getCount(env, "createOne", "MyWindowSTAG"));
        assertEquals(1, getCount(env, "createTwo", "MyWindowSTAGTwo"));

        env.undeployModuleContaining("delete");
        env.undeployModuleContaining("insert");
        env.undeployModuleContaining("insertTwo");
        env.undeployModuleContaining("createOne");
        env.undeployModuleContaining("createTwo");
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static long getCount(RegressionEnvironment env, String statementName, String windowName) {
        return SupportInfraUtil.getDataWindowCountNoContext(env, statementName, windowName);
    }

    private static int getIndexCount(RegressionEnvironment env, String statementName, String windowName) {
        return SupportInfraUtil.getIndexCountNoContext(env, true, statementName, windowName);
    }

    public static class MyLocalJsonProvidedSTAG implements Serializable {
        public String a1;
        public int b1;
    }

    public static class MyLocalJsonProvidedSTAGTwo implements Serializable {
        public String a2;
        public int b2;
    }
}
