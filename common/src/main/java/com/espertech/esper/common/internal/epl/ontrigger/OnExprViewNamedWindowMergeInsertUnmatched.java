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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Collections;
import java.util.Iterator;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class OnExprViewNamedWindowMergeInsertUnmatched extends ViewSupport {
    /**
     * The event type of the events hosted in the named window.
     */
    private final AgentInstanceContext agentInstanceContext;

    /**
     * The root view accepting removals (old data).
     */
    protected final NamedWindowRootViewInstance rootView;

    private final InfraOnMergeViewFactory factory;

    /**
     * Ctor.
     *
     * @param factory              merge view factory
     * @param rootView             to indicate which events to delete
     * @param agentInstanceContext context for expression evalauation
     */
    public OnExprViewNamedWindowMergeInsertUnmatched(NamedWindowRootViewInstance rootView,
                                                     AgentInstanceContext agentInstanceContext,
                                                     InfraOnMergeViewFactory factory) {
        this.rootView = rootView;
        this.agentInstanceContext = agentInstanceContext;
        this.factory = factory;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_MERGE, newData, CollectionUtil.EVENTBEANARRAY_EMPTY);

        if (newData == null) {
            agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
            return;
        }

        OneEventCollection newColl = new OneEventCollection();
        EventBean[] eventsPerStream = new EventBean[3]; // first:named window, second: trigger, third:before-update (optional)

        for (EventBean trigger : newData) {
            eventsPerStream[1] = trigger;
            factory.getOnMergeHelper().getInsertUnmatched().apply(null, eventsPerStream, newColl, null, agentInstanceContext);
            OnExprViewNamedWindowMerge.applyDelta(newColl, null, factory, rootView, agentInstanceContext, this);
            newColl.clear();
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }

    /**
     * returns expr context.
     *
     * @return context
     */
    public ExprEvaluatorContext getExprEvaluatorContext() {
        return agentInstanceContext;
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return Collections.emptyIterator();
    }
}
