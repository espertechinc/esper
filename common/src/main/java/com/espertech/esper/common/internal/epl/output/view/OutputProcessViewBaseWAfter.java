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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewWithAfter;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

import java.util.Set;

public abstract class OutputProcessViewBaseWAfter extends OutputProcessView implements OutputProcessViewWithAfter {
    private final OutputProcessViewAfterState afterConditionState;
    protected final AgentInstanceContext agentInstanceContext;
    protected final ResultSetProcessor resultSetProcessor;

    protected OutputProcessViewBaseWAfter(AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied) {
        this.resultSetProcessor = resultSetProcessor;
        this.agentInstanceContext = agentInstanceContext;
        this.afterConditionState = agentInstanceContext.getResultSetProcessorHelperFactory().makeOutputConditionAfter(afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied, agentInstanceContext);
    }

    public OutputProcessViewAfterState getOptionalAfterConditionState() {
        return afterConditionState;
    }

    public EventType getEventType() {
        return resultSetProcessor.getResultEventType();
    }

    /**
     * Returns true if the after-condition is satisfied.
     *
     * @param newEvents        is the view new events
     * @param statementContext context
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(EventBean[] newEvents, StatementContext statementContext) {
        return afterConditionState.checkUpdateAfterCondition(newEvents, statementContext);
    }

    /**
     * Returns true if the after-condition is satisfied.
     *
     * @param newEvents        is the join new events
     * @param statementContext context
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, StatementContext statementContext) {
        return afterConditionState.checkUpdateAfterCondition(newEvents, statementContext);
    }

    /**
     * Returns true if the after-condition is satisfied.
     *
     * @param newOldEvents     is the new and old events pair
     * @param statementContext context
     * @return indicator for output condition
     */
    public boolean checkAfterCondition(UniformPair<EventBean[]> newOldEvents, StatementContext statementContext) {
        return afterConditionState.checkUpdateAfterCondition(newOldEvents, statementContext);
    }

    public void stop(AgentInstanceStopServices services) {
        afterConditionState.destroy();
    }
}
