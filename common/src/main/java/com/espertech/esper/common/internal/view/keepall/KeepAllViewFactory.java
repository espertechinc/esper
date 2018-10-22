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
package com.espertech.esper.common.internal.view.keepall;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

/**
 * Factory for keep-all-views.
 */
public class KeepAllViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected EventType eventType;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public PreviousGetterStrategy makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ViewUpdatedCollection randomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRandomAccess(agentInstanceViewFactoryContext);
        return new KeepAllView(agentInstanceViewFactoryContext, this, randomAccess);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return ViewEnum.KEEPALL_WINDOW.getName();
    }
}
