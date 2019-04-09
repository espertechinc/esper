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
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class InfraNWTableOnUpdate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraNWTableOnUpdateSceneOne(true));
        execs.add(new InfraNWTableOnUpdateSceneOne(false));

        execs.add(new InfraUpdateOrderOfFields(true));
        execs.add(new InfraUpdateOrderOfFields(false));

        execs.add(new InfraSubquerySelf(true));
        execs.add(new InfraSubquerySelf(false));

        execs.add(new InfraSubqueryMultikeyWArray(true));
        execs.add(new InfraSubqueryMultikeyWArray(false));

        return execs;
    }

    private static class InfraSubqueryMultikeyWArray implements RegressionExecution {

        private boolean namedWindow;

        public InfraSubqueryMultikeyWArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall() as (value int)" :
                "@name('create') create table MyInfra(value int)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");
            env.compileExecuteFAF("insert into MyInfra select 0 as value", path);

            String epl = "on SupportBean update MyInfra set value = (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array)";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[]{1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[]{1, 2}, 11));

            env.milestone(0);
            assertUpdate(env, 21);

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[]{1, 2}, 12));
            assertUpdate(env, 33);

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[]{1}, 13));
            assertUpdate(env, null);

            env.undeployAll();
        }

        private void assertUpdate(RegressionEnvironment env, Integer expected) {
            env.sendEventBean(new SupportBean());
            assertEquals(expected, env.iterator("create").next().get("value"));
        }
    }

    public static class InfraNWTableOnUpdateSceneOne implements RegressionExecution {
        private boolean namedWindow;

        public InfraNWTableOnUpdateSceneOne(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra.win:keepall() as SupportBean" :
                "@name('create') create table MyInfra(theString string, intPrimitive int primary key)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "@name('insert') insert into MyInfra select theString, intPrimitive from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(0);

            // populate some data
            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("B2", 2));

            // create onUpdate
            String stmtTextOnUpdate = "@name('update') on SupportBean_S0 update MyInfra set theString = p00 where intPrimitive = id";
            env.compileDeploy(stmtTextOnUpdate, path).addListener("update");
            assertEquals(StatementType.ON_UPDATE, env.statement("update").getProperty(StatementProperty.STATEMENTTYPE));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(1, "X1"));
            EPAssertionUtil.assertProps(env.listener("update").assertOneGetOld(), fields, new Object[]{"A1", 1});
            EPAssertionUtil.assertProps(env.listener("update").getLastNewData()[0], fields, new Object[]{"X1", 1});
            env.listener("update").reset();
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"B2", 2}, {"X1", 1}});
            } else {
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"B2", 2}, {"X1", 1}});
            }

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(2, "X2"));
            EPAssertionUtil.assertProps(env.listener("update").assertOneGetOld(), fields, new Object[]{"B2", 2});
            EPAssertionUtil.assertProps(env.listener("update").getLastNewData()[0], fields, new Object[]{"X2", 2});
            env.listener("update").reset();
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"X1", 1}, {"X2", 2}});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"X1", 1}, {"X2", 2}});

            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("update");
            env.undeployModuleContaining("create");

            env.milestone(4);

            env.undeployAll();
        }
    }

    private static class InfraUpdateOrderOfFields implements RegressionExecution {
        private final boolean namedWindow;

        public InfraUpdateOrderOfFields(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            String epl = namedWindow ?
                "create window MyInfra#keepall as SupportBean;\n" :
                "create table MyInfra(theString string primary key, intPrimitive int, intBoxed int, doublePrimitive double);\n";
            epl += "insert into MyInfra select theString, intPrimitive, intBoxed, doublePrimitive from SupportBean;\n";
            epl += "@name('update') on SupportBean_S0 as sb " +
                "update MyInfra as mywin" +
                " set intPrimitive=id, intBoxed=mywin.intPrimitive, doublePrimitive=initial.intPrimitive" +
                " where mywin.theString = sb.p00;\n";
            env.compileDeploy(epl).addListener("update");
            String[] fields = "intPrimitive,intBoxed,doublePrimitive".split(",");

            env.sendEventBean(makeSupportBean("E1", 1, 2));
            env.sendEventBean(new SupportBean_S0(5, "E1"));
            EPAssertionUtil.assertProps(env.listener("update").getAndResetLastNewData()[0], fields, new Object[]{5, 5, 1.0});

            env.milestone(0);

            env.sendEventBean(makeSupportBean("E2", 10, 20));
            env.sendEventBean(new SupportBean_S0(6, "E2"));
            EPAssertionUtil.assertProps(env.listener("update").getAndResetLastNewData()[0], fields, new Object[]{6, 6, 10.0});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(7, "E1"));
            EPAssertionUtil.assertProps(env.listener("update").getAndResetLastNewData()[0], fields, new Object[]{7, 7, 5.0});

            env.undeployAll();
        }
    }

    private static class InfraSubquerySelf implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubquerySelf(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // ESPER-507
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "@name('create') create window MyInfraSS#keepall as SupportBean" :
                "@name('create') create table MyInfraSS(theString string primary key, intPrimitive int)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfraSS select theString, intPrimitive from SupportBean", path);

            // This is better done with "set intPrimitive = intPrimitive + 1"
            String epl = "@Name(\"Self Update\")\n" +
                "on SupportBean_A c\n" +
                "update MyInfraSS s\n" +
                "set intPrimitive = (select intPrimitive from MyInfraSS t where t.theString = c.id) + 1\n" +
                "where s.theString = c.id";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 6));
            env.sendEventBean(new SupportBean_A("E1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean_A("E2"));

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "theString,intPrimitive".split(","), new Object[][]{{"E1", 3}, {"E2", 7}});
            env.undeployAll();
        }
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }
}