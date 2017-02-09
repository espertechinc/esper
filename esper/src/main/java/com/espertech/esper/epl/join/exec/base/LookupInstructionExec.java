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
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.rep.Repository;
import com.espertech.esper.util.IndentWriter;

import java.util.*;

/**
 * Execution for a lookup instruction to look up in one or more event streams with a supplied event
 * and using a given set of lookup strategies, and adding any lookup results to a lighweight repository object
 * for later result assembly.
 */
public class LookupInstructionExec {
    private final int fromStream;
    private final String fromStreamName;
    private final JoinExecTableLookupStrategy[] lookupStrategies;

    private final int numSubStreams;
    private final Set<EventBean>[] resultPerStream;
    private final int[] requiredSubStreams;
    private final int[] optionalSubStreams;
    private final boolean hasRequiredSubStreams;

    /**
     * Ctor.
     *
     * @param fromStream        - the stream supplying the lookup event
     * @param fromStreamName    - the stream name supplying the lookup event
     * @param toStreams         - the set of streams to look up in
     * @param lookupStrategies  - the strategy to use for each stream to look up in
     * @param requiredPerStream - indicates which of the lookup streams are required to build a result and which are not
     */
    public LookupInstructionExec(int fromStream, String fromStreamName, int[] toStreams, JoinExecTableLookupStrategy[] lookupStrategies, boolean[] requiredPerStream) {
        if (toStreams.length != lookupStrategies.length) {
            throw new IllegalArgumentException("Invalid number of strategies for each stream");
        }
        if (requiredPerStream.length < lookupStrategies.length) {
            throw new IllegalArgumentException("Invalid required per stream array");
        }
        if ((fromStream < 0) || (fromStream >= requiredPerStream.length)) {
            throw new IllegalArgumentException("Invalid from stream");
        }

        this.fromStream = fromStream;
        this.fromStreamName = fromStreamName;
        this.numSubStreams = toStreams.length;
        this.lookupStrategies = lookupStrategies;

        resultPerStream = new Set[numSubStreams];

        // Build a separate array for the required and for the optional streams
        List<Integer> required = new LinkedList<Integer>();
        List<Integer> optional = new LinkedList<Integer>();
        for (int stream : toStreams) {
            if (requiredPerStream[stream]) {
                required.add(stream);
            } else {
                optional.add(stream);
            }
        }
        requiredSubStreams = toArray(required);
        optionalSubStreams = toArray(optional);
        hasRequiredSubStreams = requiredSubStreams.length > 0;
    }

    /**
     * Returns the stream number of the stream supplying the event to use for lookup.
     *
     * @return stream number
     */
    public int getFromStream() {
        return fromStream;
    }

    /**
     * Returns true if there is one or more required substreams or false if no substreams are required joins.
     *
     * @return true if any substreams are required (inner) joins, or false if not
     */
    public boolean hasRequiredStream() {
        return hasRequiredSubStreams;
    }

    /**
     * Execute the instruction adding results to the repository and obtaining events for lookup from the
     * repository.
     *
     * @param repository           supplies events for lookup, and place to add results to
     * @param exprEvaluatorContext expression evaluation context
     * @return true if one or more results, false if no results
     */
    public boolean process(Repository repository, ExprEvaluatorContext exprEvaluatorContext) {
        boolean hasOneResultRow = false;
        Iterator<Cursor> it = repository.getCursors(fromStream);

        // Loop over all events for that stream
        for (; it.hasNext(); ) {
            Cursor cursor = it.next();
            EventBean lookupEvent = cursor.getTheEvent();
            int streamCount = 0;

            // For that event, lookup in all required streams
            while (streamCount < requiredSubStreams.length) {
                Set<EventBean> lookupResult = lookupStrategies[streamCount].lookup(lookupEvent, cursor, exprEvaluatorContext);

                // There is no result, break if this is a required stream
                if (lookupResult == null || lookupResult.isEmpty()) {
                    break;
                }
                resultPerStream[streamCount] = lookupResult;
                streamCount++;
            }

            // No results for a required stream, we are done with this event
            if (streamCount < requiredSubStreams.length) {
                continue;
            } else {
                // Add results to repository
                for (int i = 0; i < requiredSubStreams.length; i++) {
                    hasOneResultRow = true;
                    repository.addResult(cursor, resultPerStream[i], requiredSubStreams[i]);
                }
            }

            // For that event, lookup in all optional streams
            for (int i = 0; i < optionalSubStreams.length; i++) {
                Set<EventBean> lookupResult = lookupStrategies[streamCount].lookup(lookupEvent, cursor, exprEvaluatorContext);

                if (lookupResult != null) {
                    hasOneResultRow = true;
                    repository.addResult(cursor, lookupResult, optionalSubStreams[i]);
                }
                streamCount++;
            }
        }

        return hasOneResultRow;
    }

    private static int[] toArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        int count = 0;
        for (int value : list) {
            arr[count++] = value;
        }
        return arr;
    }

    /**
     * Output the instruction.
     *
     * @param writer is the write to output to
     */
    @SuppressWarnings({"StringContatenationInLoop"})
    public void print(IndentWriter writer) {
        writer.println("LookupInstructionExec" +
                " fromStream=" + fromStream +
                " fromStreamName=" + fromStreamName +
                " numSubStreams=" + numSubStreams +
                " requiredSubStreams=" + Arrays.toString(requiredSubStreams) +
                " optionalSubStreams=" + Arrays.toString(optionalSubStreams));

        writer.incrIndent();
        for (int i = 0; i < lookupStrategies.length; i++) {
            writer.println("lookupStrategies[" + i + "] : " + lookupStrategies[i].toString());
        }
        writer.decrIndent();
    }
}
