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
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InfraNWTableSubqUncorrel {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        // named window tests
        execs.add(new InfraNWTableSubqUncorrelAssertion(true, false, false)); // testNoShare
        execs.add(new InfraNWTableSubqUncorrelAssertion(true, true, false)); // testShare
        execs.add(new InfraNWTableSubqUncorrelAssertion(true, true, true)); // testDisableShare

        // table tests
        execs.add(new InfraNWTableSubqUncorrelAssertion(false, false, false));
        return execs;
    }

    private static class InfraNWTableSubqUncorrelAssertion implements RegressionExecution {
        private final boolean namedWindow;
        private final boolean enableIndexShareCreate;
        private final boolean disableIndexShareConsumer;

        public InfraNWTableSubqUncorrelAssertion(boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer) {
            this.namedWindow = namedWindow;
            this.enableIndexShareCreate = enableIndexShareCreate;
            this.disableIndexShareConsumer = disableIndexShareConsumer;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall as select theString as a, longPrimitive as b, longBoxed as c from SupportBean" :
                "@name('create') create table MyInfra(a string primary key, b long, c long)";
            if (enableIndexShareCreate) {
                stmtTextCreate = "@Hint('enable_window_subquery_indexshare') " + stmtTextCreate;
            }
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfra select theString as a, longPrimitive as b, longBoxed as c from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // create consumer
            String stmtTextSelectOne = "@name('select') select irstream (select a from MyInfra) as value, symbol from SupportMarketDataBean";
            if (disableIndexShareConsumer) {
                stmtTextSelectOne = "@Hint('disable_window_subquery_indexshare') " + stmtTextSelectOne;
            }
            env.compileDeploy(stmtTextSelectOne, path).addListener("select");
            EPAssertionUtil.assertEqualsAnyOrder(env.statement("select").getEventType().getPropertyNames(), new String[]{"value", "symbol"});
            assertEquals(String.class, env.statement("select").getEventType().getPropertyType("value"));
            assertEquals(String.class, env.statement("select").getEventType().getPropertyType("symbol"));

            sendMarketBean(env, "M1");
            String[] fieldsStmt = new String[]{"value", "symbol"};
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M1"});

            sendSupportBean(env, "S1", 1L, 2L);
            assertFalse(env.listener("select").isInvoked());
            String[] fieldsWin = new String[]{"a", "b", "c"};
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fieldsWin, new Object[]{"S1", 1L, 2L});
            } else {
                assertFalse(env.listener("create").isInvoked());
            }

            // create consumer 2 -- note that this one should not start empty now
            String stmtTextSelectTwo = "@name('selectTwo') select irstream (select a from MyInfra) as value, symbol from SupportMarketDataBean";
            if (disableIndexShareConsumer) {
                stmtTextSelectTwo = "@Hint('disable_window_subquery_indexshare') " + stmtTextSelectTwo;
            }
            env.compileDeploy(stmtTextSelectTwo, path).addListener("selectTwo");

            sendMarketBean(env, "M1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S1", "M1"});
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S1", "M1"});

            sendSupportBean(env, "S2", 10L, 20L);
            assertFalse(env.listener("select").isInvoked());
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fieldsWin, new Object[]{"S2", 10L, 20L});
            }

            sendMarketBean(env, "M2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M2"});
            assertFalse(env.listener("create").isInvoked());
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M2"});

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportBean_A delete from MyInfra where id = a";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // delete S1
            env.sendEventBean(new SupportBean_A("S1"));
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fieldsWin, new Object[]{"S1", 1L, 2L});
            }

            sendMarketBean(env, "M3");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S2", "M3"});
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S2", "M3"});

            // delete S2
            env.sendEventBean(new SupportBean_A("S2"));
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fieldsWin, new Object[]{"S2", 10L, 20L});
            }

            sendMarketBean(env, "M4");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M4"});
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M4"});

            sendSupportBean(env, "S3", 100L, 200L);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fieldsWin, new Object[]{"S3", 100L, 200L});
            }

            sendMarketBean(env, "M5");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S3", "M5"});
            EPAssertionUtil.assertProps(env.listener("selectTwo").assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S3", "M5"});

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

    private static void sendMarketBean(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(bean);
    }
}
