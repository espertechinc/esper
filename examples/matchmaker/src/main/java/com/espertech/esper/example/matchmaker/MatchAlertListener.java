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
package com.espertech.esper.example.matchmaker;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class MatchAlertListener implements UpdateListener {
    private List<MatchAlertBean> emittedList = new LinkedList<MatchAlertBean>();

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
        for (int i = 0; i < newEvents.length; i++) {
            MobileUserBean other = (MobileUserBean) newEvents[i].get("other");
            MobileUserBean self = (MobileUserBean) newEvents[i].get("self");
            MatchAlertBean alert = new MatchAlertBean(other.getUserId(), self.getUserId());
            emittedList.add(alert);
        }
    }

    public int getSize() {
        return emittedList.size();
    }

    public List getEmittedList() {
        return emittedList;
    }

    public int getAndClearEmittedCount() {
        int count = emittedList.size();
        emittedList.clear();
        return count;
    }

    public void clearEmitted() {
        emittedList.clear();
    }

    private static final Logger log = LoggerFactory.getLogger(MatchAlertListener.class);
}
