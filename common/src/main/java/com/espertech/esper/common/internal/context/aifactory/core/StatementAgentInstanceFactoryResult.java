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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.StatementAgentInstancePreload;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StatementAgentInstanceFactoryResult {
    private final Viewable finalView;
    private AgentInstanceStopCallback stopCallback;
    private final AgentInstanceContext agentInstanceContext;
    private final AggregationService optionalAggegationService;
    private final Map<Integer, SubSelectFactoryResult> subselectStrategies;
    private final PriorEvalStrategy[] priorStrategies;
    private final PreviousGetterStrategy[] previousGetterStrategies;
    private final RowRecogPreviousStrategy rowRecogPreviousStrategy;
    private final Map<Integer, ExprTableEvalStrategy> tableAccessStrategies;
    private final List<StatementAgentInstancePreload> preloadList;

    protected StatementAgentInstanceFactoryResult(Viewable finalView, AgentInstanceStopCallback stopCallback, AgentInstanceContext agentInstanceContext, AggregationService optionalAggegationService, Map<Integer, SubSelectFactoryResult> subselectStrategies, PriorEvalStrategy[] priorStrategies, PreviousGetterStrategy[] previousGetterStrategies, RowRecogPreviousStrategy rowRecogPreviousStrategy, Map<Integer, ExprTableEvalStrategy> tableAccessStrategies, List<StatementAgentInstancePreload> preloadList) {
        this.finalView = finalView;
        this.stopCallback = stopCallback;
        this.agentInstanceContext = agentInstanceContext;
        this.optionalAggegationService = optionalAggegationService;
        this.subselectStrategies = subselectStrategies;
        this.priorStrategies = priorStrategies;
        this.previousGetterStrategies = previousGetterStrategies;
        this.rowRecogPreviousStrategy = rowRecogPreviousStrategy;
        this.tableAccessStrategies = tableAccessStrategies;
        this.preloadList = preloadList;
    }

    public Viewable getFinalView() {
        return finalView;
    }

    public AgentInstanceStopCallback getStopCallback() {
        return stopCallback;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public AggregationService getOptionalAggegationService() {
        return optionalAggegationService;
    }

    public Map<Integer, SubSelectFactoryResult> getSubselectStrategies() {
        return subselectStrategies;
    }

    public PriorEvalStrategy[] getPriorStrategies() {
        return priorStrategies;
    }

    public PreviousGetterStrategy[] getPreviousGetterStrategies() {
        return previousGetterStrategies;
    }

    public Collection<StatementAgentInstancePreload> getPreloadList() {
        return preloadList;
    }

    public RowRecogPreviousStrategy getRowRecogPreviousStrategy() {
        return rowRecogPreviousStrategy;
    }

    public void setStopCallback(AgentInstanceStopCallback stopCallback) {
        this.stopCallback = stopCallback;
    }

    public Map<Integer, ExprTableEvalStrategy> getTableAccessStrategies() {
        return tableAccessStrategies;
    }
}
