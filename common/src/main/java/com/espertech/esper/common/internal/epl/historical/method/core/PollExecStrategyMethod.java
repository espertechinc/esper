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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.execstrategy.PollExecStrategy;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodConversionStrategy;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodTargetStrategy;

import java.util.List;

public class PollExecStrategyMethod implements PollExecStrategy {
    private final MethodTargetStrategy methodTargetStrategy;
    private final MethodConversionStrategy methodConversionStrategy;

    public PollExecStrategyMethod(MethodTargetStrategy methodTargetStrategy, MethodConversionStrategy methodConversionStrategy) {
        this.methodTargetStrategy = methodTargetStrategy;
        this.methodConversionStrategy = methodConversionStrategy;
    }

    public void start() {
        // no action
    }

    public List<EventBean> poll(Object lookupValues, AgentInstanceContext agentInstanceContext) {
        Object result = methodTargetStrategy.invoke(lookupValues, agentInstanceContext);
        if (result != null) {
            return methodConversionStrategy.convert(result, methodTargetStrategy, agentInstanceContext);
        }
        return null;
    }

    public void done() {
        // no action
    }

    public void destroy() {
        // no action
    }
}
