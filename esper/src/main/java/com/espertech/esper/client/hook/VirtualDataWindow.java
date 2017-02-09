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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;

/**
 * A virtual data window exposes externally-managed data transparently as a named window without the need
 * to retain any data in memory.
 * <p>
 * An instance is associated to each named window that is backed by a virtual data window.
 */
public interface VirtualDataWindow {

    /**
     * Returns the lookup strategy for use by an EPL statement to obtain data.
     * <p>
     * This method is invoked one or more times at the time an EPL statement is created
     * that performs a subquery, join, on-action or fire-and-forget query against the virtual data window.
     * <p>
     * The lookup strategy returned is used when the EPL statement for which it was created
     * performs a read-operation against the managed data. Multiple lookup strategies
     * for the same EPL statement are possible for join statements.
     * <p>
     * The context object passed in is derived from an analysis of the where-clause
     * and lists the unique property names of the event type that are index fields, i.e.
     * fields against which the lookup occurs.
     * <p>
     * The order of hash and btree properties provided by the context
     * matches the order that lookup values are provided to the lookup strategy.
     *
     * @param desc hash and binary tree (sorted access for ranges) index fields
     * @return lookup strategy, or null to veto the statement
     */
    public VirtualDataWindowLookup getLookup(VirtualDataWindowLookupContext desc);

    /**
     * Handle a management event.
     * <p>
     * Management events indicate:
     * <ul>
     * <li>Create/Start of an index on a virtual data window.</li>
     * <li>Stop/Destroy of an index.</li>
     * <li>Destroy of the virtual data window.</li>
     * <li>Add/Remove of a consumer to the virtual data window.</li>
     * </ul>
     *
     * @param theEvent to handle
     */
    public void handleEvent(VirtualDataWindowEvent theEvent);

    /**
     * This method is invoked when events are inserted-into or removed-from the
     * virtual data window.
     * <p>
     * When a statement uses insert-into to insert events into the virtual data window
     * the newData parameter carries the inserted event.
     * <p>
     * When a statement uses on-delete to delete events from the virtual data window
     * the oldData parameter carries the deleted event.
     * <p>
     * When a statement uses on-merge to merge events with the virtual data window
     * the events passed depends on the action: For then-delete the oldData carries the removed event,
     * for then-update the newData carries the after-update event and the oldData carries the before-update event,
     * for then-insert the newData carries the inserted event.
     * <p>
     * When a statement uses on-update to update events in the virtual data window
     * the newData carries the after-update event and the oldData parameter carries the before-update event.
     * <p>
     * Implement as follows to post all inserted or removed events to consuming statements:
     * context.getOutputStream().update(newData, oldData);
     * <p>
     * For data originating from the virtual data window use the sendEvent() method with "insert-into" statement
     * to insert events.
     *
     * @param newData the insert stream
     * @param oldData the remove stream
     */
    public void update(EventBean[] newData, EventBean[] oldData);

    /**
     * Called when the named window is stopped or destroyed.
     * <p>
     * We used with contexts then this method is invoked for each context partition that gets destroyed.
     * <p>
     * There is also a destroy method on the factory level that is called once per named window (and not once per context partition).
     */
    public void destroy();

    /**
     * This method is called when a consuming statement to the named window
     * receives initial state from the named window, for example "select sum(field) from MyVirtualDataWindow"
     * in order to initialize its state.
     * <p>
     * It is valid to return an empty iterator such as "return Collections.&lt;EventBean&gt;emptyList().iterator();".
     * If returning an empty iterator then consuming statements do not receive initial data, therefor in the example provide earlier
     * the "sum(field)" is initially zero and no the sum of the field values.
     * </p>
     *
     * @return empty iterator or an iterator for all events currently held by the virtual data window.
     */
    public Iterator<EventBean> iterator();
}
