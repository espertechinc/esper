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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.service.resource.StatementResourceService;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.EventTableIndexMetadata;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.lookup.IndexedPropDesc;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import java.util.*;

/**
 * An instance of this class is associated with a specific named window. The processor
 * provides the views to create-window, on-delete statements and statements selecting from a named window.
 */
public class NamedWindowProcessor {
    private final String namedWindowName;
    private final NamedWindowTailView tailView;
    private final NamedWindowRootView rootView;
    private final String contextName;
    private final EventType eventType;
    private final String eplExpression;
    private final String statementName;
    private final boolean isEnableSubqueryIndexShare;
    private final boolean isVirtualDataWindow;
    private final Set<String> optionalUniqueKeyProps;
    private final String eventTypeAsName;
    private final EventTableIndexMetadata eventTableIndexMetadataRepo = new EventTableIndexMetadata();
    private final StatementContext statementContextCreateWindow;

    public NamedWindowProcessor(String namedWindowName, NamedWindowMgmtService namedWindowMgmtService, NamedWindowDispatchService namedWindowDispatchService, String contextName, EventType eventType, StatementResultService statementResultService, ValueAddEventProcessor revisionProcessor, String eplExpression, String statementName, boolean isPrioritized, boolean isEnableSubqueryIndexShare, boolean enableQueryPlanLog, MetricReportingService metricReportingService, boolean isBatchingDataWindow, boolean isVirtualDataWindow, Set<String> optionalUniqueKeyProps, String eventTypeAsName, StatementContext statementContextCreateWindow) {
        this.namedWindowName = namedWindowName;
        this.contextName = contextName;
        this.eventType = eventType;
        this.eplExpression = eplExpression;
        this.statementName = statementName;
        this.isEnableSubqueryIndexShare = isEnableSubqueryIndexShare;
        this.isVirtualDataWindow = isVirtualDataWindow;
        this.optionalUniqueKeyProps = optionalUniqueKeyProps;
        this.eventTypeAsName = eventTypeAsName;
        this.statementContextCreateWindow = statementContextCreateWindow;

        rootView = new NamedWindowRootView(revisionProcessor, enableQueryPlanLog, metricReportingService, eventType, isBatchingDataWindow, isEnableSubqueryIndexShare, optionalUniqueKeyProps);
        tailView = namedWindowDispatchService.createTailView(eventType, namedWindowMgmtService, namedWindowDispatchService, statementResultService, revisionProcessor, isPrioritized, isBatchingDataWindow, contextName, statementContextCreateWindow.getTimeSourceService(), statementContextCreateWindow.getConfigSnapshot().getEngineDefaults().getThreading());
    }

    public String getEventTypeAsName() {
        return eventTypeAsName;
    }

    public synchronized NamedWindowProcessorInstance addInstance(AgentInstanceContext agentInstanceContext) {
        if (contextName == null) {
            return new NamedWindowProcessorInstance(null, this, agentInstanceContext);
        }

        int instanceId = agentInstanceContext.getAgentInstanceId();
        return new NamedWindowProcessorInstance(instanceId, this, agentInstanceContext);
    }

    public NamedWindowProcessorInstance getProcessorInstanceNoContext() {
        StatementResourceHolder holder = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources().getUnpartitioned();
        return holder == null ? null : holder.getNamedWindowProcessorInstance();
    }

    public NamedWindowProcessorInstance getProcessorInstance(int agentInstanceId) {
        StatementResourceHolder holder = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources().getPartitioned(agentInstanceId);
        return holder == null ? null : holder.getNamedWindowProcessorInstance();
    }

    public NamedWindowProcessorInstance getProcessorInstanceAllowUnpartitioned(int agentInstanceId) {
        if (agentInstanceId == EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID) {
            return getProcessorInstanceNoContext();
        }
        StatementResourceHolder holder = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources().getPartitioned(agentInstanceId);
        return holder == null ? null : holder.getNamedWindowProcessorInstance();
    }

    public synchronized Collection<Integer> getProcessorInstancesAll() {
        Set<Integer> keyset = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources().getResourcesPartitioned().keySet();
        return new ArrayDeque<Integer>(keyset);
    }

    public NamedWindowProcessorInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        if (contextName == null) {
            return getProcessorInstanceNoContext();
        }

        if (agentInstanceContext.getStatementContext().getContextDescriptor() == null) {
            return null;
        }

        if (this.contextName.equals(agentInstanceContext.getStatementContext().getContextDescriptor().getContextName())) {
            return getProcessorInstance(agentInstanceContext.getAgentInstanceId());
        }

