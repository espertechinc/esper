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
package com.espertech.esper.runtime.internal.dataflow.op.eventbussink;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventCollector;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.event.core.EventBeanAdapterFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Map;

public class EventBusSinkFactory implements DataFlowOperatorFactory {

    private Map<String, Object> collector;
    private EventType[] eventTypes;
    private EventBeanAdapterFactory[] adapterFactories;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        adapterFactories = new EventBeanAdapterFactory[eventTypes.length];
        for (int i = 0; i < eventTypes.length; i++) {
            if (eventTypes[i] != null) {
                adapterFactories[i] = EventTypeUtility.getAdapterFactoryForType(eventTypes[i], context.getStatementContext().getEventBeanTypedEventFactory(),
                        context.getStatementContext().getEventTypeAvroHandler());
            }
        }
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        EPDataFlowEventCollector collectorInstance = DataFlowParameterResolution.resolveOptionalInstance("collector", collector, EPDataFlowEventCollector.class, context);
        return new EventBusSinkOp(this, context.getAgentInstanceContext(), collectorInstance);
    }

    public void setEventTypes(EventType[] eventTypes) {
        this.eventTypes = eventTypes;
    }

    public EventBeanAdapterFactory[] getAdapterFactories() {
        return adapterFactories;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public Map<String, Object> getCollector() {
        return collector;
    }
}
