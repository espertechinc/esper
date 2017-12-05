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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.event.EventBeanReader;
import com.espertech.esper.event.EventBeanReaderDefaultImpl;
import com.espertech.esper.event.EventTypeSPI;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, but works with distinct and after-output policies
 */
public class OutputProcessViewDirectDistinctOrAfterFactory extends OutputProcessViewDirectFactory {
    private final boolean isDistinct;
    protected final ExprTimePeriod afterTimePeriod;
    protected final Integer afterConditionNumberOfEvents;

    private EventBeanReader eventBeanReader;

    public OutputProcessViewDirectDistinctOrAfterFactory(StatementContext statementContext, OutputStrategyPostProcessFactory postProcessFactory, ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, boolean distinct, ExprTimePeriod afterTimePeriod, Integer afterConditionNumberOfEvents, EventType resultEventType) {
        super(statementContext, postProcessFactory, resultSetProcessorHelperFactory);
        isDistinct = distinct;
        this.afterTimePeriod = afterTimePeriod;
        this.afterConditionNumberOfEvents = afterConditionNumberOfEvents;

        if (isDistinct) {
            if (resultEventType instanceof EventTypeSPI) {
                EventTypeSPI eventTypeSPI = (EventTypeSPI) resultEventType;
                eventBeanReader = eventTypeSPI.getReader();
            }
            if (eventBeanReader == null) {
                eventBeanReader = new EventBeanReaderDefaultImpl(resultEventType);
            }
        }
    }

    @Override
    public OutputProcessViewBase makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {

        boolean isAfterConditionSatisfied = true;
        Long afterConditionTime = null;
        if (afterConditionNumberOfEvents != null) {
            isAfterConditionSatisfied = false;
        } else if (afterTimePeriod != null) {
            isAfterConditionSatisfied = false;
            long delta = afterTimePeriod.nonconstEvaluator().deltaUseEngineTime(null, agentInstanceContext, agentInstanceContext.getTimeProvider());
            afterConditionTime = agentInstanceContext.getStatementContext().getTimeProvider().getTime() + delta;
        }

        if (super.postProcessFactory == null) {
            return new OutputProcessViewDirectDistinctOrAfter(resultSetProcessorHelperFactory, agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this);
        }
        OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
        return new OutputProcessViewDirectDistinctOrAfterPostProcess(resultSetProcessorHelperFactory, agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, postProcess);
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public EventBeanReader getEventBeanReader() {
        return eventBeanReader;
    }
}