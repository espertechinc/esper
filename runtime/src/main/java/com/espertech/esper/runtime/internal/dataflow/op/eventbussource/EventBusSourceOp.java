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
package com.espertech.esper.runtime.internal.dataflow.op.eventbussource;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollector;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollectorContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;
import com.espertech.esper.common.internal.filtersvc.FilterService;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;

public class EventBusSourceOp implements DataFlowSourceOperator, DataFlowOperatorLifecycle, FilterHandleCallback {

    private final EventBusSourceFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final EPDataFlowEventBeanCollector collector;

    @DataFlowContext
    protected EPDataFlowEmitter graphContext;

    protected LinkedBlockingDeque<Object> emittables = new LinkedBlockingDeque<Object>();

    public EventBusSourceOp(EventBusSourceFactory factory, AgentInstanceContext agentInstanceContext, EPDataFlowEventBeanCollector collector) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
        this.collector = collector;
    }

    public void next() throws InterruptedException {
        Object next = emittables.take();
        graphContext.submit(next);
    }

    public void open(DataFlowOpOpenContext openContext) {
        DataFlowFilterServiceAdapter adapter = agentInstanceContext.getDataFlowFilterServiceAdapter();
        FilterService filterService = agentInstanceContext.getFilterService();
        FilterValueSetParam[][] filterValues = FilterSpecActivatable.evaluateValueSet(factory.getFilterSpecActivatable().getParameters(), null, agentInstanceContext);
        adapter.addFilterCallback(this, agentInstanceContext, factory.getFilterSpecActivatable().getFilterForEventType(), filterValues, factory.getFilterSpecActivatable().getFilterCallbackId());
        long filtersVersion = filterService.getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        if (collector != null) {
            EPDataFlowEventBeanCollectorContext holder = new EPDataFlowEventBeanCollectorContext(graphContext, factory.isSubmitEventBean(), theEvent);
            collector.collect(holder);
        } else if (factory.isSubmitEventBean()) {
            emittables.add(theEvent);
        } else {
            emittables.add(theEvent.getUnderlying());
        }
    }

    public boolean isSubSelect() {
        return false;
    }

    public synchronized void close(DataFlowOpCloseContext closeContext) {
        DataFlowFilterServiceAdapter adapter = agentInstanceContext.getDataFlowFilterServiceAdapter();
        FilterService filterService = agentInstanceContext.getFilterService();
        FilterValueSetParam[][] filterValues = FilterSpecActivatable.evaluateValueSet(factory.getFilterSpecActivatable().getParameters(), null, agentInstanceContext);
        adapter.removeFilterCallback(this, agentInstanceContext, factory.getFilterSpecActivatable().getFilterForEventType(), filterValues, factory.getFilterSpecActivatable().getFilterCallbackId());
        long filtersVersion = filterService.getFiltersVersion();
        agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }
}
