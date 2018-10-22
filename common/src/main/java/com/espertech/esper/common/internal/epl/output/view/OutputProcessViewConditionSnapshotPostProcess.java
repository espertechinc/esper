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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewConditionSnapshot;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionSnapshotPostProcess extends OutputProcessViewConditionSnapshot {
    private final OutputStrategyPostProcess postProcessor;

    public OutputProcessViewConditionSnapshotPostProcess(ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied, OutputProcessViewConditionFactory parent, AgentInstanceContext agentInstanceContext, OutputStrategyPostProcess postProcessor) {
        super(resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied, parent, agentInstanceContext);
        this.postProcessor = postProcessor;
    }

    @Override
    public void output(boolean forceUpdate, UniformPair<EventBean[]> results) {
        if (child != null) {
            postProcessor.output(forceUpdate, results, child);
        }
    }
}
