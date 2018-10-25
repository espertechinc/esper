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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class InfraNWTableJoin {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraNWTableJoinSimple(true));
        execs.add(new InfraNWTableJoinSimple(false));
        return execs;
    }

    public static class InfraNWTableJoinSimple implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNWTableJoinSimple(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"c0", "c1"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "create schema MyEvent(cid string);\n";
            stmtTextCreate += namedWindow ?
                "create window MyInfra.win:keepall() as MyEvent" :
                "create table MyInfra(cid string primary key)";
            env.compileDeployWBusPublicType(stmtTextCreate, path);

            // create insert into
            String stmtTextInsert = "insert into MyInfra select * from MyEvent";
            env.compileDeploy(stmtTextInsert, path);

            // create join
            String stmtTextJoin = "@name('s0') select ce.cid as c0, sb.intPrimitive as c1 from MyInfra as ce, SupportBean#keepall() as sb" +
                " where sb.theString = ce.cid";
            env.compileDeploy(stmtTextJoin, path).addListener("s0");

            sendMyEvent(env, "C1");
            sendMyEvent(env, "C2");
            sendMyEvent(env, "C3");

            env.milestone(0);

            env.sendEventBean(new SupportBean("C2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C2", 1});

            env.sendEventBean(new SupportBean("C1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C1", 4});

            env.undeployAll();
        }

        private void sendMyEvent(RegressionEnvironment env, String c1) {
            env.sendEventMap(Collections.singletonMap("cid", c1), "MyEvent");
        }
    }
}
