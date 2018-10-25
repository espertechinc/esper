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
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class InfraNWTableSubquery {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraSubquerySceneOne(true));
        execs.add(new InfraSubquerySceneOne(false));

        execs.add(new InfraSubquerySelfCheck(true));
        execs.add(new InfraSubquerySelfCheck(false));

        execs.add(new InfraSubqueryDeleteInsertReplace(true));
        execs.add(new InfraSubqueryDeleteInsertReplace(false));

        execs.add(new InfraInvalidSubquery(true));
        execs.add(new InfraInvalidSubquery(false));

        execs.add(new InfraUncorrelatedSubqueryAggregation(true));
        execs.add(new InfraUncorrelatedSubqueryAggregation(false));

        return execs;
    }

    public static class InfraSubquerySceneOne implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubquerySceneOne(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // create infra
            String stmtTextCreate = namedWindow ?
                "@Name('Create') create window MyInfra.win:keepall() as SupportBean" :
                "@Name('Create') create table MyInfra(theString string primary key, intPrimitive int)";
            env.compileDeploy(stmtTextCreate, path).addListener("Create");

            // create insert into
            String stmtTextInsertOne = "@Name('Insert') insert into MyInfra select theString, intPrimitive from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("B2", 2));
            env.sendEventBean(new SupportBean("C3", 3));

            // create subquery
            String stmtSubquery = "@Name('Subq') select (select intPrimitive from MyInfra where theString = s0.p00) as c0 from SupportBean_S0 as s0";
            env.compileDeploy(stmtSubquery, path).addListener("Subq");

            env.milestone(0);

            assertSubquery(env, "A1", 1);

            env.milestone(1);

            assertSubquery(env, "B2", 2);

            // cleanup
            env.undeployAll();
        }

        private void assertSubquery(RegressionEnvironment env, String p00, int expected) {
            env.sendEventBean(new SupportBean_S0(0, p00));
            EPAssertionUtil.assertProps(env.listener("Subq").assertOneGetNewAndReset(),
                "c0".split(","), new Object[]{expected});
        }
    }

    private static class InfraUncorrelatedSubqueryAggregation implements RegressionExecution {
        private final boolean namedWindow;

        public InfraUncorrelatedSubqueryAggregation(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraUCS#keepall as select theString as a, longPrimitive as b from SupportBean" :
                "@name('create') create table MyInfraUCS(a string primary key, b long)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfraUCS select theString as a, longPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // create consumer
            String stmtTextSelectOne = "select irstream (select sum(b) from MyInfraUCS) as value, symbol from SupportMarketDataBean";
            env.compileDeploy("@name('selectOne')" + stmtTextSelectOne, path).addListener("selectOne");

            sendMarketBean(env, "M1");
            String[] fieldsStmt = new String[]{"value", "symbol"};
            EPAssertionUtil.assertProps(env.listener("selectOne").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M1"});

            env.milestone(0);

            sendSupportBean(env, "S1", 5L, -1L);
            sendMarketBean(env, "M2");
            EPAssertionUtil.assertProps(env.listener("selectOne").assertOneGetNewAndReset(), fieldsStmt, new Object[]{5L, "M2"});

            sendSupportBean(env, "S2", 10L, -1L);
            sendMarketBean(env, "M3");
            EPAssertionUtil.assertProps(env.listener("selectOne").assertOneGetNewAndReset(), fieldsStmt, new Object[]{15L, "M3"});

            // create 2nd consumer
            env.compileDeploy("@name('selectTwo')" + stmtTextSelectOne, path).addListener("selectTwo"); // same stmt

            env.milestone(1);

            sendSupportBean(env, "S3", 8L, -1L);
            sendMarketBean(env, "M4");
            EPAssertionUtil.assertProps(env.listener("selectOne").assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{23L, "M4"});

            env.undeployAll();
        }
    }

    private static class InfraInvalidSubquery implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInvalidSubquery(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfraIS#keepall as SupportBean" :
                "create table MyInfraIS(theString string)";
            env.compileDeploy(eplCreate, path);

            try {
                env.compileWCheckedEx("select (select theString from MyInfraIS#lastevent) from MyInfraIS", path);
                fail();
            } catch (EPCompileException ex) {
                if (namedWindow) {
                    assertEquals("Failed to plan subquery number 1 querying MyInfraIS: Consuming statements to a named window cannot declare a data window view onto the named window [select (select theString from MyInfraIS#lastevent) from MyInfraIS]", ex.getMessage());
                } else {
                    SupportMessageAssertUtil.assertMessage(ex, "Views are not supported with tables");
                }
            }

            env.undeployAll();
        }
    }

    private static class InfraSubqueryDeleteInsertReplace implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubqueryDeleteInsertReplace(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall as select theString as key, intBoxed as value from SupportBean" :
                "@name('create') create table MyInfra(key string primary key, value int primary key)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // delete
            String stmtTextDelete = "@name('delete') on SupportBean delete from MyInfra where key = theString";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfra select theString as key, intBoxed as value from SupportBean as s0";
            env.compileDeploy(stmtTextInsertOne, path);

            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            }
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            env.milestone(0);

            sendSupportBean(env, "E2", 2);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
                EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
            } else {
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
            }

            sendSupportBean(env, "E1", 3);
            if (namedWindow) {
                assertEquals(2, env.listener("create").getNewDataList().size());
                EPAssertionUtil.assertProps(env.listener("create").getOldDataList().get(0)[0], fields, new Object[]{"E1", 1});
                EPAssertionUtil.assertProps(env.listener("create").getNewDataList().get(1)[0], fields, new Object[]{"E1", 3});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E2", 2}, {"E1", 3}});

            env.undeployAll();
        }
    }

    private static class InfraSubquerySelfCheck implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubquerySelfCheck(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraSSS#keepall as select theString as key, intBoxed as value from SupportBean" :
                "@name('create') create table MyInfraSSS (key string primary key, value int)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into (not does insert if key already exists)
            String stmtTextInsertOne = "insert into MyInfraSSS select theString as key, intBoxed as value from SupportBean as s0" +
                " where not exists (select * from MyInfraSSS as win where win.key = s0.theString)";
            env.compileDeploy(stmtTextInsertOne, path);

            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            } else {
                assertFalse(env.listener("create").isInvoked());
            }
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            sendSupportBean(env, "E2", 2);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            sendSupportBean(env, "E1", 3);
            assertFalse(env.listener("create").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.milestone(0);

            sendSupportBean(env, "E3", 4);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E3", 4});
                EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
            } else {
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
            }

            // Add delete
            String stmtTextDelete = "@name('delete') on SupportBean_A delete from MyInfraSSS where key = id";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // delete E2
            env.sendEventBean(new SupportBean_A("E2"));
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E3", 4}});

            env.milestone(1);

            sendSupportBean(env, "E2", 5);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 5});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E3", 4}, {"E2", 5}});

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, long longPrimitive, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(bean);
    }
}
