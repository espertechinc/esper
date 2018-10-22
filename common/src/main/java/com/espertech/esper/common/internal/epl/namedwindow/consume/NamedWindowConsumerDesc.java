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
package com.espertech.esper.common.internal.epl.namedwindow.consume;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.filterspec.PropertyEvaluator;

public class NamedWindowConsumerDesc {
    private final int namedWindowConsumerId;
    private final ExprEvaluator filterEvaluator;
    private final PropertyEvaluator optPropertyEvaluator;
    private final AgentInstanceContext agentInstanceContext;

    public NamedWindowConsumerDesc(int namedWindowConsumerId, ExprEvaluator filterEvaluator, PropertyEvaluator optPropertyEvaluator, AgentInstanceContext agentInstanceContext) {
        this.namedWindowConsumerId = namedWindowConsumerId;
        this.filterEvaluator = filterEvaluator;
        this.optPropertyEvaluator = optPropertyEvaluator;
        this.agentInstanceContext = agentInstanceContext;
    }

    public int getNamedWindowConsumerId() {
        return namedWindowConsumerId;
    }

    public ExprEvaluator getFilterEvaluator() {
        return filterEvaluator;
    }

    public PropertyEvaluator getOptPropertyEvaluator() {
        return optPropertyEvaluator;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }
}
