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
package com.espertech.esper.runtime.internal.dataflow.op.epstatementsource;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.Queue;

public class EmitterUpdateListener implements UpdateListener {
    private final Queue<Object> queue;
    private final boolean submitEventBean;

    public EmitterUpdateListener(Queue<Object> queue, boolean submitEventBean) {
        this.queue = queue;
        this.submitEventBean = submitEventBean;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
        if (newEvents != null) {
            for (EventBean newEvent : newEvents) {
                if (submitEventBean) {
                    queue.add(newEvent);
                } else {
                    Object underlying = newEvent.getUnderlying();
                    queue.add(underlying);
                }
            }
        }
    }
}
