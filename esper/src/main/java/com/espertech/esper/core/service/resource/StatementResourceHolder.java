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
package com.espertech.esper.core.service.resource;

import com.espertech.esper.core.context.factory.StatementAgentInstancePostLoad;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.view.Viewable;

import java.util.Collections;
import java.util.Map;

public class StatementResourceHolder {
    private AgentInstanceContext agentInstanceContext;
    private Viewable[] topViewables;
    private Viewable[] eventStreamViewables;
    private EvalRootState[] patternRoots;
    private AggregationService aggregationService;
    private Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies = Collections.EMPTY_MAP;
    private StatementAgentInstancePostLoad postLoad;
    private NamedWindowProcessorInstance namedWindowProcessorInstance;
    private StatementResourceExtension statementResourceExtension;

    public StatementResourceHolder(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    protected void setTopViewables(Viewable[] topViewables) {
        this.topViewables = topViewables;
    }

    protected void setEventStreamViewables(Viewable[] eventStreamViewables) {
        this.eventStreamViewables = eventStreamViewables;
    }

    protected void setPatternRoots(EvalRootState[] patternRoots) {
        this.patternRoots = patternRoots;
    }

    protected void setAggregationService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    protected void setSubselectStrategies(Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies) {
        this.subselectStrategies = subselectStrategies;
    }

    protected void setPostLoad(StatementAgentInstancePostLoad postLoad) {
        this.postLoad = postLoad;
    }

    protected void setNamedWindowProcessorInstance(NamedWindowProcessorInstance namedWindowProcessorInstance) {
        this.namedWindowProcessorInstance = namedWindowProcessorInstance;
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

    public NamedWindowProcessorInstance getNamedWindowProcessorInstance() {
        return namedWindowProcessorInstance;
    }

    public StatementResourceExtension getStatementResourceExtension() {
        return statementResourceExtension;
    }
}
