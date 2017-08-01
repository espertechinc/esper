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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.timer.TimeSourceService;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This view is hooked into a named window's view chain as the last view and handles dispatching of named window
 * insert and remove stream results via {@link NamedWindowMgmtService} to consuming statements.
 */
public class NamedWindowTailView {
    protected final EventType eventType;
    protected final NamedWindowMgmtService namedWindowMgmtService;
    protected final NamedWindowDispatchService namedWindowDispatchService;
    protected final StatementResultService statementResultService;
    protected final ValueAddEventProcessor revisionProcessor;
    protected final boolean isPrioritized;
    protected final boolean isParentBatchWindow;
    protected volatile Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumersNonContext;  // handles as copy-on-write
    protected final TimeSourceService timeSourceService;
    protected final ConfigurationEngineDefaults.Threading threadingConfig;

    public NamedWindowTailView(EventType eventType, NamedWindowMgmtService namedWindowMgmtService, NamedWindowDispatchService namedWindowDispatchService, StatementResultService statementResultService, ValueAddEventProcessor revisionProcessor, boolean prioritized, boolean parentBatchWindow, TimeSourceService timeSourceService, ConfigurationEngineDefaults.Threading threadingConfig) {
        this.eventType = eventType;
        this.namedWindowMgmtService = namedWindowMgmtService;
        this.namedWindowDispatchService = namedWindowDispatchService;
        this.statementResultService = statementResultService;
        this.revisionProcessor = revisionProcessor;
        this.isPrioritized = prioritized;
        this.isParentBatchWindow = parentBatchWindow;
        this.consumersNonContext = NamedWindowUtil.createConsumerMap(isPrioritized);
        this.threadingConfig = threadingConfig;
        this.timeSourceService = timeSourceService;
    }

    /**
     * Returns true to indicate that the data window view is a batch view.
     *
     * @return true if batch view
     */
    public boolean isParentBatchWindow() {
        return isParentBatchWindow;
    }

    public EventType getEventType() {
        return eventType;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public NamedWindowMgmtService getNamedWindowMgmtService() {
        return namedWindowMgmtService;
    }

    public NamedWindowDispatchService getNamedWindowDispatchService() {
        return namedWindowDispatchService;
    }

    public boolean isPrioritized() {
        return isPrioritized;
    }

    public ValueAddEventProcessor getRevisionProcessor() {
        return revisionProcessor;
    }

    public Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> getConsumersNonContext() {
        return consumersNonContext;
    }

    public NamedWindowConsumerView addConsumer(NamedWindowConsumerDesc consumerDesc) {
        NamedWindowConsumerCallback consumerCallback = new NamedWindowConsumerCallback() {
            public Iterator<EventBean> getIterator() {
                throw new UnsupportedOperationException("Iterator not supported on named windows that have a context attached and when that context is not the same as the consuming statement's context");
            }

            public void stopped(NamedWindowConsumerView namedWindowConsumerView) {
                removeConsumer(namedWindowConsumerView);
            }
        };

        // Construct consumer view, allow a callback to this view to remove the consumer
        boolean audit = AuditEnum.STREAM.getAudit(consumerDesc.getAgentInstanceContext().getStatementContext().getAnnotations()) != null;
        NamedWindowConsumerView consumerView = new NamedWindowConsumerView(consumerDesc.getFilterEvaluators(), consumerDesc.getOptPropertyEvaluator(), eventType, consumerCallback, consumerDesc.getAgentInstanceContext(), audit);

        // Keep a list of consumer views per statement to accomodate joins and subqueries
        List<NamedWindowConsumerView> viewsPerStatements = consumersNonContext.get(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle());
        if (viewsPerStatements == null) {
            viewsPerStatements = new CopyOnWriteArrayList<NamedWindowConsumerView>();

            // avoid concurrent modification as a thread may currently iterate over consumers as its dispatching
            // without the engine lock
            Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> newConsumers = NamedWindowUtil.createConsumerMap(isPrioritized);
            newConsumers.putAll(consumersNonContext);
            newConsumers.put(consumerDesc.getAgentInstanceContext().getEpStatementAgentInstanceHandle(), viewsPerStatements);
            consumersNonContext = newConsumers;
        }
        viewsPerStatements.add(consumerView);

        return consumerView;
    }

    /**
     * Called by the consumer view to indicate it was stopped or destroyed, such that the
     * consumer can be deregistered and further dispatches disregard this consumer.
     *
     * @param namedWindowConsumerView is the consumer representative view
     */
    public void removeConsumer(NamedWindowConsumerView namedWindowConsumerView) {
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
