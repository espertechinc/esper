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

public final class ExprNodeAdapterSSStmtLock extends ExprNodeAdapterBase {
    protected final VariableManagementService variableService;

    public ExprNodeAdapterSSStmtLock(FilterSpecParamExprNode factory, ExprEvaluatorContext evaluatorContext, VariableManagementService variableService) {
        super(factory, evaluatorContext);
        this.variableService = variableService;
    }

    public boolean evaluate(EventBean theEvent) {
        evaluatorContext.getAgentInstanceLock().acquireWriteLock();
        try {
            variableService.setLocalVersion();
            return evaluatePerStream(new EventBean[]{theEvent});
        } finally {
            evaluatorContext.getAgentInstanceLock().releaseWriteLock();
        }
    }
}
