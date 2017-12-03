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
package com.espertech.esper.epl.named;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.filterspec.PropertyEvaluator;

public class NamedWindowConsumerDesc {
    private final ExprNode[] filterExpressions;
    private final ExprEvaluator[] filterEvaluators;
    private final PropertyEvaluator optPropertyEvaluator;
    private final AgentInstanceContext agentInstanceContext;

    public NamedWindowConsumerDesc(ExprNode[] filterExpressions, ExprEvaluator[] filterEvaluators, PropertyEvaluator optPropertyEvaluator, AgentInstanceContext agentInstanceContext) {
        this.filterExpressions = filterExpressions;
        this.filterEvaluators = filterEvaluators;
        this.optPropertyEvaluator = optPropertyEvaluator;
        this.agentInstanceContext = agentInstanceContext;
    }

    public ExprNode[] getFilterExpressions() {
        return filterExpressions;
    }

    public ExprEvaluator[] getFilterEvaluators() {
        return filterEvaluators;
    }

    public PropertyEvaluator getOptPropertyEvaluator() {
        return optPropertyEvaluator;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }
}
