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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

public class LRMovingZoneStmt {
    public static void createStmt(EPRuntime runtime,
                                  int secTimeout,
                                  UpdateListener listener) {
        String epl = "@name('count') insert into CountZone " +
            "select zone, count(*) as cnt " +
            "from LocationReport#unique(assetId) " +
            "where assetId in ('A1', 'A2', 'A3') " +
            "group by zone;\n" +
            "\n" +
            "@name('out') select Part.zone from pattern [" +
            "  every Part=CountZone(cnt in (1,2)) ->" +
            "  (timer:interval(" + secTimeout + " sec) " +
            "    and not CountZone(zone=Part.zone, cnt in (0,3)))]";

        EPDeployment deployed;
        try {
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(runtime.getRuntimePath()));
            deployed = runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        EPStatement stmtCount = runtime.getDeploymentService().getStatement(deployed.getDeploymentId(), "count");
        stmtCount.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                for (int i = 0; i < newEvents.length; i++) {
                    System.out.println("Summary: zone " + newEvents[i].get("zone") +
                        " now has count " + newEvents[i].get("cnt"));
                }
            }
        });

        EPStatement stmtAlert = runtime.getDeploymentService().getStatement(deployed.getDeploymentId(), "out");
        stmtAlert.addListener(listener);
    }
}
