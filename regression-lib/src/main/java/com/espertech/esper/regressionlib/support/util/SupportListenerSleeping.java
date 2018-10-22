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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SupportListenerSleeping implements UpdateListener {
    private List<Pair<Long, EventBean[]>> newEvents = Collections.synchronizedList(new ArrayList<Pair<Long, EventBean[]>>());

    private final long sleepTime;

    public SupportListenerSleeping(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void update(EventBean[] newData, EventBean[] oldData, EPStatement statement, EPRuntime runtime) {
        long time = System.nanoTime();
        newEvents.add(new Pair<>(time, newData));

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
