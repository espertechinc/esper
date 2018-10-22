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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.FilterServiceProfile;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import static org.junit.Assert.fail;

public class MultithreadContextMultiStmtStartEnd {

    public void run(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        runAssertion(FilterServiceProfile.READMOSTLY, configuration);
        runAssertion(FilterServiceProfile.READWRITE, configuration);
    }

    private void runAssertion(FilterServiceProfile profile, Configuration configuration) {
        configuration.getRuntime().getExecution().setFilterServiceProfile(profile);
        configuration.getCommon().addEventType(MyEvent.class);

        String runtimeURI = this.getClass().getSimpleName() + "_" + profile;
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        RegressionPath path = new RegressionPath();
        String eplContext = "create context MyContext start @now end after 100 milliseconds;\n";
        EPCompiled compiledContext = SupportCompileDeployUtil.compile(eplContext, configuration, path);
        SupportCompileDeployUtil.deploy(compiledContext, runtime);
        path.add(compiledContext);

        String epl = "context MyContext select fieldOne, count(*) as cnt from MyEvent " +
            "group by fieldOne output last when terminated;\n";
        EPCompiled compiledStmt = SupportCompileDeployUtil.compile(epl, configuration, path);
        SupportUpdateListener[] listeners = new SupportUpdateListener[100];

        for (int i = 0; i < 100; i++) {
            listeners[i] = new SupportUpdateListener();
            String stmtName = "s" + i;
            SupportCompileDeployUtil.deployAddListener(compiledStmt, stmtName, listeners[i], runtime);
        }

        int eventCount = 100000; // keep this divisible by 1000
        for (int i = 0; i < eventCount; i++) {
            String group = Integer.toString(eventCount % 1000);
            runtime.getEventService().sendEventBean(new MyEvent(Integer.toString(i), group), "MyEvent");
        }

        SupportCompileDeployUtil.threadSleep(2000);

        assertReceived(eventCount, listeners);

        try {
            runtime.getDeploymentService().undeployAll();
        } catch (EPUndeployException e) {
            throw new RuntimeException(e);
        }

        runtime.destroy();
    }

    private static void assertReceived(int eventCount, SupportUpdateListener[] listeners) {
        for (SupportUpdateListener listener : listeners) {
            EventBean[] outputEvents = listener.getNewDataListFlattened();
            long total = 0;

            for (EventBean out : outputEvents) {
                long cnt = (Long) out.get("cnt");
                total += cnt;
            }

            if (total != eventCount) {
                fail("Listener received " + total + " expected " + eventCount);
            }
        }
    }

    public class MyEvent {
        private final String id;
        private final String fieldOne;

        public MyEvent(String id, String fieldOne) {
            this.id = id;
            this.fieldOne = fieldOne;
        }

        public String getId() {
            return id;
        }

        public String getFieldOne() {
            return fieldOne;
        }
    }
}
