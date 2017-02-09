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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * This class holds a list of indizes storing filter constants in {@link FilterParamIndexBase} nodes
 * and a set of {@link FilterHandle}.
 * An instance of this class represents a leaf-node (no indizes stored, just filter callbacks)
 * but can also be non-leaf (some indizes exist) in a filter evaluation tree.
 * Events are evaluated by asking each of the indizes to evaluate the event and by
 * adding any filter callbacks in this node to the "matches" list of callbacks.
 */
public final class FilterHandleSetNode implements EventEvaluator {
    private final ReadWriteLock nodeRWLock;
    private final Set<FilterHandle> callbackSet;
    private final List<FilterParamIndexBase> indizes;

    public FilterHandleSetNode(ReadWriteLock nodeRWLock) {
        this.nodeRWLock = nodeRWLock;
        callbackSet = new LinkedHashSet<FilterHandle>();
        indizes = new LinkedList<FilterParamIndexBase>();
    }

    /**
     * Returns an indication of whether there are any callbacks or index nodes at all in this set.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @return true if there are neither indizes nor filter callbacks stored, false if either exist.
     */
    public boolean isEmpty() {
        return callbackSet.isEmpty() && indizes.isEmpty();
    }

    /**
     * Returns the number of filter callbacks stored.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @return number of filter callbacks stored
     */
    public int getFilterCallbackCount() {
        return callbackSet.size();
    }

    /**
     * Returns to lock to use for making changes to the filter callback or inzides collections stored by this node.
     *
     * @return lock to use in multithreaded environment
     */
    public final ReadWriteLock getNodeRWLock() {
        return nodeRWLock;
    }

    /**
     * Returns list of indexes - not returning an iterator. Client classes should not change this collection.
     *
     * @return list of indizes
     */
    public List<FilterParamIndexBase> getIndizes() {
        return indizes;
    }

    /**
     * Evaluate an event by asking each index to match the event. Any filter callbacks at this node automatically
     * match the event and do not need to be further evaluated, and are thus added to the "matches" list of callbacks.
     * NOTE: This client should not use the lock before calling this method.
     *
     * @param theEvent is the event wrapper supplying the event property values
     * @param matches  is the list of callbacks to add to for any matches found
     */
    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        nodeRWLock.readLock().lock();
        try {
            if (InstrumentationHelper.ENABLED) {
                if (!indizes.isEmpty()) {
                    InstrumentationHelper.get().qFilterHandleSetIndexes(indizes);
                }
            }

            // Ask each of the indizes to match against the attribute values
            for (FilterParamIndexBase index : indizes) {
                index.matchEvent(theEvent, matches);
            }

            if (InstrumentationHelper.ENABLED) {
                if (!indizes.isEmpty()) {
                    InstrumentationHelper.get().aFilterHandleSetIndexes();
                }
            }

            if (InstrumentationHelper.ENABLED) {
                if (!callbackSet.isEmpty()) {
                    InstrumentationHelper.get().qaFilterHandleSetCallbacks(callbackSet);
                }
            }

            // Add each filter callback stored in this node to the matching list
            for (FilterHandle filterCallback : callbackSet) {
                matches.add(filterCallback);
            }
        } finally {
            nodeRWLock.readLock().unlock();
        }
    }

    /**
     * Returns an indication whether the filter callback exists in this node.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @param filterCallback is the filter callback to check for
     * @return true if callback found, false if not
     */
    public boolean contains(FilterHandle filterCallback) {
        return callbackSet.contains(filterCallback);
    }

    /**
     * Add an index. The same index can be added twice - there is no checking done.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @param index - index to add
     */
    public final void add(FilterParamIndexBase index) {
        indizes.add(index);
    }

    /**
     * Remove an index, returning true if it was found and removed or false if not in collection.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @param index is the index to remove
     * @return true if found, false if not existing
     */
    public final boolean remove(FilterParamIndexBase index) {
        return indizes.remove(index);
    }

    /**
     * Add a filter callback. The filter callback set allows adding the same callback twice with no effect.
     * If a client to the class needs to check that the callback already existed, the contains method does that.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @param filterCallback is the callback to add
     */
    public final void add(FilterHandle filterCallback) {
        callbackSet.add(filterCallback);
    }

    /**
     * Remove a filter callback, returning true if it was found and removed or false if not in collection.
     * NOTE: the client to this method must use the read-write lock of this object to lock, if required by the client
     * code.
     *
     * @param filterCallback is the callback to remove
     * @return true if found, false if not existing
     */
    public final boolean remove(FilterHandle filterCallback) {
        return callbackSet.remove(filterCallback);
    }

    public Set<FilterHandle> getCallbackSet() {
        return callbackSet;
    }
}
