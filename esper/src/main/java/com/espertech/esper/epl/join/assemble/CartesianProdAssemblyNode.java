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

import java.util.*;

/**
 * Assembly node for an event stream that is a branch with a two or more child nodes (required and optional) below it.
 */
public class CartesianProdAssemblyNode extends BaseAssemblyNode {
    private final int[] childStreamIndex; // maintain mapping of stream number to index in array
    private final boolean allSubStreamsOptional;

    // keep a reference to results for processing optional child nodes not generating results
    private List<Node> resultsForStream;

    // maintain for each child the list of stream number descending that child
    private int[][] subStreamsNumsPerChild;
    private int[][] combinedSubStreams; // for any cartesian product past 2 streams

    // For tracking when we only have a single event for this stream as a result
    private Node singleResultNode;
    private EventBean singleResultParentEvent;
    private List<EventBean[]>[] singleResultRowsPerStream;
    private boolean haveChildResults;

    // For tracking when we have multiple events for this stream
    private Map<EventBean, ChildStreamResults> completedEvents;

    /**
     * Ctor.
     *
     * @param streamNum             - is the stream number
     * @param numStreams            - is the number of streams
     * @param allSubStreamsOptional - true if all child nodes to this node are optional, or false if
     *                              one or more child nodes are required for a result.
     * @param childStreamIndex      indexes for child streams
     */
    public CartesianProdAssemblyNode(int streamNum, int numStreams, boolean allSubStreamsOptional, int[] childStreamIndex) {
        super(streamNum, numStreams);
        this.childStreamIndex = childStreamIndex;
        this.allSubStreamsOptional = allSubStreamsOptional;
    }

    public void init(List<Node>[] result) {
        resultsForStream = result[streamNum];
        singleResultNode = null;
        singleResultParentEvent = null;
        singleResultRowsPerStream = null;
        haveChildResults = false;

        if (subStreamsNumsPerChild == null) {
            if (childNodes.size() < 2) {
                throw new IllegalStateException("Expecting at least 2 child nodes");
            }
            subStreamsNumsPerChild = new int[childNodes.size()][];
            for (int i = 0; i < childNodes.size(); i++) {
                subStreamsNumsPerChild[i] = childNodes.get(i).getSubstreams();
            }

            combinedSubStreams = RootCartProdAssemblyNode.computeCombined(subStreamsNumsPerChild);
        }

        if (resultsForStream != null) {
            int numNodes = resultsForStream.size();
            if (numNodes == 1) {
                Node node = resultsForStream.get(0);
                Set<EventBean> nodeEvents = node.getEvents();

                // If there is a single result event (typical case)
                if (nodeEvents.size() == 1) {
                    singleResultNode = node;
                    singleResultParentEvent = nodeEvents.iterator().next();
                    singleResultRowsPerStream = new LinkedList[childNodes.size()];
                }
            }

            if (singleResultNode == null) {
                completedEvents = new HashMap<EventBean, ChildStreamResults>();
            }
        } else {
            completedEvents = new HashMap<EventBean, ChildStreamResults>();
        }
    }

    public void process(List<Node>[] result, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        // there cannot be child nodes to compute a cartesian product if this node had no results
        if (resultsForStream == null) {
            return;
        }

        // If this node's result set consisted of a single event
        if (singleResultNode != null) {
            // If no child has posted any rows
            if (!haveChildResults) {
                // And all substreams are optional, generate a row
                if (allSubStreamsOptional) {
                    EventBean[] row = new EventBean[numStreams];
                    row[streamNum] = singleResultParentEvent;
                    parentNode.result(row, streamNum, singleResultNode.getParentEvent(), singleResultNode, resultFinalRows, resultRootEvent);
                }
                return;
            }

            // Compute the cartesian product
            postCartesian(singleResultRowsPerStream, singleResultNode, resultFinalRows, resultRootEvent);
            return;
        }

        // We have multiple events for this node, generate an event row for each event not yet received from
        // event rows generated by the child node.
        for (Node node : resultsForStream) {
            Set<EventBean> events = node.getEvents();
            for (EventBean theEvent : events) {
                ChildStreamResults results = completedEvents.get(theEvent);

                // If there were no results for the event posted by any child nodes
                if (results == null) {
                    if (allSubStreamsOptional) {
                        EventBean[] row = new EventBean[numStreams];
                        row[streamNum] = theEvent;
                        parentNode.result(row, streamNum, node.getParentEvent(), node.getParent(), resultFinalRows, resultRootEvent);
                    }
                    continue;
                }

                // Compute the cartesian product
                postCartesian(results.getRowsPerStream(), node, resultFinalRows, resultRootEvent);
            }
        }
    }

    private void postCartesian(List<EventBean[]>[] rowsPerStream, Node node, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
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
            parentNode.result(row, streamNum, node.getParentEvent(), node.getParent(), resultFinalRows, resultRootEvent);
        }
    }

    public void result(EventBean[] row, int fromStreamNum, EventBean myEvent, Node myNode, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
        // fill event in
        row[streamNum] = myEvent;
        int childStreamArrIndex = childStreamIndex[fromStreamNum];

        // treat single-event result for this stream
        if (singleResultNode != null) {
            // record the fact that an event that was generated by a child
            haveChildResults = true;

            if (singleResultRowsPerStream == null) {
                singleResultRowsPerStream = new LinkedList[childNodes.size()];
            }

            List<EventBean[]> streamRows = singleResultRowsPerStream[childStreamArrIndex];
            if (streamRows == null) {
                streamRows = new LinkedList<EventBean[]>();
                singleResultRowsPerStream[childStreamArrIndex] = streamRows;
            }

            streamRows.add(row);
            return;
        }

        ChildStreamResults childStreamResults = completedEvents.get(myEvent);
        if (childStreamResults == null) {
            childStreamResults = new ChildStreamResults(childNodes.size());
            completedEvents.put(myEvent, childStreamResults);
        }

        childStreamResults.add(childStreamArrIndex, row);
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("CartesianProdAssemblyNode streamNum=" + streamNum);
    }

    /**
     * Structure to represent a list of event result rows per stream.
     */
    public static class ChildStreamResults {
        private List<EventBean[]>[] rowsPerStream;

        /**
         * Ctor.
         *
         * @param size - number of streams
         */
        public ChildStreamResults(int size) {
            this.rowsPerStream = new LinkedList[size];
        }

        /**
         * Add result from stream.
         *
         * @param fromStreamIndex - from stream
         * @param row             - row to add
         */
        public void add(int fromStreamIndex, EventBean[] row) {
            List<EventBean[]> rows = rowsPerStream[fromStreamIndex];
            if (rows == null) {
                rows = new LinkedList<EventBean[]>();
                rowsPerStream[fromStreamIndex] = rows;
            }

            rows.add(row);
        }

        /**
         * Returns rows per stream.
         *
         * @return rows per stream
         */
        public List<EventBean[]>[] getRowsPerStream() {
            return rowsPerStream;
        }
    }
}
