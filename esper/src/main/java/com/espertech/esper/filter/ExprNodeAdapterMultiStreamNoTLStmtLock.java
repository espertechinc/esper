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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.variable.VariableService;

import java.lang.annotation.Annotation;

public class ExprNodeAdapterMultiStreamNoTLStmtLock extends ExprNodeAdapterMultiStreamNoTL {
    public ExprNodeAdapterMultiStreamNoTLStmtLock(int filterSpecId, int filterSpecParamPathNum, ExprNode exprNode, ExprEvaluator exprEvaluator, ExprEvaluatorContext evaluatorContext, VariableService variableService, EngineImportService engineImportService, EventBean[] prototype, Annotation[] annotations) {
        super(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluator, evaluatorContext, variableService, engineImportService, prototype, annotations);
    }

    @Override
    protected boolean evaluatePerStream(EventBean[] eventsPerStream) {
        boolean obtained = evaluatorContext.getAgentInstanceLock().acquireWriteLock(ExprNodeAdapterMultiStreamStmtLock.LOCK_BACKOFF_MSEC);
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
