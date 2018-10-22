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
package com.espertech.esper.common.internal.view.timebatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetterImpl;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

/**
 * Factory for {@link TimeBatchView}.
 */
public class TimeBatchViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    /**
     * The reference point, or null if none supplied.
     */
    protected Long optionalReferencePoint;
    protected int scheduleCallbackId;
    protected TimePeriodCompute timePeriodCompute;
    protected EventType eventType;
    protected boolean isForceUpdate;
    protected boolean isStartEager;

    public void setOptionalReferencePoint(Long optionalReferencePoint) {
        this.optionalReferencePoint = optionalReferencePoint;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public void setTimePeriodCompute(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
    }

    public void setStartEager(boolean startEager) {
        isStartEager = startEager;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public PreviousGetterStrategy makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        TimePeriodProvide timePeriodProvide = timePeriodCompute.getNonVariableProvide(agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        if (agentInstanceViewFactoryContext.isRemoveStream()) {
            return new TimeBatchViewRStream(this, agentInstanceViewFactoryContext, timePeriodProvide);
        } else {
            return new TimeBatchView(this, agentInstanceViewFactoryContext, viewUpdatedCollection, timePeriodProvide);
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public Long getOptionalReferencePoint() {
        return optionalReferencePoint;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public boolean isStartEager() {
        return isStartEager;
    }

    public String getViewName() {
        return ViewEnum.TIME_BATCH.getName();
    }
}
