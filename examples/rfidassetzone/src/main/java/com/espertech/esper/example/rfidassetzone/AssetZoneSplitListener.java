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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AssetZoneSplitListener implements UpdateListener {
    private static final Logger log = LoggerFactory.getLogger(AssetZoneSplitListener.class);

    private List<Integer> callbacks = new ArrayList<Integer>();

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        int groupId = (Integer) newEvents[0].get("a.groupId");
        callbacks.add(groupId);
        log.info(".update Received event from group id " + groupId);
    }

    public List<Integer> getCallbacks() {
        return callbacks;
    }

    public void reset() {
        callbacks.clear();
    }
}
