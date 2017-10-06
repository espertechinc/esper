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
package com.espertech.esper.example.ohlc;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class OHLCUpdateListener implements StatementAwareUpdateListener {
    private final static Logger log = LoggerFactory.getLogger(OHLCUpdateListener.class);

    public void update(EventBean[] newData, EventBean[] oldData, EPStatement epStatement, EPServiceProvider epServiceProvider) {
        for (int i = 0; i < newData.length; i++) {
            if (log.isInfoEnabled()) {
                log.info("Statement " + String.format("%s", epStatement.getName()) + " produced: " + getProperties(newData[i]));
            }
        }
    }

    private String getProperties(EventBean theEvent) {
        StringBuilder buf = new StringBuilder();

        for (String name : theEvent.getEventType().getPropertyNames()) {
            Object value = theEvent.get(name);
            buf.append(name);
            buf.append("=");

            if (name.contains("timestamp")) {
                buf.append(new Date((Long) value));
            } else {
                buf.append(value);
            }
            buf.append(" ");
        }
        return buf.toString();
    }
}
