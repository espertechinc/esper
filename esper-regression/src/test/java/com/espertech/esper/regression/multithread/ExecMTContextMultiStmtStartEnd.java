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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.fail;

public class ExecMTContextMultiStmtStartEnd implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion(ConfigurationEngineDefaults.FilterServiceProfile.READMOSTLY);
        runAssertion(ConfigurationEngineDefaults.FilterServiceProfile.READWRITE);
    }

    private void runAssertion(ConfigurationEngineDefaults.FilterServiceProfile profile) throws Exception {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getExecution().setFilterServiceProfile(profile);
        String engineURI = this.getClass().getSimpleName() + "_" + profile;
        EPServiceProvider engine = EPServiceProviderManager.getProvider(engineURI, configuration);
        engine.getEPAdministrator().getConfiguration().addEventType(MyEvent.class);

        engine.getEPAdministrator().createEPL("create context MyContext start @now end after 100 milliseconds");
        SupportUpdateListener[] listeners = new SupportUpdateListener[100];
        for (int i = 0; i < 100; i++) {
            listeners[i] = new SupportUpdateListener();
            EPStatement stmt = engine.getEPAdministrator().createEPL("context MyContext select fieldOne, count(*) as cnt from MyEvent " +
                    "group by fieldOne output last when terminated");
            stmt.addListener(listeners[i]);
        }

        int eventCount = 100000; // keep this divisible by 1000
        for (int i = 0; i < eventCount; i++) {
            String group = Integer.toString(eventCount % 1000);
            engine.getEPRuntime().sendEvent(new MyEvent(Integer.toString(i), group));
        }

        Thread.sleep(2000);
        engine.destroy();

        assertReceived(eventCount, listeners);
    }

    private void assertReceived(int eventCount, SupportUpdateListener[] listeners) {
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
