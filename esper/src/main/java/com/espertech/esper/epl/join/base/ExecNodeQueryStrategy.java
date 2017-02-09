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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.base.ExecNode;

import java.util.ArrayDeque;
import java.util.Set;

/**
 * Query strategy for building a join tuple set by using an execution node tree.
 */
public class ExecNodeQueryStrategy implements QueryStrategy {
    private int forStream;
    private int numStreams;
    private ExecNode execNode;

    /**
     * CTor.
     *
     * @param forStream  - stream the strategy is for
     * @param numStreams - number of streams in total
     * @param execNode   - execution node for building join tuple set
     */
    public ExecNodeQueryStrategy(int forStream, int numStreams, ExecNode execNode) {
        this.forStream = forStream;
        this.numStreams = numStreams;
        this.execNode = execNode;
    }

    public void lookup(EventBean[] lookupEvents, Set<MultiKey<EventBean>> joinSet, ExprEvaluatorContext exprEvaluatorContext) {
        if (lookupEvents == null || lookupEvents.length == 0) {
            return;
        }

        ArrayDeque<EventBean[]> results = new ArrayDeque<EventBean[]>();
        for (EventBean theEvent : lookupEvents) {
            // Set up prototype row
            EventBean[] prototype = new EventBean[numStreams];
            prototype[forStream] = theEvent;

            // Perform execution
            execNode.process(theEvent, prototype, results, exprEvaluatorContext);

            // Convert results into unique set
            for (EventBean[] row : results) {
                joinSet.add(new MultiKey<EventBean>(row));
            }
            results.clear();
        }
    }

    /**
     * Return stream number this strategy is for.
     *
     * @return stream num
     */
    protected int getForStream() {
        return forStream;
    }

    /**
     * Returns the total number of streams.
     *
     * @return number of streams
     */
    protected int getNumStreams() {
        return numStreams;
    }

    /**
     * Returns execution node.
     *
     * @return execution node
     */
    protected ExecNode getExecNode() {
        return execNode;
    }
}
