/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.service.resource;

import com.espertech.esper.core.context.factory.*;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.view.Viewable;

import java.util.Map;

public class StatementResourceHolder {
    private AgentInstanceContext agentInstanceContext;
    private Viewable[] topViewables;
    private Viewable[] eventStreamViewables;
    private EvalRootState[] patternRoots;
    private AggregationService aggregationService;
    private Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies;
    private StatementAgentInstancePostLoad postLoad;
    private NamedWindowProcessorInstance namedWindowProcessorInstance;
    private StatementResourceExtension statementResourceExtension;

    public StatementResourceHolder() {
    }

    public NamedWindowProcessorInstance getNamedWindowProcessorInstance() {
        return namedWindowProcessorInstance;
    }

    public void setNamedWindowProcessorInstance(NamedWindowProcessorInstance namedWindowProcessorInstance) {
        this.namedWindowProcessorInstance = namedWindowProcessorInstance;
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

    public EvalRootState[] getPatternRoots() {
        return patternRoots;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public Map<ExprSubselectNode, SubSelectStrategyHolder> getSubselectStrategies() {
        return subselectStrategies;
    }

    public StatementAgentInstancePostLoad getPostLoad() {
        return postLoad;
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public void setTopViewables(Viewable[] topViewables) {
        this.topViewables = topViewables;
    }

    public void setEventStreamViewables(Viewable[] eventStreamViewables) {
        this.eventStreamViewables = eventStreamViewables;
    }

    public void setPatternRoots(EvalRootState[] patternRoots) {
        this.patternRoots = patternRoots;
    }

    public void setAggregationService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public void setSubselectStrategies(Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies) {
        this.subselectStrategies = subselectStrategies;
    }

    public void setPostLoad(StatementAgentInstancePostLoad postLoad) {
        this.postLoad = postLoad;
    }

    public void addResources(StatementAgentInstanceFactoryResult startResult) {
        agentInstanceContext = startResult.getAgentInstanceContext();

        if (startResult instanceof StatementAgentInstanceFactorySelectResult) {
            StatementAgentInstanceFactorySelectResult selectResult = (StatementAgentInstanceFactorySelectResult) startResult;
            topViewables = selectResult.getTopViews();
            eventStreamViewables = selectResult.getEventStreamViewables();
            patternRoots = selectResult.getPatternRoots();
            aggregationService = selectResult.getOptionalAggegationService();
            subselectStrategies = selectResult.getSubselectStrategies();
            postLoad = selectResult.getOptionalPostLoadJoin();
        }
        else if (startResult instanceof StatementAgentInstanceFactoryCreateWindowResult) {
            StatementAgentInstanceFactoryCreateWindowResult createResult = (StatementAgentInstanceFactoryCreateWindowResult) startResult;
            topViewables = new Viewable[] {createResult.getTopView()};
            postLoad = createResult.getPostLoad();
        }
        else if (startResult instanceof StatementAgentInstanceFactoryCreateTableResult) {
            StatementAgentInstanceFactoryCreateTableResult createResult = (StatementAgentInstanceFactoryCreateTableResult) startResult;
            topViewables = new Viewable[] {createResult.getFinalView()};
            aggregationService = createResult.getOptionalAggegationService();
        }
        else if (startResult instanceof StatementAgentInstanceFactoryOnTriggerResult) {
            StatementAgentInstanceFactoryOnTriggerResult onTriggerResult = (StatementAgentInstanceFactoryOnTriggerResult) startResult;
            patternRoots = new EvalRootState[] {onTriggerResult.getOptPatternRoot()};
            aggregationService = onTriggerResult.getOptionalAggegationService();
            subselectStrategies = onTriggerResult.getSubselectStrategies();
        }
    }

    public StatementResourceExtension getStatementResourceExtension() {
        return statementResourceExtension;
    }

    public void setStatementResourceExtension(StatementResourceExtension statementResourceExtension) {
        this.statementResourceExtension = statementResourceExtension;
    }
}
