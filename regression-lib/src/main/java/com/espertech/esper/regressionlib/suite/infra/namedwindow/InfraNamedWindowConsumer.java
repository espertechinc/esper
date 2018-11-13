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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowConsumer {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraNamedWindowConsumerKeepAll());
        execs.add(new InfraNamedWindowConsumerLengthWin());
        execs.add(new InfraNamedWindowConsumerWBatch());
        return execs;
    }

    public static class InfraNamedWindowConsumerKeepAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "theString".split(",");
            String epl = "@Name('create') create window MyWindow.win:keepall() as SupportBean;\n" +
                "@Name('insert') insert into MyWindow select * from SupportBean;\n" +
                "@Name('select') select irstream * from MyWindow;\n";
            env.compileDeploy(epl).addListener("select");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.undeployAll();

            env.milestone(2);
        }
    }

    public static class InfraNamedWindowConsumerLengthWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "create window MyWindow#length(2) as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "@name('s0') select theString as c0, sum(intPrimitive) as c1 from MyWindow;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 30});

            env.sendEventBean(new SupportBean("E3", 25));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 45});

            env.sendEventBean(new SupportBean("E4", 26));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", 51});

            env.undeployAll();
        }
    }

    public static class InfraNamedWindowConsumerWBatch implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "create schema IncomingEvent(id int);\n" +
                "create schema RetainedEvent(id int);\n" +
                "insert into RetainedEvent select * from IncomingEvent#expr_batch(current_count >= 10000);\n" +
                "create window RetainedEventWindow#keepall as RetainedEvent;\n" +
                "insert into RetainedEventWindow select * from RetainedEvent;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath());

            Map<String, Object> event = new HashMap<>();
            event.put("id", 1);
            for (int i = 0; i < 10000; i++) {
                env.sendEventMap(event, "IncomingEvent");
            }

            env.undeployAll();
        }
    }
}
