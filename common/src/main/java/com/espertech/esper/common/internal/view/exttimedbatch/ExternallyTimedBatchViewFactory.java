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
package com.espertech.esper.common.internal.view.exttimedbatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetterImpl;
import com.espertech.esper.common.internal.view.core.*;

public class ExternallyTimedBatchViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected EventType eventType;
    protected ExprEvaluator timestampEval;
    protected Long optionalReferencePoint;
    protected TimePeriodCompute timePeriodCompute;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public RelativeAccessByEventNIndexGetterImpl makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        TimePeriodProvide timePeriodProvide = timePeriodCompute.getNonVariableProvide(agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new ExternallyTimedBatchView(this, viewUpdatedCollection, agentInstanceViewFactoryContext, timePeriodProvide);
    }

    public EventType getEventType() {
        return eventType;
    }

    public Long getOptionalReferencePoint() {
        return optionalReferencePoint;
    }

    public ExprEvaluator getTimestampEval() {
        return timestampEval;
    }

    public void setTimestampEval(ExprEvaluator timestampEval) {
        this.timestampEval = timestampEval;
    }

    public void setOptionalReferencePoint(Long optionalReferencePoint) {
        this.optionalReferencePoint = optionalReferencePoint;
    }

    public void setTimePeriodCompute(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public String getViewName() {
        return ViewEnum.EXT_TIMED_BATCH.getName();
    }
}
