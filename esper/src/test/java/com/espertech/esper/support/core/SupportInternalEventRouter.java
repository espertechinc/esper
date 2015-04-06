/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.UpdateDesc;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

public class SupportInternalEventRouter implements InternalEventRouter
{
    private List<EventBean> routed  = new LinkedList<EventBean>();

    public List<EventBean> getRouted()
    {
        return routed;
    }

    public void reset()
    {
        routed.clear();
    }

    public InternalEventRouterDesc getValidatePreprocessing(EventType eventType, UpdateDesc desc, Annotation[] annotations) throws ExprValidationException {
        return null;
    }

    public void addPreprocessing(InternalEventRouterDesc internalEventRouterDesc, InternalRoutePreprocessView outputView, StatementAgentInstanceLock agentInstanceLock, boolean hasSubselect) {

    }

    public void removePreprocessing(EventType eventType, UpdateDesc desc) {
    }

    public void route(EventBean theEvent, EPStatementHandle statementHandle, InternalEventRouteDest routeDest, ExprEvaluatorContext exprEvaluatorContext, boolean addToFront) {
        routed.add(theEvent);
    }

    public boolean isHasPreprocessing()
    {
        return false;
    }

    public EventBean preprocess(EventBean theEvent, ExprEvaluatorContext engineFilterAndDispatchTimeContext)
    {
        return null;
    }

    public void setInsertIntoListener(InsertIntoListener insertIntoListener) {

    }
}
