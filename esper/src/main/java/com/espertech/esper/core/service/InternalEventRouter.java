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
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.UpdateDesc;

import java.lang.annotation.Annotation;

/**
 * Interface for a service that routes events within the engine for further processing.
 */
public interface InternalEventRouter {
    public InternalEventRouterDesc getValidatePreprocessing(EventType eventType, UpdateDesc desc, Annotation[] annotations, String statementName)
            throws ExprValidationException;

    public void addPreprocessing(InternalEventRouterDesc internalEventRouterDesc, InternalRoutePreprocessView outputView, StatementAgentInstanceLock agentInstanceLock, boolean hasSubselect);

    /**
     * Remove preprocessing.
     *
     * @param eventType type to remove for
     * @param desc      update statement specification
     */
    public void removePreprocessing(EventType eventType, UpdateDesc desc);

    /**
     * Route the event such that the event is processed as required.
     *
     * @param theEvent             to route
     * @param statementHandle      provides statement resources
     * @param exprEvaluatorContext context for expression evalauation
     * @param routeDest            routing destination
     * @param addToFront           indicator whether to add to front queue
     */
    public void route(EventBean theEvent, EPStatementHandle statementHandle, InternalEventRouteDest routeDest, ExprEvaluatorContext exprEvaluatorContext, boolean addToFront);

    public boolean isHasPreprocessing();

    public EventBean preprocess(EventBean theEvent, ExprEvaluatorContext engineFilterAndDispatchTimeContext);

    public void setInsertIntoListener(InsertIntoListener insertIntoListener);
}
