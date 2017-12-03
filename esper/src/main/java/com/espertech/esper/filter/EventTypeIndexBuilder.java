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
package com.espertech.esper.filter;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for changes to {@link EventTypeIndex} for addition and removal of filters.
 * It delegates the work to make modifications to the filter parameter tree to an {@link IndexTreeBuilder}.
 * It enforces a policy that a filter callback can only be added once.
 */
public class EventTypeIndexBuilder {
    private final Map<FilterHandle, EventTypeIndexBuilderValueIndexesPair> isolatableCallbacks;
    private final Lock callbacksLock;
    private final EventTypeIndex eventTypeIndex;

    /**
     * Constructor - takes the event type index to manipulate as its parameter.
     *
     * @param eventTypeIndex - index to manipulate
     * @param allowIsolation - indicator whether isolated service provider is allowed for the engine
     */
    public EventTypeIndexBuilder(EventTypeIndex eventTypeIndex, boolean allowIsolation) {
        this.eventTypeIndex = eventTypeIndex;
        this.callbacksLock = new ReentrantLock();

        if (allowIsolation) {
            this.isolatableCallbacks = new HashMap<FilterHandle, EventTypeIndexBuilderValueIndexesPair>();
        } else {
            this.isolatableCallbacks = null;
        }
    }

    /**
     * Destroy the service.
     */
    public void destroy() {
        eventTypeIndex.destroy();
        if (isolatableCallbacks != null) {
            isolatableCallbacks.clear();
        }
    }

    /**
     * Add a filter to the event type index structure, and to the filter subtree.
     * Throws an IllegalStateException exception if the callback is already registered.
     *
     * @param filterValueSet is the filter information
     * @param filterCallback is the callback
     * @param lockFactory    lock factory
     * @return filter service enrty
     */
    public final FilterServiceEntry add(FilterValueSet filterValueSet, FilterHandle filterCallback, FilterServiceGranularLockFactory lockFactory) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterAdd(filterValueSet, filterCallback);
        }
        EventType eventType = filterValueSet.getEventType();

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
        ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[] path = IndexTreeBuilder.add(filterValueSet, filterCallback, rootNode, lockFactory);
        EventTypeIndexBuilderIndexLookupablePair[][] pathArray = new EventTypeIndexBuilderIndexLookupablePair[path.length][];
        for (int i = 0; i < path.length; i++) {
            pathArray[i] = path[i].toArray(new EventTypeIndexBuilderIndexLookupablePair[path[i].size()]);
        }
        EventTypeIndexBuilderValueIndexesPair pair = new EventTypeIndexBuilderValueIndexesPair(filterValueSet, pathArray);

        // for non-isolatable callbacks the consumer keeps track of tree location
        if (isolatableCallbacks == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterAdd();
            }
            return pair;
        }

        // for isolatable callbacks this class is keeping track of tree location
        callbacksLock.lock();
        try {
            isolatableCallbacks.put(filterCallback, pair);
        } finally {
            callbacksLock.unlock();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterAdd();
        }
        return null;
    }

    /**
     * Remove a filter callback from the given index node.
     *
     * @param filterCallback     is the callback to remove
     * @param filterServiceEntry entry
     */
    public final void remove(FilterHandle filterCallback, FilterServiceEntry filterServiceEntry) {
        EventTypeIndexBuilderValueIndexesPair pair;
        if (isolatableCallbacks != null) {
            callbacksLock.lock();
            try {
                pair = isolatableCallbacks.remove(filterCallback);
            } finally {
                callbacksLock.unlock();
            }
            if (pair == null) {
                return;
            }
        } else {
            pair = (EventTypeIndexBuilderValueIndexesPair) filterServiceEntry;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterRemove(filterCallback, pair);
        }

        EventType eventType = pair.getFilterValueSet().getEventType();
        FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

        // Now remove from tree
        if (rootNode != null) {
            for (int i = 0; i < pair.getIndexPairs().length; i++) {
                IndexTreeBuilder.remove(eventType, filterCallback, pair.getIndexPairs()[i], rootNode);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterRemove();
        }
    }

    /**
     * Returns filters for the statement ids.
     *
     * @param statementIds ids to take
     * @return set of filters for taken statements
     */
    public final FilterSet take(Set<Integer> statementIds) {
        if (isolatableCallbacks == null) {
            throw new EPException("Operation not supported, please enable isolation in the engine configuration");
        }

        List<FilterSetEntry> list = new ArrayList<FilterSetEntry>();
        callbacksLock.lock();
        try {
            for (Map.Entry<FilterHandle, EventTypeIndexBuilderValueIndexesPair> entry : isolatableCallbacks.entrySet()) {
                EventTypeIndexBuilderValueIndexesPair pair = entry.getValue();
                if (statementIds.contains(entry.getKey().getStatementId())) {
                    list.add(new FilterSetEntry(entry.getKey(), pair.getFilterValueSet()));

                    EventType eventType = pair.getFilterValueSet().getEventType();
                    FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

                    // Now remove from tree
                    for (int i = 0; i < pair.getIndexPairs().length; i++) {
                        IndexTreeBuilder.remove(eventType, entry.getKey(), pair.getIndexPairs()[i], rootNode);
                    }
                }
            }

            for (FilterSetEntry removed : list) {
                isolatableCallbacks.remove(removed.getHandle());
            }
        } finally {
            callbacksLock.unlock();
        }

        return new FilterSet(list);
    }

    /**
     * Add the filters, from previously-taken filters.
     *
     * @param filterSet   to add
     * @param lockFactory lock factory
     */
    public void apply(FilterSet filterSet, FilterServiceGranularLockFactory lockFactory) {
        for (FilterSetEntry entry : filterSet.getFilters()) {
            add(entry.getFilterValueSet(), entry.getHandle(), lockFactory);
        }
    }

    public boolean isSupportsTakeApply() {
        return isolatableCallbacks != null;
    }
}
