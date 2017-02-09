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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.base.HistoricalIndexLookupStrategy;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.view.HistoricalEventViewable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A lookup strategy for use in outer joins onto historical streams.
 */
public class HistoricalTableLookupStrategy implements JoinExecTableLookupStrategy {
    private final HistoricalEventViewable viewable;
    private final PollResultIndexingStrategy indexingStrategy;
    private final HistoricalIndexLookupStrategy lookupStrategy;
    private final int streamNum;
    private final int rootStreamNum;
    private final ExprEvaluator outerJoinExprNode;
    private final EventBean[][] lookupEventsPerStream;

    /**
     * Ctor.
     *
     * @param viewable          providing the polling access
     * @param indexingStrategy  strategy for indexing results
     * @param lookupStrategy    strategy for using indexed results
     * @param numStreams        number of streams
     * @param streamNum         stream number of the historical stream
     * @param rootStreamNum     the query plan root stream number
     * @param outerJoinExprNode an optional outer join expression
     */
    public HistoricalTableLookupStrategy(HistoricalEventViewable viewable, PollResultIndexingStrategy indexingStrategy, HistoricalIndexLookupStrategy lookupStrategy, int numStreams, int streamNum, int rootStreamNum, ExprEvaluator outerJoinExprNode) {
        this.viewable = viewable;
        this.indexingStrategy = indexingStrategy;
        this.lookupStrategy = lookupStrategy;
        this.streamNum = streamNum;
        this.rootStreamNum = rootStreamNum;
        this.outerJoinExprNode = outerJoinExprNode;
        lookupEventsPerStream = new EventBean[1][numStreams];
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        int currStream = cursor.getStream();

        // fill the current stream and the deep cursor events
        lookupEventsPerStream[0][currStream] = theEvent;
        recursiveFill(lookupEventsPerStream[0], cursor.getNode());

        // poll
        EventTable[][] indexPerLookupRow = viewable.poll(lookupEventsPerStream, indexingStrategy, exprEvaluatorContext);

        Set<EventBean> result = null;
        for (EventTable[] index : indexPerLookupRow) {
            // Using the index, determine a subset of the whole indexed table to process, unless
            // the strategy is a full table scan
            Iterator<EventBean> subsetIter = lookupStrategy.lookup(theEvent, index, exprEvaluatorContext);

            if (subsetIter != null) {
                if (outerJoinExprNode != null) {
                    // Add each row to the join result or, for outer joins, run through the outer join filter
                    for (; subsetIter.hasNext(); ) {
                        EventBean candidate = subsetIter.next();

                        lookupEventsPerStream[0][streamNum] = candidate;
                        Boolean pass = (Boolean) outerJoinExprNode.evaluate(lookupEventsPerStream[0], true, exprEvaluatorContext);
                        if ((pass != null) && pass) {
                            if (result == null) {
                                result = new HashSet<EventBean>();
                            }
                            result.add(candidate);
                        }
                    }
                } else {
                    // Add each row to the join result or, for outer joins, run through the outer join filter
                    for (; subsetIter.hasNext(); ) {
                        EventBean candidate = subsetIter.next();
                        if (result == null) {
                            result = new HashSet<EventBean>();
                        }
                        result.add(candidate);
                    }
                }
            }
        }

        return result;
    }

    private void recursiveFill(EventBean[] lookupEventsPerStream, Node node) {
        if (node == null) {
            return;
        }

        Node parent = node.getParent();
        if (parent == null) {
            lookupEventsPerStream[rootStreamNum] = node.getParentEvent();
            return;
        }

        lookupEventsPerStream[parent.getStream()] = node.getParentEvent();
        recursiveFill(lookupEventsPerStream, parent);
    }

    public LookupStrategyDesc getStrategyDesc() {
        return null;
    }
}
