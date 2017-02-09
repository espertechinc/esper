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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.IndentWriter;

import java.util.Collection;
import java.util.Set;

/**
 * Execution node for lookup in a table for outer joins. This execution node thus generates
 * rows even if no joined events could be found, the joined table events are set to null if no
 * joined events are found.
 */
public class TableOuterLookupExecNode extends ExecNode {
    private int indexedStream;
    protected JoinExecTableLookupStrategy lookupStrategy;

    /**
     * Ctor.
     *
     * @param indexedStream  - stream indexed for lookup
     * @param lookupStrategy - strategy to use for lookup (full table/indexed)
     */
    public TableOuterLookupExecNode(int indexedStream, JoinExecTableLookupStrategy lookupStrategy) {
        this.indexedStream = indexedStream;
        this.lookupStrategy = lookupStrategy;
    }

    /**
     * Returns strategy for lookup.
     *
     * @return lookup strategy
     */
    public JoinExecTableLookupStrategy getLookupStrategy() {
        return lookupStrategy;
    }

    public void process(EventBean lookupEvent, EventBean[] prefillPath, Collection<EventBean[]> result, ExprEvaluatorContext exprEvaluatorContext) {
        // Lookup events
        Set<EventBean> joinedEvents = lookupStrategy.lookup(lookupEvent, null, exprEvaluatorContext);

        // process
        processResults(prefillPath, result, joinedEvents);
    }

    protected void processResults(EventBean[] prefillPath, Collection<EventBean[]> result, Set<EventBean> joinedEvents) {

        // If no events are found, since this is an outer join, create a result row leaving the
        // joined event as null.
        if ((joinedEvents == null) || (joinedEvents.isEmpty())) {
            EventBean[] events = new EventBean[prefillPath.length];
            System.arraycopy(prefillPath, 0, events, 0, events.length);
            result.add(events);

            return;
        }

        // Create result row for each found event
        for (EventBean joinedEvent : joinedEvents) {
            EventBean[] events = new EventBean[prefillPath.length];
            System.arraycopy(prefillPath, 0, events, 0, events.length);
            events[indexedStream] = joinedEvent;
            result.add(events);
        }
    }

    /**
     * Returns target stream for lookup.
     *
     * @return indexed stream
     */
    public int getIndexedStream() {
        return indexedStream;
    }

    public void print(IndentWriter writer) {
        writer.println("TableOuterLookupExecNode indexedStream=" + indexedStream + " lookup=" + lookupStrategy);
    }
}
