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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SupportListenerSleeping implements UpdateListener {
    private List<Pair<Long, EventBean[]>> newEvents = Collections.synchronizedList(new ArrayList<Pair<Long, EventBean[]>>());

    private final long sleepTime;

    public SupportListenerSleeping(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void update(EventBean[] newData, EventBean[] oldEvents) {
        long time = System.nanoTime();
        newEvents.add(new Pair<Long, EventBean[]>(time, newData));

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pair<Long, EventBean[]>> getNewEvents() {
        return newEvents;
    }
}
