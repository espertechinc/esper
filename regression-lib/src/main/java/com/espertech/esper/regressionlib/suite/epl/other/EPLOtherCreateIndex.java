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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo.INDEX_CALLBACK_HOOK;

public class EPLOtherCreateIndex {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherCreateIndexPathOneModule());
        execs.add(new EPLOtherCreateIndexPathThreeModule());
        return execs;
    }

    private static class EPLOtherCreateIndexPathOneModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#keepall as (p0 string, p1 int);\n" +
                "create unique index MyIndex on MyWindow(p0);\n" +
                INDEX_CALLBACK_HOOK + "@name('s0') on SupportBean_S0 as s0 select p0,p1 from MyWindow as win where win.p0 = s0.p00;\n";
            env.compileDeploy(epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "unique hash={p0(string)} btree={} advanced={}");

            env.compileExecuteFAF("insert into MyWindow select 'a' as p0, 1 as p1", path);

            env.sendEventBean(new SupportBean_S0(1, "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{"a", 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateIndexPathThreeModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as (p0 string, p1 int);", path);
            env.compileDeploy("create unique index MyIndex on MyWindow(p0);", path);
            env.compileDeploy(INDEX_CALLBACK_HOOK + "@name('s0') on SupportBean_S0 as s0 select p0, p1 from MyWindow as win where win.p0 = s0.p00;", path);
            env.addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "unique hash={p0(string)} btree={} advanced={}");

            env.compileExecuteFAF("insert into MyWindow select 'a' as p0, 1 as p1", path);

            env.sendEventBean(new SupportBean_S0(1, "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{"a", 1});

            env.undeployAll();
        }
    }
}
