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
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.property.PropertyEvaluator;

import java.util.List;

public class NamedWindowConsumerDesc {
    private final List<ExprNode> filterList;
    private final PropertyEvaluator optPropertyEvaluator;
    private final AgentInstanceContext agentInstanceContext;

    public NamedWindowConsumerDesc(List<ExprNode> filterList, PropertyEvaluator optPropertyEvaluator, AgentInstanceContext agentInstanceContext) {
        this.filterList = filterList;
        this.optPropertyEvaluator = optPropertyEvaluator;
        this.agentInstanceContext = agentInstanceContext;
    }

    public List<ExprNode> getFilterList() {
        return filterList;
    }

    public PropertyEvaluator getOptPropertyEvaluator() {
        return optPropertyEvaluator;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }
}
