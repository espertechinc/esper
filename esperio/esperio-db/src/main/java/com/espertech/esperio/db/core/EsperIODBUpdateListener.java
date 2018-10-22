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
package com.espertech.esperio.db.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class EsperIODBUpdateListener implements UpdateListener {
    private final static Logger log = LoggerFactory.getLogger(EsperIODBUpdateListener.class);

    private final RunnableFactory runnableFactory;
    private final Executor executor;

    public EsperIODBUpdateListener(RunnableFactory runnableFactory, Executor executor) {
        this.runnableFactory = runnableFactory;
        this.executor = executor;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
        if (newEvents == null) {
            return;
        }
        for (EventBean event : newEvents) {
            try {
                Runnable runnable = runnableFactory.makeRunnable(event);
                executor.execute(runnable);
            } catch (Throwable t) {
                log.error("Error executing database action:" + t.getMessage(), t);
            }
        }
    }
}
