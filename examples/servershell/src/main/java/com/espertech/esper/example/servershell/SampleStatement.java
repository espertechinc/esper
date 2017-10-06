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
package com.espertech.esper.example.servershell;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleStatement {
    private final static Logger log = LoggerFactory.getLogger(SampleStatement.class);

    public static void createStatement(EPAdministrator admin) {
        EPStatement statement = admin.createEPL("select istream ipAddress, avg(duration) from SampleEvent#time(10 sec) group by ipAddress output snapshot every 2 seconds order by ipAddress asc");
        statement.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                if (newEvents == null) {
                    return;
                }

                for (int i = 0; i < newEvents.length; i++) {
                    if (log.isInfoEnabled()) {
                        log.info("IPAddress: " + newEvents[i].get("ipAddress") +
                                " Avg Duration: " + newEvents[i].get("avg(duration)"));
                    }
                }
            }
        });

    }
}
