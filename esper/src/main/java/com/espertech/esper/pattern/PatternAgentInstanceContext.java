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
package com.espertech.esper.pattern;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.IdentityHashMap;

/**
 * Contains handles to implementations of services needed by evaluation nodes.
 */
public class PatternAgentInstanceContext {
    private final PatternContext patternContext;
    private final AgentInstanceContext agentInstanceContext;
    private final EvalFilterConsumptionHandler consumptionHandler;
    private final IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum;

    public PatternAgentInstanceContext(PatternContext patternContext, AgentInstanceContext agentInstanceContext, boolean hasConsumingFilter, IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum) {
        this.patternContext = patternContext;
        this.agentInstanceContext = agentInstanceContext;
        this.filterAddendum = filterAddendum;

        if (hasConsumingFilter) {
            consumptionHandler = new EvalFilterConsumptionHandler();
        } else {
            consumptionHandler = null;
        }
    }

    public PatternContext getPatternContext() {
        return patternContext;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public EvalFilterConsumptionHandler getConsumptionHandler() {
        return consumptionHandler;
    }

    public StatementContext getStatementContext() {
        return agentInstanceContext.getStatementContext();
    }

    public IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> getFilterAddendum() {
        return filterAddendum;
    }
}
