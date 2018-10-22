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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for changes to {@link EventTypeIndex} for addition and removal of filters.
 * It delegates the work to make modifications to the filter parameter tree to an {@link IndexTreeBuilderAdd} and {@link IndexTreeBuilderRemove}.
 * It enforces a policy that a filter callback can only be added once.
 */
public class EventTypeIndexBuilder {
    private final Lock callbacksLock;
    private final EventTypeIndex eventTypeIndex;

    /**
     * Constructor - takes the event type index to manipulate as its parameter.
     *
     * @param eventTypeIndex - index to manipulate
     * @param allowIsolation - indicator whether isolated service provider is allowed for the runtime
     */
    public EventTypeIndexBuilder(EventTypeIndex eventTypeIndex, boolean allowIsolation) {
        this.eventTypeIndex = eventTypeIndex;
        this.callbacksLock = new ReentrantLock();
    }

    public Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> get(Set<Integer> statementIds) {
        return eventTypeIndex.getTraverseStatement(statementIds);
    }

    /**
     * Destroy the service.
     */
    public void destroy() {
        eventTypeIndex.destroy();
    }

    /**
     * Add a filter to the event type index structure, and to the filter subtree.
     * Throws an IllegalStateException exception if the callback is already registered.
     *
     * @param valueSet       is the filter information
     * @param filterCallback is the callback
     * @param lockFactory    lock factory
     * @param eventType      event type
     */
    public final void add(EventType eventType, FilterValueSetParam[][] valueSet, FilterHandle filterCallback, FilterServiceGranularLockFactory lockFactory) {

        // Check if a filter tree exists for this event type
        FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

        // Make sure we have a root node
        if (rootNode == null) {
            callbacksLock.lock();
            try {
                rootNode = eventTypeIndex.get(eventType);
                if (rootNode == null) {
                    rootNode = new FilterHandleSetNode(lockFactory.obtainNew());
                    eventTypeIndex.add(eventType, rootNode);
                }
            } finally {
                callbacksLock.unlock();
            }
        }

        // Now add to tree
        IndexTreeBuilderAdd.add(valueSet, filterCallback, rootNode, lockFactory);
    }

    /**
     * Remove a filter callback from the given index node.
     *
     * @param eventType      event type
     * @param valueSet       value set
     * @param filterCallback is the callback to remove
     */
    public final void remove(FilterHandle filterCallback, EventType eventType, FilterValueSetParam[][] valueSet) {
        FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

        if (rootNode != null) {
            if (valueSet.length == 0) {
                IndexTreeBuilderRemove.remove(eventType, filterCallback, FilterSpecParam.EMPTY_VALUE_ARRAY, rootNode);
            } else {
                for (int i = 0; i < valueSet.length; i++) {
                    IndexTreeBuilderRemove.remove(eventType, filterCallback, valueSet[i], rootNode);
                }
            }
        }
    }

    public boolean isSupportsTakeApply() {
        return false;
    }
}
