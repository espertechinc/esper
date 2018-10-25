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

import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableRollup {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraRollupOneDim());
        execs.add(new InfraRollupTwoDim());
        execs.add(new InfraGroupingSetThreeDim());
        return execs;
    }

    private static class InfraRollupOneDim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsOut = "theString,total".split(",");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create table MyTableR1D(pk string primary key, total sum(int))", path);
            env.compileDeploy("@name('into') into table MyTableR1D insert into MyStreamOne select theString, sum(intPrimitive) as total from SupportBean#length(4) group by rollup(theString)", path).addListener("into");
            env.compileDeploy("@name('s0') select MyTableR1D[p00].total as c0 from SupportBean_S0", path).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 10));
            assertValuesListener(env, new Object[][]{{null, 10}, {"E1", 10}, {"E2", null}});
            EPAssertionUtil.assertPropsPerRow(env.listener("into").getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 10}, {null, 10}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 200));
            assertValuesListener(env, new Object[][]{{null, 210}, {"E1", 10}, {"E2", 200}});
            EPAssertionUtil.assertPropsPerRow(env.listener("into").getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 200}, {null, 210}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E1", 11));
            assertValuesListener(env, new Object[][]{{null, 221}, {"E1", 21}, {"E2", 200}});
            EPAssertionUtil.assertPropsPerRow(env.listener("into").getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 21}, {null, 221}});

            env.sendEventBean(new SupportBean("E2", 201));
            assertValuesListener(env, new Object[][]{{null, 422}, {"E1", 21}, {"E2", 401}});
            EPAssertionUtil.assertPropsPerRow(env.listener("into").getAndResetLastNewData(), fieldsOut, new Object[][]{{"E2", 401}, {null, 422}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E1", 12)); // {"E1", 10} leaving window
            assertValuesListener(env, new Object[][]{{null, 424}, {"E1", 23}, {"E2", 401}});
            EPAssertionUtil.assertPropsPerRow(env.listener("into").getAndResetLastNewData(), fieldsOut, new Object[][]{{"E1", 23}, {null, 424}});

            env.undeployAll();
        }
    }

    private static class InfraRollupTwoDim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "k0,k1,total".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create objectarray schema MyEventTwo(k0 int, k1 int, col int)", path);
            env.compileDeploy("create table MyTableR2D(k0 int primary key, k1 int primary key, total sum(int))", path);
            env.compileDeploy("into table MyTableR2D insert into MyStreamTwo select sum(col) as total from MyEventTwo#length(3) group by rollup(k0,k1)", path);

            env.sendEventObjectArray(new Object[]{1, 10, 100}, "MyEventTwo");
            env.sendEventObjectArray(new Object[]{2, 10, 200}, "MyEventTwo");
            env.sendEventObjectArray(new Object[]{1, 20, 300}, "MyEventTwo");

            assertValuesIterate(env, path, "MyTableR2D", fields, new Object[][]{{null, null, 600}, {1, null, 400}, {2, null, 200},
                {1, 10, 100}, {2, 10, 200}, {1, 20, 300}});

            env.milestone(0);

            env.sendEventObjectArray(new Object[]{1, 10, 400}, "MyEventTwo"); // expires {1, 10, 100}

            assertValuesIterate(env, path, "MyTableR2D", fields, new Object[][]{{null, null, 900}, {1, null, 700}, {2, null, 200},
                {1, 10, 400}, {2, 10, 200}, {1, 20, 300}});

            env.undeployAll();
        }
    }

    private static class InfraGroupingSetThreeDim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create objectarray schema MyEventThree(k0 int, k1 int, k2 int, col int)", path);

            env.compileDeploy("create table MyTableGS3D(k0 int primary key, k1 int primary key, k2 int primary key, total sum(int))", path);
            env.compileDeploy("into table MyTableGS3D insert into MyStreamThree select sum(col) as total from MyEventThree#length(3) group by grouping sets(k0,k1,k2)", path);

            String[] fields = "k0,k1,k2,total".split(",");
            env.sendEventObjectArray(new Object[]{1, 10, 100, 1000}, "MyEventThree");
            env.sendEventObjectArray(new Object[]{2, 10, 200, 2000}, "MyEventThree");

            env.milestone(0);

            env.sendEventObjectArray(new Object[]{1, 20, 300, 3000}, "MyEventThree");

            assertValuesIterate(env, path, "MyTableGS3D", fields, new Object[][]{
                {1, null, null, 4000}, {2, null, null, 2000},
                {null, 10, null, 3000}, {null, 20, null, 3000},
                {null, null, 100, 1000}, {null, null, 200, 2000}, {null, null, 300, 3000}});

            env.milestone(1);

            env.sendEventObjectArray(new Object[]{1, 10, 400, 4000}, "MyEventThree"); // expires {1, 10, 100, 1000}

            env.milestone(2);

            assertValuesIterate(env, path, "MyTableGS3D", fields, new Object[][]{
                {1, null, null, 7000}, {2, null, null, 2000},
                {null, 10, null, 6000}, {null, 20, null, 3000},
                {null, null, 100, null}, {null, null, 400, 4000}, {null, null, 200, 2000}, {null, null, 300, 3000}});

            env.undeployAll();
        }
    }

    private static void assertValuesIterate(RegressionEnvironment env, RegressionPath path, String name, String[] fields, Object[][] objects) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from " + name, path);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, objects);
    }

    private static void assertValuesListener(RegressionEnvironment env, Object[][] objects) {
        for (int i = 0; i < objects.length; i++) {
            String p00 = (String) objects[i][0];
            Integer expected = (Integer) objects[i][1];
            env.sendEventBean(new SupportBean_S0(0, p00));
            assertEquals("Failed at " + i + " for key " + p00, expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }
}
