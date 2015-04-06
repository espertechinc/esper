/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.filter;

import com.espertech.esper.client.EventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for changes to {@link EventTypeIndex} for addition and removal of filters.
 * It delegates the work to make modifications to the filter parameter tree to an {@link IndexTreeBuilder}.
 * It enforces a policy that a filter callback can only be added once.
 */
public class EventTypeIndexBuilder
{
    private final Map<FilterHandle, EventTypeIndexBuilderValueIndexesPair> callbacks;
    private final Lock callbacksLock;
    private final EventTypeIndex eventTypeIndex;

    /**
     * Constructor - takes the event type index to manipulate as its parameter.
     * @param eventTypeIndex - index to manipulate
     */
    public EventTypeIndexBuilder(EventTypeIndex eventTypeIndex)
    {
        this.eventTypeIndex = eventTypeIndex;

        this.callbacks = new HashMap<FilterHandle, EventTypeIndexBuilderValueIndexesPair>();
        this.callbacksLock = new ReentrantLock();
    }

    /**
     * Destroy the service.
     */
    public void destroy()
    {
        callbacks.clear();
    }

    /**
     * Add a filter to the event type index structure, and to the filter subtree.
     * Throws an IllegalStateException exception if the callback is already registered.
     * @param filterValueSet is the filter information
     * @param filterCallback is the callback
     */
    public final void add(FilterValueSet filterValueSet, FilterHandle filterCallback, FilterServiceGranularLockFactory lockFactory)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qFilterAdd(filterValueSet, filterCallback);}
        EventType eventType = filterValueSet.getEventType();

        // Check if a filter tree exists for this event type
        FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

        // Make sure we have a root node
        if (rootNode == null)
        {
            callbacksLock.lock();
            try
            {
                rootNode = eventTypeIndex.get(eventType);
                if (rootNode == null)
                {
                    rootNode = new FilterHandleSetNode(lockFactory.obtainNew());
                    eventTypeIndex.add(eventType, rootNode);
                }
            }
            finally
            {
                callbacksLock.unlock();
            }
        }

        // Make sure the filter callback doesn't already exist
        callbacksLock.lock();
        try
        {
            if (callbacks.containsKey(filterCallback))
            {
                throw new IllegalStateException("Callback for filter specification already exists in collection");
            }
        }
        finally
        {
            callbacksLock.unlock();
        }

        // Now add to tree
        ArrayDeque<EventTypeIndexBuilderIndexLookupablePair>[] path = IndexTreeBuilder.add(filterValueSet, filterCallback, rootNode, lockFactory);
        EventTypeIndexBuilderIndexLookupablePair[][] pathArray = new EventTypeIndexBuilderIndexLookupablePair[path.length][];
        for (int i = 0; i < path.length; i++) {
            pathArray[i] = path[i].toArray(new EventTypeIndexBuilderIndexLookupablePair[path[i].size()]);
        }
        EventTypeIndexBuilderValueIndexesPair pair = new EventTypeIndexBuilderValueIndexesPair(filterValueSet, pathArray);

        callbacksLock.lock();
        try
        {
            callbacks.put(filterCallback, pair);
        }
        finally
        {
            callbacksLock.unlock();
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aFilterAdd();}
    }

    /**
     * Remove a filter callback from the given index node.
     * @param filterCallback is the callback to remove
     */
    public final void remove(FilterHandle filterCallback)
    {
        EventTypeIndexBuilderValueIndexesPair pair = null;
        callbacksLock.lock();
        try
        {
            pair = callbacks.get(filterCallback);
        }
        finally
        {
            callbacksLock.unlock();
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qFilterRemove(filterCallback, pair);}

        if (pair == null)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aFilterRemove();}
            return;
        }

        EventType eventType = pair.getFilterValueSet().getEventType();
        FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

        // Now remove from tree
        if (rootNode != null) {
            for (int i = 0; i < pair.getIndexPairs().length; i++) {
                IndexTreeBuilder.remove(eventType, filterCallback, pair.getIndexPairs()[i], rootNode);
            }
        }

        // Remove from callbacks list
        callbacksLock.lock();
        try
        {
            callbacks.remove(filterCallback);
        }
        finally
        {
            callbacksLock.unlock();
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aFilterRemove();}
    }

    /**
     * Returns filters for the statement ids.
     * @param statementIds ids to take
     * @return set of filters for taken statements
     */
    public final FilterSet take(Set<String> statementIds)
    {
        List<FilterSetEntry> list = new ArrayList<FilterSetEntry>();
        callbacksLock.lock();
        try
        {
            for (Map.Entry<FilterHandle, EventTypeIndexBuilderValueIndexesPair> entry : callbacks.entrySet())
            {
                EventTypeIndexBuilderValueIndexesPair pair = entry.getValue();
                if (statementIds.contains(entry.getKey().getStatementId()))
                {
                    list.add(new FilterSetEntry(entry.getKey(), pair.getFilterValueSet()));

                    EventType eventType = pair.getFilterValueSet().getEventType();
                    FilterHandleSetNode rootNode = eventTypeIndex.get(eventType);

                    // Now remove from tree
                    for (int i = 0; i < pair.getIndexPairs().length; i++) {
                        IndexTreeBuilder.remove(eventType, entry.getKey(), pair.getIndexPairs()[i], rootNode);
                    }
                }
            }
            
            for (FilterSetEntry removed : list)
            {
                callbacks.remove(removed.getHandle());
            }
        }
        finally
        {
            callbacksLock.unlock();
        }

        return new FilterSet(list);
    }

    /**
     * Add the filters, from previously-taken filters.
     * @param filterSet to add
     */
    public void apply(FilterSet filterSet, FilterServiceGranularLockFactory lockFactory)
    {
        for (FilterSetEntry entry : filterSet.getFilters())
        {
            add(entry.getFilterValueSet(), entry.getHandle(), lockFactory);
        }
    }
}
