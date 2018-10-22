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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerDesc;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerLatchFactory;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDeltaData;

import java.util.List;
import java.util.Map;

public interface NamedWindowTailView {
    NamedWindowConsumerLatchFactory makeLatchFactory();

    boolean isPrioritized();

    boolean isParentBatchWindow();

    StatementResultService getStatementResultService();

    void addDispatches(NamedWindowConsumerLatchFactory latchFactory, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumersInContext, NamedWindowDeltaData delta, AgentInstanceContext agentInstanceContext);

    EventType getEventType();

    NamedWindowConsumerView addConsumerNoContext(NamedWindowConsumerDesc consumerDesc);

    void removeConsumerNoContext(NamedWindowConsumerView namedWindowConsumerView);
}

