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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

public class ExecNamedWindowConsumer implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create schema IncomingEvent(id int);\n" +
                        "create schema RetainedEvent(id int);\n" +
                        "insert into RetainedEvent select * from IncomingEvent#expr_batch(current_count >= 10000);\n" +
                        "create window RetainedEventWindow#keepall as RetainedEvent;\n" +
                        "insert into RetainedEventWindow select * from RetainedEvent;\n");

        Map<String, Integer> event = new HashMap<String, Integer>();
        event.put("id", 1);
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(event, "IncomingEvent");
        }
    }
}
