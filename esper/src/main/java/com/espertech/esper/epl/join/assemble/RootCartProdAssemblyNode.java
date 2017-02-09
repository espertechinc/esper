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
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.util.IndentWriter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Assembly node for an event stream that is a root with a two or more child nodes below it.
 */
public class RootCartProdAssemblyNode extends BaseAssemblyNode {
    private final int[] childStreamIndex; // maintain mapping of stream number to index in array
    private final List<EventBean[]>[] rowsPerStream;
    private final boolean allSubStreamsOptional;

    // maintain for each child the list of stream number descending that child
    private int[][] subStreamsNumsPerChild;
    private int[][] combinedSubStreams; // for any cartesian product past 2 streams
    private boolean haveChildResults;

    /**
     * Ctor.
     *
     * @param streamNum             - is the stream number
     * @param numStreams            - is the number of streams
     * @param allSubStreamsOptional - true if all substreams are optional and none are required
     * @param childStreamIndex      indexes for child streams
     */
    public RootCartProdAssemblyNode(int streamNum, int numStreams, boolean allSubStreamsOptional, int[] childStreamIndex) {
        super(streamNum, numStreams);
        this.allSubStreamsOptional = allSubStreamsOptional;
        this.childStreamIndex = childStreamIndex;
        rowsPerStream = new List[numStreams];
    }

    public void init(List<Node>[] result) {
        if (subStreamsNumsPerChild == null) {
            if (childNodes.size() < 2) {
                throw new IllegalStateException("Expecting at least 2 child nodes");
            }
            subStreamsNumsPerChild = new int[childNodes.size()][];
            for (int i = 0; i < childNodes.size(); i++) {
                subStreamsNumsPerChild[i] = childNodes.get(i).getSubstreams();
            }

            combinedSubStreams = computeCombined(subStreamsNumsPerChild);
        }

        haveChildResults = false;
        for (int i = 0; i < rowsPerStream.length; i++) {
            rowsPerStream[i] = null;
        }
    }

    public void process(List<Node>[] result, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        // If no child has posted any rows, generate row and done
        if ((!haveChildResults) && allSubStreamsOptional) {
            // post an empty row
            EventBean[] row = new EventBean[numStreams];
            parentNode.result(row, streamNum, null, null, resultFinalRows, resultRootEvent);
            return;
        }

        // Compute the cartesian product
        postCartesian(rowsPerStream, resultFinalRows, resultRootEvent);
    }

    public void result(EventBean[] row, int fromStreamNum, EventBean myEvent, Node myNode, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        haveChildResults = true;

        // fill event in
        row[streamNum] = myEvent;
        int childStreamArrIndex = childStreamIndex[fromStreamNum];

        // keep a reference to the row to build a cartesian product on the call to process
        List<EventBean[]> rows = rowsPerStream[childStreamArrIndex];
        if (rows == null) {
            rows = new LinkedList<EventBean[]>();
            rowsPerStream[childStreamArrIndex] = rows;
        }
        rows.add(row);
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("RootCartProdAssemblyNode streamNum=" + streamNum);
    }

    private void postCartesian(List<EventBean[]>[] rowsPerStream, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        List<EventBean[]> result = new LinkedList<EventBean[]>();
        CartesianUtil.computeCartesian(
                rowsPerStream[0], subStreamsNumsPerChild[0],
                rowsPerStream[1], subStreamsNumsPerChild[1],
                result);

        if (rowsPerStream.length > 2) {
            for (int i = 0; i < subStreamsNumsPerChild.length - 2; i++) {
                List<EventBean[]> product = new LinkedList<EventBean[]>();
                CartesianUtil.computeCartesian(
                        result, combinedSubStreams[i],
                        rowsPerStream[i + 2], subStreamsNumsPerChild[i + 2],
                        product);
                result = product;
            }
        }

        for (EventBean[] row : result) {
            parentNode.result(row, streamNum, null, null, resultFinalRows, resultRootEvent);
        }
    }

    /**
     * Compute an array of supersets of sub stream numbers per stream, for at least 3 or more streams.
     *
     * @param subStreamsPerChild is for each stream number a list of direct child sub streams
     * @return an array in with length (subStreamsPerChild.lenght - 2) in which
     * array[0] contains the streams for subStreamsPerChild[0] and subStreamsPerChild[1] combined, and
     * array[1] contains the streams for subStreamsPerChild[0], subStreamsPerChild[1] and subStreamsPerChild[2] combined
     */
    protected static int[][] computeCombined(int[][] subStreamsPerChild) {
        if (subStreamsPerChild.length < 3) {
            return null;
        }

        // Add all substreams of (1 + 2)  up into = Sum3
        // Then add all substreams of (Sum3 + 3) => Sum4
        // Results in an array of size (subStreamsPerChild.lenght - 2) containing Sum3, Sum4 etc

        int[][] result = new int[subStreamsPerChild.length - 2][];

        result[0] = addSubstreams(subStreamsPerChild[0], subStreamsPerChild[1]);
        for (int i = 0; i < subStreamsPerChild.length - 3; i++) {
            result[i + 1] = addSubstreams(result[i], subStreamsPerChild[i + 2]);
        }

        return result;
    }

    private static int[] addSubstreams(int[] arrayOne, int[] arrayTwo) {
        int[] result = new int[arrayOne.length + arrayTwo.length];

        int count = 0;
        for (int i = 0; i < arrayOne.length; i++) {
            result[count] = arrayOne[i];
            count++;
        }

        for (int i = 0; i < arrayTwo.length; i++) {
            result[count] = arrayTwo[i];
            count++;
        }
        return result;
    }
}
