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
package com.espertech.esper.common.internal.context.aifactory.createvariable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.variable.core.VariableChangeCallback;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CreateVariableView extends ViewSupport implements VariableChangeCallback {
    private final StatementAgentInstanceFactoryCreateVariable parent;
    private final AgentInstanceContext agentInstanceContext;
    private final VariableReader reader;

    CreateVariableView(StatementAgentInstanceFactoryCreateVariable parent, AgentInstanceContext agentInstanceContext, VariableReader reader) {
        this.parent = parent;
        this.agentInstanceContext = agentInstanceContext;
        this.reader = reader;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // nothing to do
    }

    public void update(Object newValue, Object oldValue) {
        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
            String variableName = reader.getMetaData().getVariableName();

            Map<String, Object> valuesOld = new HashMap<String, Object>();
            valuesOld.put(variableName, oldValue);
            EventBean eventOld = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(valuesOld, parent.getStatementEventType());

            Map<String, Object> valuesNew = new HashMap<String, Object>();
            valuesNew.put(variableName, newValue);
            EventBean eventNew = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(valuesNew, parent.getStatementEventType());

            EventBean[] newDataToPost = new EventBean[]{eventNew};
            EventBean[] oldDataToPost = new EventBean[]{eventOld};
            child.update(newDataToPost, oldDataToPost);
        }
    }

    public EventType getEventType() {
        return parent.getResultSetProcessorFactoryProvider().getResultEventType();
    }

    public Iterator<EventBean> iterator() {
        Object value = reader.getValue();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(reader.getMetaData().getVariableName(), value);
        EventBean theEvent = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(values, getEventType());
        return new SingleEventIterator(theEvent);
    }
}
