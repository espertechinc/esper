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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;

public final class ExprNodeAdapterMSStmtLock extends ExprNodeAdapterMSBase {
    public static final long LOCK_BACKOFF_MSEC = 10;
    private final VariableManagementService variableService;

    ExprNodeAdapterMSStmtLock(FilterSpecParamExprNode factory, ExprEvaluatorContext evaluatorContext, EventBean[] prototype, VariableManagementService variableService) {
        super(factory, evaluatorContext, prototype);
        this.variableService = variableService;
    }

    public boolean evaluate(EventBean theEvent) {
        EventBean[] eventsPerStream = new EventBean[prototypeArray.length];
        System.arraycopy(prototypeArray, 0, eventsPerStream, 0, prototypeArray.length);
        eventsPerStream[0] = theEvent;

        if (variableService != null) {
            variableService.setLocalVersion();
        }

        boolean obtained = evaluatorContext.getAgentInstanceLock().acquireWriteLock(LOCK_BACKOFF_MSEC);
        if (!obtained) {
            throw new FilterLockBackoffException();
        }
        try {
            return super.evaluatePerStream(eventsPerStream);
        } finally {
            evaluatorContext.getAgentInstanceLock().releaseWriteLock();
        }
    }
}
