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
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;

public class OutputProcessViewDirectPostProcess extends OutputProcessViewDirect {
    private final OutputStrategyPostProcess postProcessor;

    public OutputProcessViewDirectPostProcess(AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor, OutputStrategyPostProcess postProcessor) {
        super(agentInstanceContext, resultSetProcessor);
        this.postProcessor = postProcessor;
    }

    @Override
    protected void postProcess(boolean force, UniformPair<EventBean[]> newOldEvents, UpdateDispatchView childView) {
        postProcessor.output(force, newOldEvents, childView);
    }
}
