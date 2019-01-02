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
import com.espertech.esper.common.internal.statement.dispatch.Dispatchable;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * Convenience view for dispatching view updates received from a parent view to update listeners
 * via the dispatch service.
 */
public abstract class UpdateDispatchViewBase extends ViewSupport implements Dispatchable, UpdateDispatchView {
    protected final EventType eventType;

    /**
     * Handles result delivery
     */
    protected final StatementResultService statementResultService;

    /**
     * Dispatches events to listeners.
     */
    protected final DispatchService dispatchService;

    /**
     * For iteration with patterns.
     */
    protected EventBean lastIterableEvent;

    /**
     * Ctor.
     *
     * @param dispatchService            - for performing the dispatch
     * @param statementResultServiceImpl - handles result delivery
     * @param eventType                  event type
     */
    public UpdateDispatchViewBase(EventType eventType, StatementResultService statementResultServiceImpl, DispatchService dispatchService) {
        this.dispatchService = dispatchService;
        this.statementResultService = statementResultServiceImpl;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException();
    }

    public void execute() {
        StatementDispatchTLEntry dispatchTLEntry = statementResultService.getDispatchTL().get();
        dispatchTLEntry.setDispatchWaiting(false);
        statementResultService.execute(dispatchTLEntry);
    }

    /**
     * Remove event reference to last event.
     */
    public void clear() {
        lastIterableEvent = null;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public UpdateDispatchView getView() {
        return this;
    }

    public void newResult(UniformPair<EventBean[]> result) {

    }

    public void update(EventBean[] newData, EventBean[] oldData) {

    }

    public void cancelled() {
        clear();
    }
}
