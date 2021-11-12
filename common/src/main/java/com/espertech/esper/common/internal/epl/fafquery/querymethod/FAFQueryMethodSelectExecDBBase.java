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

import java.util.Collection;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.*;

public abstract class FAFQueryMethodSelectExecDBBase implements FAFQueryMethodSelectExec {
    protected final StatementContextRuntimeServices services;

    public FAFQueryMethodSelectExecDBBase(StatementContextRuntimeServices services) {
        this.services = services;
    }

    protected abstract Collection<EventBean> executeInternal(ExprEvaluatorContext exprEvaluatorContext, FAFQueryMethodSelect select);

    public final EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        ExprEvaluatorContext exprEvaluatorContext = new FAFQueryMethodSelectNoFromExprEvaluatorContext(services, select);
        Collection<EventBean> rows = executeInternal(exprEvaluatorContext, select);
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), exprEvaluatorContext, null, assignerSetter, select.getTableAccesses(), select.getSubselects());
        if (select.getWhereClause() != null) {
            rows = filtered(rows, select.getWhereClause(), exprEvaluatorContext);
        }
        return processedNonJoin(resultSetProcessor, rows, select.getDistinctKeyGetter());
    }

    public final void releaseTableLocks(FireAndForgetProcessor[] processors) {
        services.getTableExprEvaluatorContext().releaseAcquiredLocks();
    }
}
