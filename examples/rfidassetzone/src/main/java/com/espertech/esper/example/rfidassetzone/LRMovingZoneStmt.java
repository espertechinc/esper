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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class LRMovingZoneStmt {
    public static void createStmt(EPServiceProvider epService,
                                  int secTimeout,
                                  UpdateListener listener) {
        String textOne = "insert into CountZone " +
                "select zone, count(*) as cnt " +
                "from LocationReport#unique(assetId) " +
                "where assetId in ('A1', 'A2', 'A3') " +
                "group by zone";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        stmtOne.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                for (int i = 0; i < newEvents.length; i++) {
                    System.out.println("Summary: zone " + newEvents[i].get("zone") +
                            " now has count " + newEvents[i].get("cnt"));
                }
            }
        });

        String textTwo = "select Part.zone from pattern [" +
                "  every Part=CountZone(cnt in (1,2)) ->" +
                "  (timer:interval(" + secTimeout + " sec) " +
                "    and not CountZone(zone=Part.zone, cnt in (0,3)))]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        stmtTwo.addListener(listener);
    }
}
