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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class InfraNWTableOnDelete {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraDeleteCondition(true));
        execs.add(new InfraDeleteCondition(false));

        execs.add(new InfraDeletePattern(true));
        execs.add(new InfraDeletePattern(false));

        execs.add(new InfraDeleteAll(true));
        execs.add(new InfraDeleteAll(false));

        return execs;
    }

    private static class InfraDeleteAll implements RegressionExecution {
        private final boolean namedWindow;

        public InfraDeleteAll(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // create window
            String stmtTextCreate = namedWindow ?
                "@Name('CreateInfra') create window MyInfra#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@Name('CreateInfra') create table MyInfra (a string primary key, b int)";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(stmtTextCreate, path).addListener("CreateInfra");

            // create delete stmt
            String stmtTextDelete = "@Name('OnDelete') on SupportBean_A delete from MyInfra";
            env.compileDeploy(stmtTextDelete, path).addListener("OnDelete");
            env.assertStatement("OnDelete", statement -> EPAssertionUtil.assertEqualsAnyOrder(statement.getEventType().getPropertyNames(), new String[]{"a", "b"}));

            // create insert into
            String stmtTextInsertOne = "@Name('Insert') insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // create consumer
            String[] fields = new String[]{"a", "b"};
            String stmtTextSelect = "@Name('Select') select irstream MyInfra.a as a, b from MyInfra as s1";
            env.compileDeploy(stmtTextSelect, path).addListener("Select");

            // Delete all events, no result expected
            sendSupportBean_A(env, "A1");
            env.assertListenerNotInvoked("CreateInfra");
            env.assertListenerNotInvoked("Select");
            env.assertListenerNotInvoked("OnDelete");
            env.assertThat(() -> assertEquals(0, getCount(env, path, "MyInfra")));

            // send 1 event
            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                env.assertPropsNew("CreateInfra", fields, new Object[]{"E1", 1});
                env.assertPropsNew("Select", fields, new Object[]{"E1", 1});
            } else {
                env.assertListenerNotInvoked("CreateInfra");
                env.assertListenerNotInvoked("Select");
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, new Object[][]{{"E1", 1}});
            env.assertPropsPerRowIterator("OnDelete", fields, null);
            env.assertThat(() -> assertEquals(1, getCount(env, path, "MyInfra")));

            env.milestone(0);

            // Delete all events, 1 row expected
            sendSupportBean_A(env, "A2");
            if (namedWindow) {
                env.assertPropsOld("CreateInfra", fields, new Object[]{"E1", 1});
                env.assertPropsOld("Select", fields, new Object[]{"E1", 1});
                env.assertPropsPerRowIterator("OnDelete", fields, new Object[0][]);
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, null);
            env.assertPropsNew("OnDelete", fields, new Object[]{"E1", 1});
            env.assertThat(() -> assertEquals(0, getCount(env, path, "MyInfra")));

            // send 2 events
            sendSupportBean(env, "E2", 2);
            sendSupportBean(env, "E3", 3);
            env.listenerReset("CreateInfra");
            env.assertPropsPerRowIterator("CreateInfra", fields, new Object[][]{{"E2", 2}, {"E3", 3}});
            env.assertListenerNotInvoked("OnDelete");
            env.assertThat(() -> assertEquals(2, getCount(env, path, "MyInfra")));

            env.milestone(1);

            // Delete all events, 2 rows expected
            sendSupportBean_A(env, "A2");
            if (namedWindow) {
                env.assertListener("CreateInfra", listener -> {
                    EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E2", 2});
                    EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"E3", 3});
                });
                env.assertPropsPerRowIterator("OnDelete", fields, new Object[0][]);
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, null);
            env.assertListener("OnDelete", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E2", 2});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"E3", 3});
            });
            env.assertThat(() -> assertEquals(0, getCount(env, path, "MyInfra")));

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "namedWindow=" + namedWindow +
                '}';
        }
    }

    private static class InfraDeletePattern implements RegressionExecution {
        private final boolean namedWindow;

        public InfraDeletePattern(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // create infra
            String stmtTextCreate = namedWindow ?
                "@name('CreateInfra') create window MyInfra#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('CreateInfra') create table MyInfra(a string primary key, b int)";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(stmtTextCreate, path).addListener("CreateInfra");

            // create delete stmt
            String stmtTextDelete = "@name('OnDelete') on pattern [every ea=SupportBean_A or every eb=SupportBean_B] delete from MyInfra";
            env.compileDeploy(stmtTextDelete, path).addListener("OnDelete");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 1 event
            String[] fields = new String[]{"a", "b"};
            sendSupportBean(env, "E1", 1);
            if (namedWindow) {
                env.assertPropsNew("CreateInfra", fields, new Object[]{"E1", 1});
                env.assertPropsPerRowIterator("OnDelete", fields, null);
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, new Object[][]{{"E1", 1}});
            env.assertThat(() -> assertEquals(1, getCount(env, path, "MyInfra")));

            // Delete all events using A, 1 row expected
            sendSupportBean_A(env, "A1");
            if (namedWindow) {
                env.assertPropsOld("CreateInfra", fields, new Object[]{"E1", 1});
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, null);
            env.assertPropsNew("OnDelete", fields, new Object[]{"E1", 1});
            env.assertThat(() -> assertEquals(0, getCount(env, path, "MyInfra")));

            env.milestone(0);

            // send 1 event
            sendSupportBean(env, "E2", 2);
            if (namedWindow) {
                env.assertPropsNew("CreateInfra", fields, new Object[]{"E2", 2});
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, new Object[][]{{"E2", 2}});
            env.assertThat(() -> assertEquals(1, getCount(env, path, "MyInfra")));

            // Delete all events using B, 1 row expected
            sendSupportBean_B(env, "B1");
            if (namedWindow) {
                env.assertPropsOld("CreateInfra", fields, new Object[]{"E2", 2});
            }
            env.assertPropsPerRowIterator("CreateInfra", fields, null);
            env.assertPropsNew("OnDelete", fields, new Object[]{"E2", 2});
            env.assertThat(() -> assertEquals(0, getCount(env, path, "MyInfra")));

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "namedWindow=" + namedWindow +
                '}';
        }
    }

    private static class InfraDeleteCondition implements RegressionExecution {
        private final boolean namedWindow;

        public InfraDeleteCondition(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // create infra
            String stmtTextCreate = namedWindow ?
                "@name('CreateInfra') create window MyInfra#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('CreateInfra') create table MyInfra (a string primary key, b int)";
            env.compileDeploy(stmtTextCreate, path).addListener("CreateInfra");

            // create delete stmt
            String stmtTextDelete = "on SupportBean_A delete from MyInfra where 'X' || a || 'X' = id";
            env.compileDeploy(stmtTextDelete, path);

            // create delete stmt
            stmtTextDelete = "on SupportBean_B delete from MyInfra where b < 5";
            env.compileDeploy(stmtTextDelete, path);

            // create insert into
            String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 3 event
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);

            env.milestone(0);

            sendSupportBean(env, "E3", 3);
            env.assertThat(() -> assertEquals(3, getCount(env, path, "MyInfra")));
            env.listenerReset("CreateInfra");
            String[] fields = new String[]{"a", "b"};
            env.assertPropsPerRowIterator("CreateInfra", fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            // delete E2
            sendSupportBean_A(env, "XE2X");
            if (namedWindow) {
                env.assertPropsOld("CreateInfra", fields, new Object[]{"E2", 2});
            }
            env.listenerReset("CreateInfra");
            env.assertPropsPerRowIteratorAnyOrder("CreateInfra", fields, new Object[][]{{"E1", 1}, {"E3", 3}});
            env.assertThat(() -> assertEquals(2, getCount(env, path, "MyInfra")));

            sendSupportBean(env, "E7", 7);
            env.assertPropsPerRowIteratorAnyOrder("CreateInfra", fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E7", 7}});
            env.assertThat(() -> assertEquals(3, getCount(env, path, "MyInfra")));

            env.milestone(1);

            // delete all under 5
            sendSupportBean_B(env, "B1");
            if (namedWindow) {
                env.assertListener("CreateInfra", listener -> {
                    assertEquals(2, listener.getLastOldData().length);
                    EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1});
                    EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"E3", 3});
                });
            }
            env.assertPropsPerRowIteratorAnyOrder("CreateInfra",  fields, new Object[][]{{"E7", 7}});
            env.assertThat(() -> assertEquals(1, getCount(env, path, "MyInfra")));

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "namedWindow=" + namedWindow +
                '}';
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

    private static long getCount(RegressionEnvironment env, RegressionPath path, String windowOrTableName) {
        return (Long) env.compileExecuteFAF("select count(*) as c0 from " + windowOrTableName, path).getArray()[0].get("c0");
    }
}
