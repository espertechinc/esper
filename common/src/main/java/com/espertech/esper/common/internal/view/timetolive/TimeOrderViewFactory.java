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
package com.espertech.esper.common.internal.view.timetolive;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.IStreamSortRankRandomAccess;

/**
 * Factory for views for time-ordering events.
 */
public class TimeOrderViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected ExprEvaluator timestampEval;
    protected TimePeriodCompute timePeriodCompute;
    protected EventType eventType;
    protected int scheduleCallbackId;
    protected boolean isTimeToLive;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public RandomAccessByIndexGetter makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        TimePeriodProvide timePeriodProvide = timePeriodCompute.getNonVariableProvide(agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess sortedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new TimeOrderView(agentInstanceViewFactoryContext, this, sortedRandomAccess, timePeriodProvide);
    }

    public EventType getEventType() {
        return eventType;
    }

    public ExprEvaluator getTimestampEval() {
        return timestampEval;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setTimestampEval(ExprEvaluator timestampEval) {
        this.timestampEval = timestampEval;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public void setTimePeriodCompute(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public void setTimeToLive(boolean timeToLive) {
        isTimeToLive = timeToLive;
    }

    public String getViewName() {
        return isTimeToLive ? ViewEnum.TIMETOLIVE.getName() : ViewEnum.TIME_ORDER.getName();
    }
}
