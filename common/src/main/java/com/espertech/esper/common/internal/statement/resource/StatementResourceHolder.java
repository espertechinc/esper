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
package com.espertech.esper.common.internal.statement.resource;

import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposer;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.Collections;
import java.util.Map;

public class StatementResourceHolder {
    private final AgentInstanceContext agentInstanceContext;
    private final AgentInstanceStopCallback agentInstanceStopCallback;
    private final Viewable finalView;
    private AggregationService aggregationService;
    private final PriorEvalStrategy[] priorEvalStrategies;
    private final PreviousGetterStrategy[] previousGetterStrategies;
    private final RowRecogPreviousStrategy rowRecogPreviousStrategy;

    private Viewable[] topViewables;
    private Viewable[] eventStreamViewables;
    private EvalRootState[] patternRoots;
    private Map<Integer, SubSelectFactoryResult> subselectStrategies = Collections.EMPTY_MAP;
    private Map<Integer, ExprTableEvalStrategy> tableAccessStrategies = Collections.EMPTY_MAP;
    private NamedWindowInstance namedWindowInstance;
    private TableInstance tableInstance;
    private StatementResourceExtension statementResourceExtension;
    private ContextManagerRealization contextManagerRealization;
    private JoinSetComposer joinSetComposer;

    public StatementResourceHolder(AgentInstanceContext agentInstanceContext, AgentInstanceStopCallback agentInstanceStopCallback, Viewable finalView, AggregationService aggregationService, PriorEvalStrategy[] priorEvalStrategies, PreviousGetterStrategy[] previousGetterStrategies, RowRecogPreviousStrategy rowRecogPreviousStrategy) {
        this.agentInstanceContext = agentInstanceContext;
        this.agentInstanceStopCallback = agentInstanceStopCallback;
        this.finalView = finalView;
        this.aggregationService = aggregationService;
        this.priorEvalStrategies = priorEvalStrategies;
        this.previousGetterStrategies = previousGetterStrategies;
        this.rowRecogPreviousStrategy = rowRecogPreviousStrategy;
    }

    public AgentInstanceStopCallback getAgentInstanceStopCallback() {
        return agentInstanceStopCallback;
    }

    protected void setTopViewables(Viewable[] topViewables) {
        this.topViewables = topViewables;
    }

    protected void setEventStreamViewables(Viewable[] eventStreamViewables) {
        this.eventStreamViewables = eventStreamViewables;
    }

    public Viewable getFinalView() {
        return finalView;
    }

    protected void setPatternRoots(EvalRootState[] patternRoots) {
        this.patternRoots = patternRoots;
    }

    protected void setAggregationService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public void setSubselectStrategies(Map<Integer, SubSelectFactoryResult> subselectStrategies) {
        this.subselectStrategies = subselectStrategies;
    }

    public void setNamedWindowInstance(NamedWindowInstance namedWindowInstance) {
        this.namedWindowInstance = namedWindowInstance;
    }

    public void setTableInstance(TableInstance tableInstance) {
        this.tableInstance = tableInstance;
    }

    public void setStatementResourceExtension(StatementResourceExtension statementResourceExtension) {
        this.statementResourceExtension = statementResourceExtension;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public Viewable[] getTopViewables() {
        return topViewables;
    }

    public Viewable[] getEventStreamViewables() {
        return eventStreamViewables;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public NamedWindowInstance getNamedWindowInstance() {
        return namedWindowInstance;
    }

    public TableInstance getTableInstance() {
        return tableInstance;
    }

    public Map<Integer, SubSelectFactoryResult> getSubselectStrategies() {
        return subselectStrategies;
    }

    public StatementResourceExtension getStatementResourceExtension() {
        return statementResourceExtension;
    }

    public PriorEvalStrategy[] getPriorEvalStrategies() {
        return priorEvalStrategies;
    }

    public void setContextManagerRealization(ContextManagerRealization contextManagerRealization) {
        this.contextManagerRealization = contextManagerRealization;
    }

    public ContextManagerRealization getContextManagerRealization() {
        return contextManagerRealization;
    }

    public PreviousGetterStrategy[] getPreviousGetterStrategies() {
        return previousGetterStrategies;
    }

    public Map<Integer, ExprTableEvalStrategy> getTableAccessStrategies() {
        return tableAccessStrategies;
    }

    public void setTableAccessStrategies(Map<Integer, ExprTableEvalStrategy> tableAccessStrategies) {
        this.tableAccessStrategies = tableAccessStrategies;
    }

    public RowRecogPreviousStrategy getRowRecogPreviousStrategy() {
        return rowRecogPreviousStrategy;
    }

    public EvalRootState[] getPatternRoots() {
        return patternRoots;
    }

    public JoinSetComposer getJoinSetComposer() {
        return joinSetComposer;
    }

    public void setJoinSetComposer(JoinSetComposer joinSetComposer) {
        this.joinSetComposer = joinSetComposer;
    }
}