        return null;
    }

    public String getContextName() {
        return contextName;
    }

    public NamedWindowConsumerView addConsumer(NamedWindowConsumerDesc consumerDesc, boolean isSubselect) {

        StatementResourceService statementResourceService = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources();

        // handle same-context consumer
        if (this.contextName != null) {
            ContextDescriptor contextDescriptor = consumerDesc.getAgentInstanceContext().getStatementContext().getContextDescriptor();
            if (contextDescriptor != null && contextName.equals(contextDescriptor.getContextName())) {
                StatementResourceHolder holder = statementResourceService.getPartitioned(consumerDesc.getAgentInstanceContext().getAgentInstanceId());
                return holder.getNamedWindowProcessorInstance().getTailViewInstance().addConsumer(consumerDesc, isSubselect);
            } else {
                // consumer is out-of-context
                return tailView.addConsumer(consumerDesc);  // non-context consumers
            }
        }

        // handle no context associated
        return statementResourceService.getResourcesUnpartitioned().getNamedWindowProcessorInstance().getTailViewInstance().addConsumer(consumerDesc, isSubselect);
    }

    public boolean isVirtualDataWindow() {
        return isVirtualDataWindow;
    }

    /**
     * Returns the tail view of the named window, hooked into the view chain after the named window's data window views,
     * as the last view.
     *
     * @return tail view
     */
    public NamedWindowTailView getTailView() {
        return tailView;    // hooked as the tail sview before any data windows
    }

    /**
     * Returns the root view of the named window, hooked into the view chain before the named window's data window views,
     * right after the filter stream that filters for insert-into events.
     *
     * @return tail view
     */
    public NamedWindowRootView getRootView() {
        return rootView;    // hooked as the top view before any data windows
    }

    /**
     * Returns the event type of the named window.
     *
     * @return event type
     */
    public EventType getNamedWindowType() {
        return eventType;
    }

    /**
     * Returns the EPL expression.
     *
     * @return epl
     */
    public String getEplExpression() {
        return eplExpression;
    }

    /**
     * Returns the statement name.
     *
     * @return name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Deletes a named window and removes any associated resources.
     */
    public void destroy() {
    }

    public boolean isEnableSubqueryIndexShare() {
        return isEnableSubqueryIndexShare;
    }

    public StatementMetricHandle getCreateNamedWindowMetricsHandle() {
        return statementContextCreateWindow.getEpStatementHandle().getMetricsHandle();
    }

    public String getNamedWindowName() {
        return namedWindowName;
    }

    public String[][] getUniqueIndexes() {
        List<String[]> unique = null;

        Set<IndexMultiKey> indexDescriptors = getEventTableIndexMetadataRepo().getIndexes().keySet();
        for (IndexMultiKey index : indexDescriptors) {
            if (!index.isUnique()) {
                continue;
            }
            String[] uniqueKeys = IndexedPropDesc.getIndexProperties(index.getHashIndexedProps());
            if (unique == null) {
                unique = new ArrayList<String[]>();
            }
            unique.add(uniqueKeys);
        }
        if (optionalUniqueKeyProps != null) {
            if (unique == null) {
                unique = new ArrayList<String[]>();
            }
            unique.add(optionalUniqueKeyProps.toArray(new String[optionalUniqueKeyProps.size()]));
        }
        if (unique == null) {
            return null;
        }
        return unique.toArray(new String[unique.size()][]);
    }

    public Set<String> getOptionalUniqueKeyProps() {
        return optionalUniqueKeyProps;
    }

    public EventTableIndexMetadata getEventTableIndexMetadataRepo() {
        return eventTableIndexMetadataRepo;
    }

    public StatementContext getStatementContextCreateWindow() {
        return statementContextCreateWindow;
    }

    public void removeAllInstanceIndexes(IndexMultiKey index) {
        StatementResourceService statementResourceService = statementContextCreateWindow.getStatementExtensionServicesContext().getStmtResources();

        if (contextName == null) {
            StatementResourceHolder holder = statementResourceService.getUnpartitioned();
            if (holder != null && holder.getNamedWindowProcessorInstance() != null) {
                holder.getNamedWindowProcessorInstance().removeIndex(index);
            }
        } else {
            for (Map.Entry<Integer, StatementResourceHolder> entry : statementResourceService.getResourcesPartitioned().entrySet()) {
                if (entry.getValue().getNamedWindowProcessorInstance() != null) {
                    entry.getValue().getNamedWindowProcessorInstance().removeIndex(index);
                }
            }
        }
    }

    public void validateAddIndex(String statementName, String explicitIndexName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey imk) throws ExprValidationException {
        eventTableIndexMetadataRepo.addIndexExplicit(false, imk, explicitIndexName, explicitIndexDesc, statementName);
    }

    public void removeIndexReferencesStmtMayRemoveIndex(IndexMultiKey imk, String finalStatementName) {
        boolean last = eventTableIndexMetadataRepo.removeIndexReference(imk, finalStatementName);
        if (last) {
            eventTableIndexMetadataRepo.removeIndex(imk);
            removeAllInstanceIndexes(imk);
        }
    }

    public void clearProcessorInstances() {
        if (contextName == null) {
            NamedWindowProcessorInstance instance = getProcessorInstanceNoContext();
            if (instance != null) {
                instance.destroy();
            }
            return;
        }
        Collection<Integer> cpids = getProcessorInstancesAll();
        for (int cpid : cpids) {
            NamedWindowProcessorInstance instance = getProcessorInstance(cpid);
            if (instance != null) {
                instance.destroy();
            }
            return;
        }
    }
}
