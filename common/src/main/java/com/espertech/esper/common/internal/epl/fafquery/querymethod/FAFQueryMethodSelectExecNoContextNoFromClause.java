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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.util.CollectionUtil;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.processedNonJoin;
import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.processorWithAssign;

public class FAFQueryMethodSelectExecNoContextNoFromClause implements FAFQueryMethodSelectExec {
    private final StatementContextRuntimeServices svc;
    private ExprEvaluatorContext exprEvaluatorContext;

    public FAFQueryMethodSelectExecNoContextNoFromClause(StatementContextRuntimeServices svc) {
        this.svc = svc;
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        if (select.getWhereClause() != null) {
            Object result = select.getWhereClause().evaluate(CollectionUtil.EVENTBEANARRAY_EMPTY, true, exprEvaluatorContext);
            if ((result == null) || (!((Boolean) result))) {
                return EPPreparedQueryResult.empty(select.getResultSetProcessorFactoryProvider().getResultEventType());
            }
        }

        exprEvaluatorContext = new FAFQueryMethodSelectNoFromExprEvaluatorContext(svc, select);
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), exprEvaluatorContext, null, assignerSetter, select.getTableAccesses(), select.getSubselects());
        return processedNonJoin(resultSetProcessor, new EventBean[]{null}, select.getDistinctKeyGetter());
    }

    public void releaseTableLocks(FireAndForgetProcessor[] processors) {
        if (exprEvaluatorContext != null) {
            exprEvaluatorContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
        }
        exprEvaluatorContext = null;
    }
}
