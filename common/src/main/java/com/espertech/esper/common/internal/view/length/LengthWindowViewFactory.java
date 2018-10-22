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
package com.espertech.esper.common.internal.view.length;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class LengthWindowViewFactory implements ViewFactory, DataWindowViewWithPrevious {

    protected ExprEvaluator size;
    protected EventType eventType;

    public void setSize(ExprEvaluator size) {
        this.size = size;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int sizeValue = ViewFactoryUtil.evaluateSizeParam(getViewName(), size, agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection randomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRandomAccess(agentInstanceViewFactoryContext);
        if (agentInstanceViewFactoryContext.isRemoveStream()) {
            return new LengthWindowViewRStream(agentInstanceViewFactoryContext, this, sizeValue);
        } else {
            return new LengthWindowView(agentInstanceViewFactoryContext, this, sizeValue, randomAccess);
        }
    }

    public PreviousGetterStrategy makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return ViewEnum.LENGTH_WINDOW.getName();
    }
}
