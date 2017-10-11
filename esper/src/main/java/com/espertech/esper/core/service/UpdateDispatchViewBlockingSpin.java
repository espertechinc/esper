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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.timer.TimeSourceService;
import com.espertech.esper.util.MutableBoolean;

/**
 * Convenience view for dispatching view updates received from a parent view to update listeners
 * via the dispatch service.
 */
public class UpdateDispatchViewBlockingSpin extends UpdateDispatchViewBase {
    private UpdateDispatchFutureSpin currentFutureSpin;
    private long msecTimeout;
    private TimeSourceService timeSourceService;

    /**
     * Ctor.
     *
     * @param dispatchService        - for performing the dispatch
     * @param msecTimeout            - timeout for preserving dispatch order through blocking
     * @param statementResultService - handles result delivery
     * @param timeSourceService      time source provider
     */
    public UpdateDispatchViewBlockingSpin(StatementResultService statementResultService, DispatchService dispatchService, long msecTimeout, TimeSourceService timeSourceService) {
        super(statementResultService, dispatchService);
        this.currentFutureSpin = new UpdateDispatchFutureSpin(timeSourceService); // use a completed future as a start
        this.msecTimeout = msecTimeout;
        this.timeSourceService = timeSourceService;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        newResult(new UniformPair<>(newData, oldData));
    }

    public void newResult(UniformPair<EventBean[]> result) {
        statementResultService.indicate(result);
        MutableBoolean waiting = isDispatchWaiting.get();
        if (!waiting.isValue()) {
            UpdateDispatchFutureSpin nextFutureSpin;
            synchronized (this) {
                nextFutureSpin = new UpdateDispatchFutureSpin(this, currentFutureSpin, msecTimeout, timeSourceService);
                currentFutureSpin = nextFutureSpin;
            }
            dispatchService.addExternal(nextFutureSpin);
            waiting.setValue(true);
        }
    }
}
