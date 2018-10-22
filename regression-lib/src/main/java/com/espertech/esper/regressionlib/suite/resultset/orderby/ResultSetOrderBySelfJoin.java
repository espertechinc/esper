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
package com.espertech.esper.regressionlib.suite.resultset.orderby;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportHierarchyEvent;

import java.util.ArrayList;
import java.util.Collection;

public class ResultSetOrderBySelfJoin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetOrderBySelfJoinSimple());
        return execs;
    }

    public static class ResultSetOrderBySelfJoinSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"prio", "cnt"};
            String epl = "@name('s0') select c1.event_criteria_id as ecid, " +
                "c1.priority as priority, " +
                "c2.priority as prio, cast(count(*), int) as cnt from " +
                "SupportHierarchyEvent#lastevent as c1, " +
                "SupportHierarchyEvent#groupwin(event_criteria_id)#lastevent as c2, " +
                "SupportHierarchyEvent#groupwin(event_criteria_id)#lastevent as p " +
                "where c2.event_criteria_id in (c1.event_criteria_id,2,1) " +
                "and p.event_criteria_id in (c1.parent_event_criteria_id, c1.event_criteria_id) " +
                "order by c2.priority asc";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, 1, 1, null);

            env.milestone(0);

            sendEvent(env, 3, 2, 2);
            sendEvent(env, 3, 2, 2);
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{1, 2}, {2, 2}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, Integer ecid, Integer priority, Integer parent) {
        SupportHierarchyEvent ev = new SupportHierarchyEvent(ecid, priority, parent);
        env.sendEventBean(ev);
    }
}
