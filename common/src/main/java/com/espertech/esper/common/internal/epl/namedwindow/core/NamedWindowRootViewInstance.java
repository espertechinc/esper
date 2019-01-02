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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetQueryExec;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableUtil;
import com.espertech.esper.common.internal.epl.index.base.EventTableVisitor;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataEntry;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepository;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepositoryEntry;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * The root window in a named window plays multiple roles: It holds the indexes for deleting rows, if any on-delete statement
 * requires such indexes. Such indexes are updated when events arrive, or remove from when a data window
 * or on-delete statement expires events. The view keeps track of on-delete statements their indexes used.
 */
public class NamedWindowRootViewInstance extends ViewSupport {
    private final NamedWindowRootView rootView;
    private final AgentInstanceContext agentInstanceContext;

    private final EventTableIndexRepository indexRepository;

    private Iterable<EventBean> dataWindowContents;

    public NamedWindowRootViewInstance(NamedWindowRootView rootView, AgentInstanceContext agentInstanceContext, EventTableIndexMetadata eventTableIndexMetadata) {
        this.rootView = rootView;
        this.agentInstanceContext = agentInstanceContext;

        this.indexRepository = new EventTableIndexRepository(eventTableIndexMetadata);
        for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry : eventTableIndexMetadata.getIndexes().entrySet()) {
            if (entry.getValue().getOptionalQueryPlanIndexItem() != null) {
                EventTable index = EventTableUtil.buildIndex(agentInstanceContext, 0, entry.getValue().getOptionalQueryPlanIndexItem(), rootView.getEventType(), true, entry.getKey().isUnique(), entry.getValue().getOptionalIndexName(), null, false);
                indexRepository.addIndex(entry.getKey(), new EventTableIndexRepositoryEntry(entry.getValue().getOptionalIndexName(), entry.getValue().getOptionalIndexModuleName(), index));
            }
        }
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public IndexMultiKey[] getIndexes() {
        return indexRepository.getIndexDescriptors();
    }

    public Iterable<EventBean> getDataWindowContents() {
        return dataWindowContents;
    }

    /**
     * Sets the iterator to use to obtain current named window data window contents.
     *
     * @param dataWindowContents iterator over events help by named window
     */
    public void setDataWindowContents(Iterable<EventBean> dataWindowContents) {
        this.dataWindowContents = dataWindowContents;
    }

    /**
     * Called by tail view to indicate that the data window view exired events that must be removed from index tables.
     *
     * @param oldData removed stream of the data window
     */
    public void removeOldData(EventBean[] oldData) {
        for (EventTable table : indexRepository.getTables()) {
            table.remove(oldData, agentInstanceContext);
        }
    }

    /**
     * Called by tail view to indicate that the data window view has new events that must be added to index tables.
     *
     * @param newData new event
     */
    public void addNewData(EventBean[] newData) {
        for (EventTable table : indexRepository.getTables()) {
            table.add(newData, agentInstanceContext);
        }
    }

    // Called by deletion strategy and also the insert-into for new events only
    public void update(EventBean[] newData, EventBean[] oldData) {
        // Update indexes for fast deletion, if there are any
        if (rootView.isChildBatching()) {
            for (EventTable table : indexRepository.getTables()) {
                table.add(newData, agentInstanceContext);
            }
        }

        // Update child views
        child.update(newData, oldData);
    }

    public void setParent(Viewable parent) {
        super.setParent(parent);
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return null;
    }

    /**
     * Destroy and clear resources.
     */
    public void destroy() {
        indexRepository.destroy();
        if (isVirtualDataWindow()) {
            getVirtualDataWindow().handleDestroy(agentInstanceContext.getAgentInstanceId());
        }
    }

    /**
     * Return a snapshot using index lookup filters.
     *
     * @param annotations annotations
     * @param queryGraph  query graph
     * @return events
     */
    public Collection<EventBean> snapshot(QueryGraph queryGraph, Annotation[] annotations) {
        VirtualDWView virtualDataWindow = null;
        if (isVirtualDataWindow()) {
            virtualDataWindow = getVirtualDataWindow();
        }
        return FireAndForgetQueryExec.snapshot(queryGraph, annotations, virtualDataWindow,
                indexRepository, rootView.getEventType().getName(), agentInstanceContext);
    }

    /**
     * Add an explicit index.
     *
     * @param explicitIndexDesc       index descriptor
     * @param explicitIndexModuleName module name
     * @param isRecoveringResilient   indicator for recovering
     * @param explicitIndexName       index name
     * @throws ExprValidationException if the index fails to be valid
     */
    public synchronized void addExplicitIndex(String explicitIndexName, String explicitIndexModuleName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient) throws ExprValidationException {
        boolean initIndex = agentInstanceContext.getStatementContext().getEventTableIndexService().allowInitIndex(isRecoveringResilient);
        Iterable<EventBean> initializeFrom = initIndex ? this.dataWindowContents : CollectionUtil.NULL_EVENT_ITERABLE;
        indexRepository.validateAddExplicitIndex(explicitIndexName, explicitIndexModuleName, explicitIndexDesc, rootView.getEventType(), initializeFrom, agentInstanceContext, isRecoveringResilient, null);
    }

    public void visitIndexes(EventTableVisitor visitor) {
        visitor.visit(indexRepository.getTables());
    }

    public boolean isParentBatchWindow() {
        return rootView.isChildBatching();
    }

    public EventTableIndexRepository getIndexRepository() {
        return indexRepository;
    }

    public boolean isVirtualDataWindow() {
        return child instanceof VirtualDWView;
    }

    public VirtualDWView getVirtualDataWindow() {
        if (!isVirtualDataWindow()) {
            return null;
        }
        return (VirtualDWView) child;
    }

    public void clearDeliveriesRemoveStream(EventBean[] removedEvents) {
        agentInstanceContext.getStatementResultService().clearDeliveriesRemoveStream(removedEvents);
    }
}
