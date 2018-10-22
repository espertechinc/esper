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
package com.espertech.esper.common.internal.view.lengthbatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetterImpl;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class LengthBatchViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    /**
     * The length window size.
     */
    protected ExprEvaluator size;

    protected EventType eventType;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public PreviousGetterStrategy makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int size = ViewFactoryUtil.evaluateSizeParam(getViewName(), this.size, agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        if (agentInstanceViewFactoryContext.isRemoveStream()) {
            return new LengthBatchViewRStream(agentInstanceViewFactoryContext, this, size);
        } else {
            return new LengthBatchView(agentInstanceViewFactoryContext, this, size, viewUpdatedCollection);
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setSize(ExprEvaluator size) {
        this.size = size;
    }

    public String getViewName() {
        return ViewEnum.LENGTH_BATCH.getName();
    }
}
