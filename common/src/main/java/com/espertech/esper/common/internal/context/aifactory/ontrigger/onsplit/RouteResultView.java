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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.context.util.InternalEventRouter;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * View for processing split-stream syntax.
 */
public class RouteResultView extends ViewSupport {
    private final EventType eventType;
    private RouteResultViewHandler handler;
    private ExprEvaluatorContext exprEvaluatorContext;

    public RouteResultView(boolean isFirst,
                           EventType eventType,
                           EPStatementHandle epStatementHandle,
                           InternalEventRouter internalEventRouter,
                           TableInstance[] tableInstances,
                           OnSplitItemEval[] items,
                           ResultSetProcessor[] processors,
                           AgentInstanceContext agentInstanceContext) {
        this.exprEvaluatorContext = agentInstanceContext;
        this.eventType = eventType;
        if (isFirst) {
            handler = new RouteResultViewHandlerFirst(epStatementHandle, internalEventRouter, tableInstances, items, processors, agentInstanceContext);
        } else {
            handler = new RouteResultViewHandlerAll(epStatementHandle, internalEventRouter, tableInstances, items, processors, agentInstanceContext);
        }
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (newData == null) {
            return;
        }

        for (EventBean bean : newData) {
            boolean isHandled = handler.handle(bean, exprEvaluatorContext);

            if (!isHandled) {
                child.update(new EventBean[]{bean}, null);
            }
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}
