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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableIterate implements RegressionExecution {

    private final static String METHOD_NAME = "method:SupportStaticMethodLib.fetchTwoRows3Cols()";

    public void run(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create table MyTable(pkey0 string primary key, pkey1 int primary key, c0 long)", path);
        env.compileDeploy("insert into MyTable select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean", path);

        sendSupportBean(env, "E1", 10, 100);
        sendSupportBean(env, "E2", 20, 200);

        runAssertion(env, path, true);
        runAssertion(env, path, false);

        env.undeployAll();
    }

    private static void runAssertion(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        runUnaggregatedUngroupedSelectStar(env, path, useTable);
        runFullyAggregatedAndUngrouped(env, path, useTable);
        runAggregatedAndUngrouped(env, path, useTable);
        runFullyAggregatedAndGrouped(env, path, useTable);
        runAggregatedAndGrouped(env, path, useTable);
        runAggregatedAndGroupedRollup(env, path, useTable);
    }

    private static void runUnaggregatedUngroupedSelectStar(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select * from " + (useTable ? "MyTable" : METHOD_NAME);
        env.compileDeploy(epl, path);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), "pkey0,pkey1,c0".split(","), new Object[][]{{"E1", 10, 100L}, {"E2", 20, 200L}});
        env.undeployModuleContaining("s0");
    }

    private static void runFullyAggregatedAndUngrouped(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        env.compileDeploy(epl, path);
        for (int i = 0; i < 2; i++) {
            EventBean event = env.iterator("s0").next();
            assertEquals(2L, event.get("thecnt"));
        }
        env.undeployModuleContaining("s0");
    }

    private static void runAggregatedAndUngrouped(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        env.compileDeploy(epl, path);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), "pkey0,thecnt".split(","), new Object[][]{{"E1", 2L}, {"E2", 2L}});
        }
        env.undeployModuleContaining("s0");
    }

    private static void runFullyAggregatedAndGrouped(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        env.compileDeploy(epl, path);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), "pkey0,thecnt".split(","), new Object[][]{{"E1", 1L}, {"E2", 1L}});
        }
        env.undeployModuleContaining("s0");
    }

    private static void runAggregatedAndGrouped(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select pkey0, pkey1, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        env.compileDeploy(epl, path);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), "pkey0,pkey1,thecnt".split(","), new Object[][]{{"E1", 10, 1L}, {"E2", 20, 1L}});
        }
        env.undeployModuleContaining("s0");
    }

    private static void runAggregatedAndGroupedRollup(RegressionEnvironment env, RegressionPath path, boolean useTable) {
        String epl = "@name('s0') select pkey0, pkey1, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by rollup (pkey0, pkey1)";
        env.compileDeploy(epl, path);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), "pkey0,pkey1,thecnt".split(","), new Object[][]{
                {"E1", 10, 1L},
                {"E2", 20, 1L},
                {"E1", null, 1L},
                {"E2", null, 1L},
                {null, null, 2L},
            });
        }
        env.undeployAll();
    }

    private SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
        return bean;
    }
}
