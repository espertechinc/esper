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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onset;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A view that handles the setting of variables upon receipt of a triggering event.
 * <p>
 * Variables are updated atomically and thus a separate commit actually updates the
 * new variable values, or a rollback if an exception occured during validation.
 */
public class OnSetVariableView extends ViewSupport {
    private final StatementAgentInstanceFactoryOnTriggerSet factory;
    private final AgentInstanceContext agentInstanceContext;

    private final EventBean[] eventsPerStream = new EventBean[1];

    public OnSetVariableView(StatementAgentInstanceFactoryOnTriggerSet factory, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if ((newData == null) || (newData.length == 0)) {
            return;
        }

        Map<String, Object> values = null;
        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean produceOutputEvents = statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic();

        if (produceOutputEvents) {
            values = new HashMap<>();
        }

        eventsPerStream[0] = newData[newData.length - 1];
        factory.getVariableReadWrite().writeVariables(eventsPerStream, values, agentInstanceContext);

        if (values != null) {
            EventBean[] newDataOut = new EventBean[1];
            newDataOut[0] = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(values, factory.getStatementEventType());
            child.update(newDataOut, null);
        }
    }

    public EventType getEventType() {
        return factory.getStatementEventType();
    }

    public Iterator<EventBean> iterator() {
        Map<String, Object> values = factory.getVariableReadWrite().iterate(agentInstanceContext.getVariableManagementService(), agentInstanceContext.getAgentInstanceId());
        EventBean theEvent = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(values, factory.getStatementEventType());
        return new SingleEventIterator(theEvent);
    }
}
