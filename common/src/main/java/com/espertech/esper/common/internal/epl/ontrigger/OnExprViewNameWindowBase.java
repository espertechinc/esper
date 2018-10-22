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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.view.core.ViewSupport;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public abstract class OnExprViewNameWindowBase extends ViewSupport {
    /**
     * The event type of the events hosted in the named window.
     */
    private final SubordWMatchExprLookupStrategy lookupStrategy;
    protected final AgentInstanceContext agentInstanceContext;

    /**
     * The root view accepting removals (old data).
     */
    protected final NamedWindowRootViewInstance rootView;

    /**
     * Ctor.
     *
     * @param lookupStrategy       for handling trigger events to determine deleted events
     * @param rootView             to indicate which events to delete
     * @param agentInstanceContext context for expression evalauation
     */
    public OnExprViewNameWindowBase(SubordWMatchExprLookupStrategy lookupStrategy,
                                    NamedWindowRootViewInstance rootView,
                                    AgentInstanceContext agentInstanceContext) {
        this.lookupStrategy = lookupStrategy;
        this.rootView = rootView;
        this.agentInstanceContext = agentInstanceContext;
    }

    /**
     * Implemented by on-trigger views to action on the combination of trigger and matching events in the named window.
     *
     * @param triggerEvents  is the trigger events (usually 1)
     * @param matchingEvents is the matching events retrieved via lookup strategy
     */
    public abstract void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents);

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (newData == null) {
            return;
        }

        if (newData.length == 1) {
            process(newData);
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (EventBean event : newData) {
            eventsPerStream[0] = event;
            process(eventsPerStream);
        }
    }

    /**
     * returns expr context.
     *
     * @return context
     */
    public ExprEvaluatorContext getExprEvaluatorContext() {
        return agentInstanceContext;
    }

    private void process(EventBean[] events) {
        EventBean[] eventsFound = lookupStrategy.lookup(events, agentInstanceContext);
        handleMatching(events, eventsFound);
    }
}
