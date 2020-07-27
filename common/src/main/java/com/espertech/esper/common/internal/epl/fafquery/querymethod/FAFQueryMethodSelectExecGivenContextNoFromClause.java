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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.mgr.ContextDeployTimeResolver;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayDeque;
import java.util.Collection;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectExecUtil.processorWithAssign;

public class FAFQueryMethodSelectExecGivenContextNoFromClause implements FAFQueryMethodSelectExec {
    private final StatementContextRuntimeServices svc;
    private FAFQueryMethodSelectNoFromExprEvaluatorContext exprEvaluatorContext;

    public FAFQueryMethodSelectExecGivenContextNoFromClause(StatementContextRuntimeServices svc) {
        this.svc = svc;
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        if (contextPartitionSelectors != null && contextPartitionSelectors.length > 1) {
            throw new IllegalArgumentException("Fire-and-forget queries without a from-clause allow only a single context partition selector");
        }

        String contextDeploymentId = ContextDeployTimeResolver.resolveContextDeploymentId(select.getContextModuleName(), NameAccessModifier.PUBLIC, select.getContextName(), null, svc.getContextPathRegistry());
        ContextManager contextManager = contextManagementService.getContextManager(contextDeploymentId, select.getContextName());
        if (contextManager == null) {
            throw new EPException("Failed to find context manager for context '" + select.getContextName() + "'");
        }

        ContextPartitionSelector singleSelector = contextPartitionSelectors != null && contextPartitionSelectors.length > 0 ? contextPartitionSelectors[0] : ContextPartitionSelectorAll.INSTANCE;
        Collection<Integer> agentInstanceIds = contextManager.getRealization().getAgentInstanceIds(singleSelector);

        exprEvaluatorContext = new FAFQueryMethodSelectNoFromExprEvaluatorContext(svc, select);
        ResultSetProcessor resultSetProcessor = processorWithAssign(select.getResultSetProcessorFactoryProvider(), exprEvaluatorContext, null, assignerSetter, select.getTableAccesses(), select.getSubselects());

        ArrayDeque<EventBean> events = new ArrayDeque<>();
        EventBean[] input = new EventBean[]{null};
        for (Integer agentInstanceId : agentInstanceIds) {
            exprEvaluatorContext.setContextProperties(contextManager.getContextPropertiesEvent(agentInstanceId));

            if (select.getWhereClause() != null) {
                Object result = select.getWhereClause().evaluate(CollectionUtil.EVENTBEANARRAY_EMPTY, true, exprEvaluatorContext);
                if ((result == null) || (!((Boolean) result))) {
                    continue;
                }
            }

            UniformPair<EventBean[]> results = resultSetProcessor.processViewResult(input, null, true);
            if (results.getFirst() != null && results.getFirst().length > 0) {
                events.add(results.getFirst()[0]);
            }
        }

        EventBean[] result = events.toArray(new EventBean[0]);
        EventBean[] distinct = EventBeanUtility.getDistinctByProp(result, select.getDistinctKeyGetter());
        return new EPPreparedQueryResult(resultSetProcessor.getResultEventType(), distinct);
    }

    public void releaseTableLocks(FireAndForgetProcessor[] processors) {
        if (exprEvaluatorContext != null) {
            exprEvaluatorContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
        }
        exprEvaluatorContext = null;
    }
}
