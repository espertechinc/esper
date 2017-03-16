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
package com.espertech.esper.example.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class SampleUpdateListener implements UpdateListener {

    private EventBean lastEvent;

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        lastEvent = newEvents[0];
    }

    public EventBean getLastEvent() {
        return lastEvent;
    }
}
