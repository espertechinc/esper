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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewConditionDefault;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewConditionFactory;
import com.espertech.esper.common.internal.epl.output.view.OutputStrategyPostProcess;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

/**
 * A view that prepares output events, batching incoming
 * events and invoking the result set processor as necessary.
 * <p>
 * Handles output rate limiting or stabilizing.
 */
public class OutputProcessViewConditionDefaultPostProcess extends OutputProcessViewConditionDefault {
    private final OutputStrategyPostProcess postProcessor;

    public OutputProcessViewConditionDefaultPostProcess(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext, OutputStrategyPostProcess postProcessor, boolean isJoin, EventType[] eventTypes) {
        super(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied, parent, agentInstanceContext, isJoin, eventTypes);
        this.postProcessor = postProcessor;
    }

    @Override
    public void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
        // Child view can be null in replay from named window
        if (child != null) {
            postProcessor.output(forceUpdate, results, child);
        }
    }
}
