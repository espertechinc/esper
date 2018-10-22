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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.namedwindow.consume.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This view is hooked into a named window's view chain as the last view and handles dispatching of named window
 * insert and remove stream results via {@link NamedWindowManagementService} to consuming statements.
 */
public class NamedWindowTailViewImpl extends NamedWindowTailViewBase {

    private volatile Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumersNonContext;  // handles as copy-on-write

    public NamedWindowTailViewImpl(EventType eventType, boolean isParentBatchWindow, EPStatementInitServices services) {
        super(eventType, isParentBatchWindow, services);
        this.consumersNonContext = NamedWindowUtil.createConsumerMap(isPrioritized);
    }

    public Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> getConsumersNonContext() {
        return consumersNonContext;
    }

    public NamedWindowConsumerView addConsumerNoContext(NamedWindowConsumerDesc consumerDesc) {
        NamedWindowConsumerCallback consumerCallback = new NamedWindowConsumerCallback() {
            public Iterator<EventBean> getIterator() {
                throw new UnsupportedOperationException("Iterator not supported on named windows that have a context attached and when that context is not the same as the consuming statement's context");
            }

            public boolean isParentBatchWindow() {
                return isParentBatchWindow;
            }

            public Collection<EventBean> snapshot(QueryGraph queryGraph, Annotation[] annotations) {
                return Collections.emptyList();
            }

            public void stopped(NamedWindowConsumerView namedWindowConsumerView) {
                removeConsumerNoContext(namedWindowConsumerView);
            }
        };

        // Construct consumer view, allow a callback to this view to remove the consumer
        boolean audit = AuditEnum.STREAM.getAudit(consumerDesc.getAgentInstanceContext().getStatementContext().getAnnotations()) != null;
        NamedWindowConsumerView consumerView = new NamedWindowConsumerView(consumerDesc.getNamedWindowConsumerId(), consumerDesc.getFilterEvaluator(), consumerDesc.getOptPropertyEvaluator(), eventType, consumerCallback, consumerDesc.getAgentInstanceContext(), audit);

        // Keep a list of consumer views per statement to accomodate joins and subqueries
        List<NamedWindowConsumerView> viewsPerStatements = consumersNonContext.get(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle());
        if (viewsPerStatements == null) {
            viewsPerStatements = new CopyOnWriteArrayList<NamedWindowConsumerView>();

            // avoid concurrent modification as a thread may currently iterate over consumers as its dispatching
            // without the runtimelock
            Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> newConsumers = NamedWindowUtil.createConsumerMap(isPrioritized);
            newConsumers.putAll(consumersNonContext);
            newConsumers.put(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle(), viewsPerStatements);
            consumersNonContext = newConsumers;
        }
        viewsPerStatements.add(consumerView);

        return consumerView;
    }

    public void removeConsumerNoContext(NamedWindowConsumerView namedWindowConsumerView) {
        EPStatementAgentInstanceHandle handleRemoved = null;
        // Find the consumer view
        for (Map.Entry<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entry : consumersNonContext.entrySet()) {
            boolean foundAndRemoved = entry.getValue().remove(namedWindowConsumerView);
            // Remove the consumer view
            if (foundAndRemoved && (entry.getValue().size() == 0)) {
                // Remove the handle if this list is now empty
                handleRemoved = entry.getKey();
                break;
            }
        }
        if (handleRemoved != null) {
            Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> newConsumers = NamedWindowUtil.createConsumerMap(isPrioritized);
            newConsumers.putAll(consumersNonContext);
            newConsumers.remove(handleRemoved);
            consumersNonContext = newConsumers;
        }
    }

    public void addDispatches(NamedWindowConsumerLatchFactory latchFactory, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumersInContext, NamedWindowDeltaData delta, AgentInstanceContext agentInstanceContext) {
        if (!consumersInContext.isEmpty()) {
            namedWindowDispatchService.addDispatch(latchFactory, delta, consumersInContext);
        }
        if (!consumersNonContext.isEmpty()) {
            namedWindowDispatchService.addDispatch(latchFactory, delta, consumersNonContext);
        }
    }

    public NamedWindowConsumerLatchFactory makeLatchFactory() {
        return new NamedWindowConsumerLatchFactory(eventType.getName(),
                threadingConfig.isNamedWindowConsumerDispatchPreserveOrder(),
                threadingConfig.getNamedWindowConsumerDispatchTimeout(),
                threadingConfig.getNamedWindowConsumerDispatchLocking(), timeSourceService, true);
    }
}
