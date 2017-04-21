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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Iterator;

/**
 * Table of events allowing add and remove. Lookup in table is coordinated
 * through the underlying implementation.
 */
public interface EventTable extends Iterable<EventBean> {
    /**
     * Add and remove events from table.
     * <p>
     * It is up to the index to decide whether to add first and then remove,
     * or whether to remove and then add.
     * </p>
     * <p>
     * It is important to note that a given event can be in both the
     * removed and the added events. This means that unique indexes probably need to remove first
     * and then add. Most other non-unique indexes will add first and then remove
     * since the an event can be both in the add and the remove stream.
     * </p>
     *
     * @param newData              to add
     * @param oldData              to remove
     * @param exprEvaluatorContext evaluator context
     */
    void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Add events to table.
     *
     * @param events               to add
     * @param exprEvaluatorContext evaluator context
     */
    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Add event to table.
     *
     * @param event                to add
     * @param exprEvaluatorContext evaluator context
     */
    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Remove events from table.
     *
     * @param events               to remove
     * @param exprEvaluatorContext evaluator context
     */
    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Remove event from table.
     *
     * @param event                to remove
     * @param exprEvaluatorContext evaluator context
     */
    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns an iterator over events in the table. Not required to be implemented for all indexes.
     * Full table scans and providers that have easy access to an iterator may implement.
     *
     * @return table iterator
     * @throws UnsupportedOperationException for operation not supported for this type of index
     */
    public Iterator<EventBean> iterator();

    /**
     * Returns true if the index is definitely empty,
     * or false if is not definitely empty but we can not certain.
     *
     * @return true for definitely empty index, false for there-may-be-rows and please-check-by-iterating
     */
    public boolean isEmpty();

    /**
     * Clear out index.
     */
    public void clear();

    /**
     * Destroy index.
     */
    public void destroy();

    public String toQueryPlan();

    public Class getProviderClass();

    /**
     * If the number of events is readily available, an implementation will return that number
     * or it may return null to indicate that the count is not readily available.
     *
     * @return number of events
     */
    public Integer getNumberOfEvents();

    /**
     * If the index retains events using some key-based organization this returns the number of keys,
     * and may return -1 to indicate that either the number of keys is not available or
     * costly to obtain.
     * <p>
     * The number returned can be an estimate and may not be accurate.
     * </p>
     *
     * @return number of keys
     */
    public int getNumKeys();

    /**
     * Return the index object itself, or an object-array for multiple index structures.
     * <p>
     * May return null if the information is not readily available, i.e. externally maintained index
     * </p>
     *
     * @return index object
     */
    public Object getIndex();

    public EventTableOrganization getOrganization();
}