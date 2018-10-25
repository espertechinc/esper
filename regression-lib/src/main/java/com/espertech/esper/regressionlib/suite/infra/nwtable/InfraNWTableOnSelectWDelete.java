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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.ArrayList;
import java.util.Collection;

public class InfraNWTableOnSelectWDelete implements IndexBackingTableInfo {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraNWTableOnSelectWDeleteAssertion(true));
        execs.add(new InfraNWTableOnSelectWDeleteAssertion(false));
        return execs;
    }

    private static class InfraNWTableOnSelectWDeleteAssertion implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNWTableOnSelectWDeleteAssertion(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fieldsWin = "theString,intPrimitive".split(",");
            String[] fieldsSelect = "c0".split(",");
            RegressionPath path = new RegressionPath();

            String eplCreate = namedWindow ?
                "@name('create') create window MyInfra#keepall as SupportBean" :
                "@name('create') create table MyInfra (theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreate, path);

            env.compileDeploy("insert into MyInfra select theString, intPrimitive from SupportBean", path);

            String eplSelectDelete = "@name('s0') on SupportBean_S0 as s0 " +
                "select and delete window(win.*).aggregate(0,(result,value) => result+value.intPrimitive) as c0 " +
                "from MyInfra as win where s0.p00=win.theString";
            env.compileDeploy(eplSelectDelete, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fieldsWin, new Object[][]{{"E1", 1}, {"E2", 2}});
            } else {
                EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsWin, new Object[][]{{"E1", 1}, {"E2", 2}});
            }

            // select and delete bean E1
            env.sendEventBean(new SupportBean_S0(100, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{1});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsWin, new Object[][]{{"E2", 2}});

            env.milestone(0);

            // add some E2 events
            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsWin, new Object[][]{{"E2", 2}, {"E2", 3}, {"E2", 4}});

            // select and delete beans E2
            env.sendEventBean(new SupportBean_S0(101, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{2 + 3 + 4});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsWin, new Object[0][]);

            // test SODA
            env.eplToModelCompileDeploy(eplSelectDelete, path);

            env.undeployAll();
        }
    }
}
