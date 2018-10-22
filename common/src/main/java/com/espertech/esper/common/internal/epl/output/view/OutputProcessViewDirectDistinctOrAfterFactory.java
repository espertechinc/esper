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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.event.core.EventBeanReader;
import com.espertech.esper.common.internal.event.core.EventBeanReaderDefaultImpl;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;

/**
 * Output process view that does not enforce any output policies and may simply
 * hand over events to child views, but works with distinct and after-output policies
 */
public class OutputProcessViewDirectDistinctOrAfterFactory extends OutputProcessViewDirectFactory {
    private final boolean isDistinct;
    protected final TimePeriodCompute afterTimePeriod;
    protected final Integer afterConditionNumberOfEvents;

    private EventBeanReader eventBeanReader;

    public OutputProcessViewDirectDistinctOrAfterFactory(OutputStrategyPostProcessFactory postProcessFactory, boolean distinct, TimePeriodCompute afterTimePeriod, Integer afterConditionNumberOfEvents, EventType resultEventType) {
        super(postProcessFactory);
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
    public OutputProcessView makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {

        boolean isAfterConditionSatisfied = true;
        Long afterConditionTime = null;
        if (afterConditionNumberOfEvents != null) {
            isAfterConditionSatisfied = false;
        } else if (afterTimePeriod != null) {
            isAfterConditionSatisfied = false;
            long time = agentInstanceContext.getTimeProvider().getTime();
            long delta = afterTimePeriod.deltaAdd(time, null, true, agentInstanceContext);
            afterConditionTime = time + delta;
        }

        if (super.postProcessFactory == null) {
            return new OutputProcessViewDirectDistinctOrAfter(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this);
        }
        OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
        return new OutputProcessViewDirectDistinctOrAfterPostProcess(agentInstanceContext, resultSetProcessor, afterConditionTime, afterConditionNumberOfEvents, isAfterConditionSatisfied, this, postProcess);
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public EventBeanReader getEventBeanReader() {
        return eventBeanReader;
    }
}