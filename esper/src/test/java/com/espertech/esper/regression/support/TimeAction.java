/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.support;

import com.espertech.esper.support.bean.SupportMarketDataBean;

import java.util.List;
import java.util.ArrayList;

public class TimeAction
{
    private List<EventSendDesc> events;
    private String actionDesc;

    public TimeAction() {
        events = new ArrayList<EventSendDesc>();
    }

    public void add(SupportMarketDataBean theEvent, String eventDesc)
    {
        events.add(new EventSendDesc(theEvent, eventDesc));
    }

    public void setActionDesc(String actionDesc) {
        this.actionDesc = actionDesc;
    }

    public List<EventSendDesc> getEvents() {
        return events;
    }

    public String getActionDesc() {
        return actionDesc;
    }
}

