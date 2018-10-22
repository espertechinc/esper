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

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.ContextRuntimeDescriptor;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerDesc;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;

import java.util.Map;

public class NamedWindowImpl implements NamedWindowWDirectConsume {
    private final NamedWindowRootView rootView;
    private final NamedWindowTailView tailView;
    private final EventTableIndexMetadata eventTableIndexMetadataRepo;
    private StatementContext statementContext;

    public NamedWindowImpl(NamedWindowMetaData metadata, EPStatementInitServices services) {
        rootView = new NamedWindowRootView(metadata);
        eventTableIndexMetadataRepo = metadata.getIndexMetadata();
        tailView = services.getNamedWindowFactoryService().createNamedWindowTailView(metadata.getEventType(), metadata.isChildBatching(), services, metadata.getContextName());
    }

    public String getName() {
        return rootView.getEventType().getName();
    }

    public NamedWindowRootView getRootView() {
        return rootView;
    }

    public NamedWindowTailView getTailView() {
        return tailView;
    }

    public NamedWindowConsumerView addConsumer(NamedWindowConsumerDesc consumerDesc, boolean isSubselect) {

        // handle same-context consumer
        if (rootView.getContextName() != null) {
            ContextRuntimeDescriptor contextDescriptor = consumerDesc.getAgentInstanceContext().getStatementContext().getContextRuntimeDescriptor();
            if (contextDescriptor != null && rootView.getContextName().equals(contextDescriptor.getContextName())) {
                StatementResourceHolder holder = statementContext.getStatementResourceService().getPartitioned(consumerDesc.getAgentInstanceContext().getAgentInstanceId());
                return holder.getNamedWindowInstance().getTailViewInstance().addConsumer(consumerDesc, isSubselect);
            } else {
                // consumer is out-of-context
                return tailView.addConsumerNoContext(consumerDesc);  // non-context consumers
            }
        }

        // handle no context associated
        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();
        return statementResourceService.getResourcesUnpartitioned().getNamedWindowInstance().getTailViewInstance().addConsumer(consumerDesc, isSubselect);
    }

    public NamedWindowInstance getNamedWindowInstance(AgentInstanceContext agentInstanceContext) {
        if (rootView.getContextName() == null) {
            return getNamedWindowInstanceNoContext();
        }

        if (agentInstanceContext.getStatementContext().getContextRuntimeDescriptor() == null) {
            return null;
        }

        if (this.rootView.getContextName().equals(agentInstanceContext.getStatementContext().getContextRuntimeDescriptor().getContextName())) {
            return getNamedWindowInstance(agentInstanceContext.getAgentInstanceId());
        }

        return null;
    }

    public NamedWindowInstance getNamedWindowInstanceNoContext() {
        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();
        StatementResourceHolder holder = statementResourceService.getUnpartitioned();
        return holder == null ? null : holder.getNamedWindowInstance();
    }

    public NamedWindowInstance getNamedWindowInstance(int agentInstanceId) {
        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();
        StatementResourceHolder holder = statementResourceService.getPartitioned(agentInstanceId);
        return holder == null ? null : holder.getNamedWindowInstance();
    }

    public EventTableIndexMetadata getEventTableIndexMetadata() {
        return eventTableIndexMetadataRepo;
    }

    public void removeAllInstanceIndexes(IndexMultiKey index) {
        StatementResourceService statementResourceService = statementContext.getStatementCPCacheService().getStatementResourceService();
        if (rootView.getContextName() == null) {
            StatementResourceHolder holder = statementResourceService.getUnpartitioned();
            if (holder != null && holder.getNamedWindowInstance() != null) {
                holder.getNamedWindowInstance().removeIndex(index);
            }
        } else {
            for (Map.Entry<Integer, StatementResourceHolder> entry : statementResourceService.getResourcesPartitioned().entrySet()) {
                if (entry.getValue().getNamedWindowInstance() != null) {
                    entry.getValue().getNamedWindowInstance().removeIndex(index);
                }
            }
        }
    }

    public void removeIndexReferencesStmtMayRemoveIndex(IndexMultiKey imk, String referringDeploymentId, String referringStatementName) {
        boolean last = eventTableIndexMetadataRepo.removeIndexReference(imk, referringDeploymentId);
        if (last) {
            eventTableIndexMetadataRepo.removeIndex(imk);
            removeAllInstanceIndexes(imk);
        }
    }

    public void validateAddIndex(String deloymentId, String statementName, String explicitIndexName, String explicitIndexModuleName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey imk) throws ExprValidationException {
        eventTableIndexMetadataRepo.addIndexExplicit(false, imk, explicitIndexName, explicitIndexModuleName, explicitIndexDesc, deloymentId);
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public void setStatementContext(StatementContext statementContext) {
        this.statementContext = statementContext;
    }
}
