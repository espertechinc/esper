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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventCollector;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventCollectorContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorLifecycle;

public class EventBusSinkOp implements DataFlowOperatorLifecycle {

    private final EventBusSinkFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final EPDataFlowEventCollector collector;

    public EventBusSinkOp(EventBusSinkFactory factory, AgentInstanceContext agentInstanceContext, EPDataFlowEventCollector collector) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
        this.collector = collector;
    }

    public void onInput(int port, Object data) {
        if (collector != null) {
            EPDataFlowEventCollectorContext holder = new EPDataFlowEventCollectorContext(agentInstanceContext.getEPRuntimeSendEvent(), data);
            collector.collect(holder);
        } else {
            if (data instanceof EventBean) {
                agentInstanceContext.getEPRuntimeEventProcessWrapped().processWrappedEvent((EventBean) data);
            } else {
                EventBean event = factory.getAdapterFactories()[port].makeAdapter(data);
                agentInstanceContext.getEPRuntimeEventProcessWrapped().processWrappedEvent(event);
            }
        }
    }

    public void open(DataFlowOpOpenContext openContext) {
        // no action
    }

    public void close(DataFlowOpCloseContext openContext) {
        // no action
    }
}
