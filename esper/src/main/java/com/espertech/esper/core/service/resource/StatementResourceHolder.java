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

import com.espertech.esper.core.context.factory.StatementAgentInstancePostLoad;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.view.Viewable;

import java.util.Map;

public class StatementResourceHolder {
    private final StatementAgentInstanceLock agentInstanceLock;
    private final Viewable[] topViewables;
    private final Viewable[] eventStreamViewables;
    private final EvalRootState[] patternRoots;
    private final AggregationService aggegationService;
    private final Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies;
    private final StatementAgentInstancePostLoad postLoad;

    public StatementResourceHolder(StatementAgentInstanceLock agentInstanceLock, Viewable[] topViewables, Viewable[] eventStreamViewables, EvalRootState[] patternRoots, AggregationService aggegationService, Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategies, StatementAgentInstancePostLoad postLoad) {
        this.agentInstanceLock = agentInstanceLock;
        this.topViewables = topViewables;
        this.eventStreamViewables = eventStreamViewables;
        this.patternRoots = patternRoots;
        this.aggegationService = aggegationService;
        this.subselectStrategies = subselectStrategies;
        this.postLoad = postLoad;
    }

    public StatementAgentInstanceLock getAgentInstanceLock() {
        return agentInstanceLock;
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

    public AggregationService getAggegationService() {
        return aggegationService;
    }

    public Map<ExprSubselectNode, SubSelectStrategyHolder> getSubselectStrategies() {
        return subselectStrategies;
    }

    public StatementAgentInstancePostLoad getPostLoad() {
        return postLoad;
    }
}
