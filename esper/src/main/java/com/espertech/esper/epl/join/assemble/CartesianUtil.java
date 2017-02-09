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
package com.espertech.esper.epl.join.assemble;

import com.espertech.esper.client.EventBean;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class to compute the cartesian product of the events from two streams.
 */
public class CartesianUtil {
    /**
     * Form the 2-ary cartesian product between zero or more events from 2 streams.
     *
     * @param streamOne        is the events from stream one
     * @param subStreamNumsOne is the list of substream numbers to stream one to include in the product
     * @param streamTwo        is the events from stream two
     * @param subStreamNumsTwo is the list of substream numbers to stream two to include in the product
     * @param resultList       is where the result of the cartesian product is added to
     */

    protected static void computeCartesian(List<EventBean[]> streamOne, int[] subStreamNumsOne,
                                           List<EventBean[]> streamTwo, int[] subStreamNumsTwo,
                                           List<EventBean[]> resultList) {

        if ((streamTwo == null) || (streamTwo.isEmpty())) {
            if ((streamOne == null) || (streamOne.isEmpty())) {
                return;
            }
            resultList.addAll(streamOne);
            return;
        }

        if ((streamOne == null) || (streamOne.isEmpty())) {
            resultList.addAll(streamTwo);
            return;
        }

        int streamOneSize = streamOne.size();
        int streamTwoSize = streamTwo.size();

        if (streamOneSize == 1) {
            // Yes we are re-using the results of stream two, same row reference
            copyToEach(subStreamNumsOne, streamOne.get(0), streamTwo);
            resultList.addAll(streamTwo);
            return;
        }

        if (streamTwoSize == 1) {
            // Yes we are re-using the results of stream one, same row reference
            copyToEach(subStreamNumsTwo, streamTwo.get(0), streamOne);
            resultList.addAll(streamOne);
            return;
        }

        // we have more then 1 rows each child stream

        // Exchange streams if one is smaller then two
        // Since if one has 100 rows the other has 2 then we can re-use the 100 event rows.
        if (streamTwoSize > streamOneSize) {
            List<EventBean[]> holdRows = streamOne;
            int holdSize = streamOneSize;

            streamOne = streamTwo;
            streamOneSize = streamTwoSize;

            streamTwo = holdRows;
            streamTwoSize = holdSize;
            subStreamNumsTwo = subStreamNumsOne;
        }

        // allocate resultList of join
        int cartesianTotalRows = streamOneSize * streamTwoSize;
        int numColumns = streamOne.get(0).length;
        EventBean[][] results = new EventBean[cartesianTotalRows][];

        // Allocate and pre-populate copies of stream 1
        int streamOneCount = 0;
        for (EventBean[] row : streamOne) {
            // first use all events in stream 1
            results[streamOneCount] = row;

            // then allocate copies for each in stream 2
            for (int i = 1; i < streamTwoSize; i++) {
                EventBean[] dupRow = new EventBean[numColumns];
                System.arraycopy(row, 0, dupRow, 0, numColumns);

                int index = streamOneSize * i + streamOneCount;
                results[index] = dupRow;
            }

            streamOneCount++;
        }

        // Copy stream 2 rows into rows of stream 1
        int streamTwoCount = 0;
        for (EventBean[] row : streamTwo) {
            for (int i = 0; i < streamOneSize; i++) {
                int index = streamTwoCount * streamOneSize + i;
                copy(subStreamNumsTwo, row, results[index]);
            }
            streamTwoCount++;
        }

        // Add results
        resultList.addAll(Arrays.asList(results));
    }

    private static void copyToEach(int[] subStreamNums, EventBean[] sourceRow, List<EventBean[]> destRows) {
        for (EventBean[] destRow : destRows) {
            copy(subStreamNums, sourceRow, destRow);
        }
    }

    private static void copy(int[] subStreamsFrom, EventBean[] from, EventBean[] to) {
        for (int index : subStreamsFrom) {
            to[index] = from[index];
        }
    }
}
