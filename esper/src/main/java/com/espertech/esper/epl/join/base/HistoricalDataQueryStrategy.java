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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.view.HistoricalEventViewable;

import java.util.Iterator;
import java.util.Set;

/**
 * Query strategy for use with {@link HistoricalEventViewable}
 * to perform lookup for a given stream using the poll method on a viewable.
 */
public class HistoricalDataQueryStrategy implements QueryStrategy {
    private final int myStreamNumber;
    private final int historicalStreamNumber;
    private final HistoricalEventViewable historicalEventViewable;
    private final EventBean[][] lookupRows1Event;
    private final boolean isOuterJoin;
    private final ExprEvaluator outerJoinCompareNode;
    private final HistoricalIndexLookupStrategy indexLookupStrategy;
    private final PollResultIndexingStrategy pollResultIndexingStrategy;

    /**
     * Ctor.
     *
     * @param myStreamNumber             is the strategy's stream number
     * @param historicalStreamNumber     is the stream number of the view to be polled
     * @param historicalEventViewable    is the view to be polled from
     * @param isOuterJoin                is this is an outer join
     * @param outerJoinCompareNode       is the node to perform the on-comparison for outer joins
     * @param indexLookupStrategy        the strategy to use for limiting the cache result set
     *                                   to only those rows that match filter criteria
     * @param pollResultIndexingStrategy the strategy for indexing poll-results such that a
     *                                   strategy can use the index instead of a full table scan to resolve rows
     */
    public HistoricalDataQueryStrategy(int myStreamNumber,
                                       int historicalStreamNumber,
                                       HistoricalEventViewable historicalEventViewable,
                                       boolean isOuterJoin,
                                       ExprEvaluator outerJoinCompareNode,
                                       HistoricalIndexLookupStrategy indexLookupStrategy,
                                       PollResultIndexingStrategy pollResultIndexingStrategy) {
        this.myStreamNumber = myStreamNumber;
        this.historicalStreamNumber = historicalStreamNumber;
        this.historicalEventViewable = historicalEventViewable;
        this.isOuterJoin = isOuterJoin;
        this.outerJoinCompareNode = outerJoinCompareNode;

        lookupRows1Event = new EventBean[1][];
        lookupRows1Event[0] = new EventBean[2];

        this.indexLookupStrategy = indexLookupStrategy;
        this.pollResultIndexingStrategy = pollResultIndexingStrategy;
    }

    public void lookup(EventBean[] lookupEvents, Set<MultiKey<EventBean>> joinSet, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[][] lookupRows;

        // If looking up a single event, reuse the buffered array
        if (lookupEvents.length == 1) {
            lookupRows = lookupRows1Event;
            lookupRows[0][myStreamNumber] = lookupEvents[0];
        } else {
            // Prepare rows with each row N events where N is the number of streams
            lookupRows = new EventBean[lookupEvents.length][];
            for (int i = 0; i < lookupEvents.length; i++) {
                lookupRows[i] = new EventBean[2];
                lookupRows[i][myStreamNumber] = lookupEvents[i];
            }
        }

        EventTable[][] indexPerLookupRow = historicalEventViewable.poll(lookupRows, pollResultIndexingStrategy, exprEvaluatorContext);

        int count = 0;
        for (EventTable[] index : indexPerLookupRow) {
            // Using the index, determine a subset of the whole indexed table to process, unless
            // the strategy is a full table scan
            Iterator<EventBean> subsetIter = indexLookupStrategy.lookup(lookupEvents[count], index, exprEvaluatorContext);

            // In an outer join
            if (isOuterJoin && ((subsetIter == null || (!subsetIter.hasNext())))) {
                EventBean[] resultRow = new EventBean[2];
                resultRow[myStreamNumber] = lookupEvents[count];
                joinSet.add(new MultiKey<EventBean>(resultRow));
            } else {
                boolean foundMatch = false;
                if (subsetIter != null) {
                    // Add each row to the join result or, for outer joins, run through the outer join filter
                    for (; subsetIter.hasNext(); ) {
                        EventBean[] resultRow = new EventBean[2];
                        resultRow[myStreamNumber] = lookupEvents[count];
                        resultRow[historicalStreamNumber] = subsetIter.next();

                        // In an outer join compare the on-fields
                        if (outerJoinCompareNode != null) {
                            Boolean compareResult = (Boolean) outerJoinCompareNode.evaluate(resultRow, true, exprEvaluatorContext);
                            if ((compareResult != null) && compareResult) {
                                joinSet.add(new MultiKey<EventBean>(resultRow));
                                foundMatch = true;
                            }
                        } else {
                            joinSet.add(new MultiKey<EventBean>(resultRow));
                        }
                    }
                }

                if (isOuterJoin && (!foundMatch)) {
                    EventBean[] resultRow = new EventBean[2];
                    resultRow[myStreamNumber] = lookupEvents[count];
                    joinSet.add(new MultiKey<EventBean>(resultRow));
                }
            }
            count++;
        }
    }
}
