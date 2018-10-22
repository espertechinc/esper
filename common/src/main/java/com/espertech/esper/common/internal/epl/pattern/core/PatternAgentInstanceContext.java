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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterConsumptionHandler;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterService;

import java.util.function.Function;

/**
 * Contains handles to implementations of services needed by evaluation nodes.
 */
public class PatternAgentInstanceContext {
    protected final PatternContext patternContext;
    protected final AgentInstanceContext agentInstanceContext;
    protected final EvalFilterConsumptionHandler consumptionHandler;
    protected final Function<FilterSpecActivatable, FilterValueSetParam[][]> contextAddendumFunction;

    public PatternAgentInstanceContext(PatternContext patternContext, AgentInstanceContext agentInstanceContext, boolean hasConsumingFilter, Function<FilterSpecActivatable, FilterValueSetParam[][]> contextAddendumFunction) {
        this.patternContext = patternContext;
        this.agentInstanceContext = agentInstanceContext;
        this.contextAddendumFunction = contextAddendumFunction;

        if (hasConsumingFilter) {
            consumptionHandler = new EvalFilterConsumptionHandler();
        } else {
            consumptionHandler = null;
        }
    }

    public PatternContext getPatternContext() {
        return patternContext;
    }

    public EvalFilterConsumptionHandler getConsumptionHandler() {
        return consumptionHandler;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public StatementContext getStatementContext() {
        return agentInstanceContext.getStatementContext();
    }

    public String getStatementName() {
        return agentInstanceContext.getStatementName();
    }

    public FilterService getFilterService() {
        return getStatementContext().getFilterService();
    }

    public long getTime() {
        return agentInstanceContext.getSchedulingService().getTime();
    }

    public FilterValueSetParam[][] getFilterAddendumForContextPath(FilterSpecActivatable filterSpec) {
        return contextAddendumFunction == null ? null : contextAddendumFunction.apply(filterSpec);
    }
}
