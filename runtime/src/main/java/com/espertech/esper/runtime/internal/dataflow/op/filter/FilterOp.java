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
package com.espertech.esper.runtime.internal.dataflow.op.filter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.common.internal.event.core.EventBeanSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class FilterOp implements DataFlowOperator {

    private static final Logger log = LoggerFactory.getLogger(FilterOp.class);

    private final FilterFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private EventBeanSPI theEvent;
    private EventBean[] eventsPerStream = new EventBean[1];

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public FilterOp(FilterFactory factory, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;

        theEvent = EventTypeUtility.getShellForType(factory.getEventType());
        eventsPerStream[0] = theEvent;
    }

    public void onInput(Object row) {
        if (log.isDebugEnabled()) {
            log.debug("Received row for filtering: " + Arrays.toString((Object[]) row));
        }

        if (!(row instanceof EventBeanSPI)) {
            theEvent.setUnderlying(row);
        } else {
            theEvent = (EventBeanSPI) row;
        }

        Boolean pass = (Boolean) factory.getFilter().evaluate(eventsPerStream, true, agentInstanceContext);
        if (pass != null && pass) {
            if (log.isDebugEnabled()) {
                log.debug("Submitting row " + Arrays.toString((Object[]) row));
            }

            if (factory.isSingleOutputPort()) {
                graphContext.submit(row);
            } else {
                graphContext.submitPort(0, row);
            }
        } else {
            if (!factory.isSingleOutputPort()) {
                graphContext.submitPort(1, row);
            }
        }
    }
}
