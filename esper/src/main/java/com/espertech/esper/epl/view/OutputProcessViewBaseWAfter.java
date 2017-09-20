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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;

import java.util.Set;

public abstract class OutputProcessViewBaseWAfter extends OutputProcessViewBase {
    private final OutputProcessViewAfterState afterConditionState;

    protected OutputProcessViewBaseWAfter(ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor, Long afterConditionTime, Integer afterConditionNumberOfEvents, boolean afterConditionSatisfied) {
        super(resultSetProcessor);
        afterConditionState = resultSetProcessorHelperFactory.makeOutputConditionAfter(afterConditionTime, afterConditionNumberOfEvents, afterConditionSatisfied, agentInstanceContext);
    }

    public OutputProcessViewAfterState getOptionalAfterConditionState() {
        return afterConditionState;
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
    public boolean checkAfterCondition(Set<MultiKey<EventBean>> newEvents, StatementContext statementContext) {
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

    public void stop() {
        afterConditionState.destroy();
    }
}
