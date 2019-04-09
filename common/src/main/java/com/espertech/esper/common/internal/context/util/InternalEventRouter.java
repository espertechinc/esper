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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.update.InternalEventRouterDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

/**
 * Interface for a service that routes events within the runtimefor further processing.
 */
public interface InternalEventRouter {
    void addPreprocessing(InternalEventRouterDesc internalEventRouterDesc, InternalRoutePreprocessView outputView, StatementContext statementContext, boolean hasSubselect);

    void removePreprocessing(EventType eventType, InternalEventRouterDesc desc);

    /**
     * Route the event such that the event is processed as required.
     *
     * @param theEvent             to route
     * @param agentInstanceContext agentInstanceContext
     * @param addToFront           indicator whether to add to front queue
     */
    void route(EventBean theEvent, AgentInstanceContext agentInstanceContext, boolean addToFront);

    boolean isHasPreprocessing();

    EventBean preprocess(EventBean theEvent, ExprEvaluatorContext runtimeFilterAndDispatchTimeContext, InstrumentationCommon instrumentation);

    void setInsertIntoListener(InsertIntoListener insertIntoListener);
}
