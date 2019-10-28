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

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototype;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactory;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodUtil.initializeSubselects;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodSelect implements FAFQueryMethod {
    private Annotation[] annotations;
    private String contextName;
    private ExprEvaluator whereClause;
    private ExprEvaluator[] consumerFilters;
    private ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider;
    private FireAndForgetProcessor[] processors;
    private JoinSetComposerPrototype joinSetComposerPrototype;
    private QueryGraph queryGraph;
    private Map<Integer, ExprTableEvalStrategyFactory> tableAccesses;
    private boolean hasTableAccess;
    private boolean isDistinct;
    private EventPropertyValueGetter distinctKeyGetter;
    private Map<Integer, SubSelectFactory> subselects;

    private FAFQueryMethodSelectExec selectExec;

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public void setProcessors(FireAndForgetProcessor[] processors) {
        this.processors = processors;
    }

    public void setResultSetProcessorFactoryProvider(ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider) {
        this.resultSetProcessorFactoryProvider = resultSetProcessorFactoryProvider;
    }

    public void setWhereClause(ExprEvaluator whereClause) {
        this.whereClause = whereClause;
    }

    public void setJoinSetComposerPrototype(JoinSetComposerPrototype joinSetComposerPrototype) {
        this.joinSetComposerPrototype = joinSetComposerPrototype;
    }

    public void setConsumerFilters(ExprEvaluator[] consumerFilters) {
        this.consumerFilters = consumerFilters;
    }

    public void setQueryGraph(QueryGraph queryGraph) {
        this.queryGraph = queryGraph;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setTableAccesses(Map<Integer, ExprTableEvalStrategyFactory> tableAccesses) {
        this.tableAccesses = tableAccesses;
    }

    public void setHasTableAccess(boolean hasTableAccess) {
        this.hasTableAccess = hasTableAccess;
    }

    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }

    public void setDistinctKeyGetter(EventPropertyValueGetter distinctKeyGetter) {
        this.distinctKeyGetter = distinctKeyGetter;
    }

    /**
     * Returns the event type of the prepared statement.
     *
     * @return event type
     */
    public EventType getEventType() {
        return resultSetProcessorFactoryProvider.getResultEventType();
    }

    public void ready(StatementContextRuntimeServices svc) {
        boolean hasContext = false;
        for (int i = 0; i < processors.length; i++) {
            hasContext |= processors[i].getContextName() != null;
        }

        if (contextName == null) {
            if (processors.length == 1) {
                if (!hasContext) {
                    selectExec = FAFQueryMethodSelectExecNoContextNoJoin.INSTANCE;
                } else {
                    selectExec = FAFQueryMethodSelectExecSomeContextNoJoin.INSTANCE;
                }
            } else {
                if (!hasContext) {
                    selectExec = FAFQueryMethodSelectExecNoContextJoin.INSTANCE;
                } else {
                    selectExec = FAFQueryMethodSelectExecSomeContextJoin.INSTANCE;
                }
            }
        } else {
            if (processors.length != 1) {
                throw new UnsupportedOperationException("Context name is not supported in a join");
            }
            if (!hasContext) {
                throw new UnsupportedOperationException("Query target is unpartitioned");
            }
            selectExec = FAFQueryMethodSelectExecGivenContextNoJoin.INSTANCE;
        }

        if (!subselects.isEmpty()) {
            initializeSubselects(svc, annotations, subselects);
        }
    }

    public EPPreparedQueryResult execute(AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService) {
        if (!serviceStatusProvider.get()) {
            throw FAFQueryMethodUtil.runtimeDestroyed();
        }
        if (contextPartitionSelectors != null && contextPartitionSelectors.length != processors.length) {
            throw new IllegalArgumentException("The number of context partition selectors does not match the number of named windows or tables in the from-clause");
        }

        try {
            return selectExec.execute(this, contextPartitionSelectors, assignerSetter, contextManagementService);
        } finally {
            if (hasTableAccess) {
                processors[0].getStatementContext().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
        }
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public String getContextName() {
        return contextName;
    }

    public ExprEvaluator getWhereClause() {
        return whereClause;
    }

    public ExprEvaluator[] getConsumerFilters() {
        return consumerFilters;
    }

    public ResultSetProcessorFactoryProvider getResultSetProcessorFactoryProvider() {
        return resultSetProcessorFactoryProvider;
    }

    public FireAndForgetProcessor[] getProcessors() {
        return processors;
    }

    public JoinSetComposerPrototype getJoinSetComposerPrototype() {
        return joinSetComposerPrototype;
    }

    public QueryGraph getQueryGraph() {
        return queryGraph;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public FAFQueryMethodSelectExec getSelectExec() {
        return selectExec;
    }

    public Map<Integer, ExprTableEvalStrategyFactory> getTableAccesses() {
        return tableAccesses;
    }

    public EventPropertyValueGetter getDistinctKeyGetter() {
        return distinctKeyGetter;
    }

    public Map<Integer, SubSelectFactory> getSubselects() {
        return subselects;
    }

    public void setSubselects(Map<Integer, SubSelectFactory> subselects) {
        this.subselects = subselects;
    }
}
