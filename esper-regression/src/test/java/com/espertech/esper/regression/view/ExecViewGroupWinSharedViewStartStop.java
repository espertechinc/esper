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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecViewGroupWinSharedViewStartStop implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(SubscriberName string, ValueInt float)");
        final String query = "select SubscriberName, avg(ValueInt) "
                + "from MyEvent#groupwin(SubscriberName)#length(4)"
                + "group by SubscriberName output snapshot every 1 events";
        final String query2 = "select SubscriberName, avedev(ValueInt) "
                + "from MyEvent#groupwin(SubscriberName)#length(3) "
                + "group by SubscriberName output snapshot every 1 events";

        final String[] groups = {
            "G_A", "G_A", "G_A", "G_A", "G_B", "G_B", "G_B", "G_B",
            "G_B", "G_B", "G_B", "G_B", "G_B", "G_B", "G_B", "G_B",
            "G_B", "G_B", "G_B", "G_B", "G_C", "G_C", "G_C", "G_C",
            "G_D", "G_A", "G_D", "G_D", "G_A", "G_D", "G_D", "G_D",
            "G_A", "G_A", "G_A", "G_A", "G_C", "G_C", "G_C", "G_C",
            "G_D", "G_A", "G_D", "G_D", "G_D", "G_A", "G_D", "G_D",
            "G_D", "G_E"};

        EPStatement statement = epService.getEPAdministrator().createEPL(query, "myquery");
        EPStatement statement2 = epService.getEPAdministrator().createEPL(query2, "myquery2");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        statement2.addListener(listener);

        int i = 0;
        for (String csv : groups) {
            Object[] event = {csv, 0f};
            epService.getEPRuntime().sendEvent(event, "MyEvent");
            i++;

            EPStatement stmt = epService.getEPAdministrator().getStatement("myquery");
            if (i % 6 == 0) {
                stmt.stop();
            } else if (i % 6 == 4) {
                stmt.start();
            }
        }
    }
}
