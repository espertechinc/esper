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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

public interface ViewFactory {
    void setEventType(EventType eventType);

    EventType getEventType();

    void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services);

    View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext);

    String getViewName();
}
