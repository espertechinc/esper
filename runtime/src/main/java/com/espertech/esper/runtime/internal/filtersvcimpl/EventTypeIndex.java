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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Mapping of event type to a tree-like structure
 * containing filter parameter constants in indexes {@link FilterParamIndexBase} and filter callbacks in {@link FilterHandleSetNode}.
 * <p>
 * This class evaluates events for the purpose of filtering by (1) looking up the event's {@link EventType}
 * and (2) asking the subtree for this event type to evaluate the event.
 * <p>
 * The class performs all the locking required for multithreaded access.
 */
public class EventTypeIndex implements EventEvaluator {
    private Map<EventType, FilterHandleSetNode> eventTypes;
    private ReadWriteLock eventTypesRWLock;

    public EventTypeIndex(FilterServiceGranularLockFactory lockFactory) {
        eventTypes = new HashMap<EventType, FilterHandleSetNode>();
        eventTypesRWLock = lockFactory.obtainNew();
    }

    /**
     * Destroy the service.
     */
    public void destroy() {
        eventTypes.clear();
    }

    public Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> getTraverseStatement(Set<Integer> statementIds) {
        ArrayDeque<FilterItem> evaluatorStack = new ArrayDeque<>();
        Map<Integer, List<FilterItem[]>> filters = new HashMap<>();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> filtersPerType = new HashMap<>();

        EventTypeIndexTraverse traverse = new EventTypeIndexTraverse() {
            public void add(ArrayDeque<FilterItem> stack, FilterHandle filterHandle) {
                FilterItem[] filterArray = stack.toArray(new FilterItem[stack.size()]);
                List<FilterItem[]> existing = filters.get(filterHandle.getStatementId());
                if (existing == null) {
                    existing = new ArrayList<>();
                    filters.put(filterHandle.getStatementId(), existing);
                }
                existing.add(filterArray);
            }
        };

        for (Map.Entry<EventType, FilterHandleSetNode> entry : eventTypes.entrySet()) {
            entry.getValue().getTraverseStatement(traverse, statementIds, evaluatorStack);
            if (!filters.isEmpty()) {
                filtersPerType.put(entry.getKey().getMetadata().getEventTypeIdPair(), new HashMap<>(filters));
                filters.clear();
            }
        }
        return filtersPerType;
    }

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        throw new UnsupportedOperationException("Use getTraverse instead");
    }

    /**
     * Add a new event type to the index and use the specified node for the root node of its subtree.
     * If the event type already existed, the method will throw an IllegalStateException.
     *
     * @param eventType is the event type to be added to the index
     * @param rootNode  is the root node of the subtree for filter constant indizes and callbacks
     */
    public void add(EventType eventType, FilterHandleSetNode rootNode) {
        eventTypesRWLock.writeLock().lock();
        try {
            if (eventTypes.containsKey(eventType)) {
                throw new IllegalStateException("Event type already in index, add not performed, type=" + eventType);
            }
            eventTypes.put(eventType, rootNode);
        } finally {
            eventTypesRWLock.writeLock().unlock();
        }
    }


    public void removeType(EventType type) {
        eventTypesRWLock.writeLock().lock();
        try {
            eventTypes.remove(type);
        } finally {
            eventTypesRWLock.writeLock().unlock();
        }
    }

    /**
     * Returns the root node for the given event type, or null if this event type has not been seen before.
     *
     * @param eventType is an event type
     * @return the subtree's root node
     */
    public FilterHandleSetNode get(EventType eventType) {
        eventTypesRWLock.readLock().lock();
        FilterHandleSetNode result = eventTypes.get(eventType);
        eventTypesRWLock.readLock().unlock();

        return result;
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        EventType eventType = theEvent.getEventType();

        // Attempt to match exact type
        matchType(eventType, theEvent, matches);

        // No supertype means we are done
        if (eventType.getSuperTypes() == null) {
            return;
        }

        for (Iterator<EventType> it = eventType.getDeepSuperTypes(); it.hasNext(); ) {
            EventType superType = it.next();
            matchType(superType, theEvent, matches);
        }
    }

    /**
     * Returns the current size of the known event types.
     *
     * @return collection size
     */
    protected int size() {
        return eventTypes.size();
    }

    protected int getFilterCountApprox() {

        int count = 0;
        eventTypesRWLock.readLock().lock();
        try {
            for (Map.Entry<EventType, FilterHandleSetNode> entry : eventTypes.entrySet()) {
                count += entry.getValue().getFilterCallbackCount();
                for (FilterParamIndexBase index : entry.getValue().getIndizes()) {
                    count += index.sizeExpensive();
                }
            }
        } finally {
            eventTypesRWLock.readLock().unlock();
        }
        return count;
    }

    private void matchType(EventType eventType, EventBean eventBean, Collection<FilterHandle> matches) {
        eventTypesRWLock.readLock().lock();
        FilterHandleSetNode rootNode = null;
        try {
            rootNode = eventTypes.get(eventType);
        } finally {
            eventTypesRWLock.readLock().unlock();
        }

        // If the top class node is null, no filters have yet been registered for this event type.
        // In this case, log a message and done.
        if (rootNode == null) {
            return;
        }

        rootNode.matchEvent(eventBean, matches);
    }
}
