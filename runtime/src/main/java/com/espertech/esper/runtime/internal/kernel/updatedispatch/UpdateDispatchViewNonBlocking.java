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
import com.espertech.esper.common.internal.statement.dispatch.DispatchService;

/**
 * Convenience view for dispatching view updates received from a parent view to update listeners
 * via the dispatch service.
 */
public class UpdateDispatchViewNonBlocking extends UpdateDispatchViewBase {
    /**
     * Ctor.
     *
     * @param dispatchService            - for performing the dispatch
     * @param statementResultServiceImpl - handles result delivery
     * @param eventType                  event type
     */
    public UpdateDispatchViewNonBlocking(EventType eventType, StatementResultService statementResultServiceImpl, DispatchService dispatchService) {
        super(eventType, statementResultServiceImpl, dispatchService);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        newResult(new UniformPair<>(newData, oldData));
    }

    public void newResult(UniformPair<EventBean[]> results) {
        StatementDispatchTLEntry dispatchTLEntry = statementResultService.getDispatchTL().get();
        statementResultService.indicate(results, dispatchTLEntry);
        if (!dispatchTLEntry.isDispatchWaiting()) {
            dispatchService.addExternal(this);
            dispatchTLEntry.setDispatchWaiting(true);
        }
    }
}
