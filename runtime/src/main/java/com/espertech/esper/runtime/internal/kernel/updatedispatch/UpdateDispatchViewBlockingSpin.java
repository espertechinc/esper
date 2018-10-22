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
package com.espertech.esper.runtime.internal.kernel.updatedispatch;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.StatementDispatchTLEntry;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.schedule.TimeSourceService;
import com.espertech.esper.common.internal.statement.dispatch.DispatchService;

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
     * @param eventType              event type
     */
    public UpdateDispatchViewBlockingSpin(EventType eventType, StatementResultService statementResultService, DispatchService dispatchService, long msecTimeout, TimeSourceService timeSourceService) {
        super(eventType, statementResultService, dispatchService);
        this.currentFutureSpin = new UpdateDispatchFutureSpin(timeSourceService); // use a completed future as a start
        this.msecTimeout = msecTimeout;
        this.timeSourceService = timeSourceService;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        newResult(new UniformPair<>(newData, oldData));
    }

    public void newResult(UniformPair<EventBean[]> result) {
        StatementDispatchTLEntry dispatchTLEntry = statementResultService.getDispatchTL().get();
        statementResultService.indicate(result, dispatchTLEntry);
        if (!dispatchTLEntry.isDispatchWaiting()) {
            UpdateDispatchFutureSpin nextFutureSpin;
            synchronized (this) {
                nextFutureSpin = new UpdateDispatchFutureSpin(this, currentFutureSpin, msecTimeout, timeSourceService);
                currentFutureSpin = nextFutureSpin;
            }
            dispatchService.addExternal(nextFutureSpin);
            dispatchTLEntry.setDispatchWaiting(true);
        }
    }
}
