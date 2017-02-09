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
package com.espertech.esper.epl.join.plan;

import java.util.*;

/**
 * This class represents outer-join relationships between outer and inner tables.
 * To add a left outer join between streams 0 and 1 use "add(0, 1)".
 * To add a full outer join between streams 0 and 1 use "add(0, 1)" and "add(1, 0)".
 * To add a right outer join between streams 0 and 1 use "add(1, 0)".
 */
public class OuterInnerDirectionalGraph {
    private final Map<Integer, Set<Integer>> streamToInnerMap;
    private final Map<Integer, Set<Integer>> unqualifiedNavigableStreams;
    private final int numStreams;

    /**
     * Ctor.
     *
     * @param numStreams - number of streams
     */
    public OuterInnerDirectionalGraph(int numStreams) {
        this.numStreams = numStreams;
        this.streamToInnerMap = new HashMap<Integer, Set<Integer>>();
        this.unqualifiedNavigableStreams = new HashMap<Integer, Set<Integer>>();
    }

    /**
     * Add an outer-to-inner join stream relationship.
     *
     * @param outerStream is the stream number of the outer stream
     * @param innerStream is the stream number of the inner stream
     * @return graph object
     */
    public OuterInnerDirectionalGraph add(int outerStream, int innerStream) {
        checkArgs(outerStream, innerStream);

        // add set
        Set<Integer> innerSet = streamToInnerMap.get(outerStream);
        if (innerSet == null) {
            innerSet = new HashSet<Integer>();
            streamToInnerMap.put(outerStream, innerSet);
        }

        // populate
        if (innerSet.contains(innerStream)) {
            throw new IllegalArgumentException("Inner stream already in collection");
        }
        innerSet.add(innerStream);

        return this;
    }

    /**
     * Returns the set of inner streams for the given outer stream number.
     *
     * @param outerStream is the stream number of the outer stream
     * @return set of inner streams, or null if empty
     */
    public Set<Integer> getInner(int outerStream) {
        checkArgs(outerStream);
        return streamToInnerMap.get(outerStream);
    }

    /**
     * Returns the set of outer streams for the given inner stream number.
     *
     * @param innerStream is the stream number of the inner stream
     * @return set of outer streams, or null if empty
     */
    public Set<Integer> getOuter(int innerStream) {
        checkArgs(innerStream);

        Set<Integer> result = new HashSet<Integer>();
        for (Integer key : streamToInnerMap.keySet()) {
            Set<Integer> set = streamToInnerMap.get(key);
            if (set.contains(innerStream)) {
                result.add(key);
            }
        }

        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * Returns true if the outer stream has an optional relationship to the inner stream.
     *
     * @param outerStream is the stream number of the outer stream
     * @param innerStream is the stream number of the inner stream
     * @return true if outer-inner relationship between streams, false if not
     */
    public boolean isInner(int outerStream, int innerStream) {
        checkArgs(outerStream, innerStream);

        Set<Integer> innerSet = streamToInnerMap.get(outerStream);
        if (innerSet == null) {
            return false;
        }
        return innerSet.contains(innerStream);
    }

    /**
     * Returns true if the inner stream has a relationship to the outer stream.
     *
     * @param outerStream is the stream number of the outer stream
     * @param innerStream is the stream number of the inner stream
     * @return true if outer-inner relationship between streams, false if not
     */
    public boolean isOuter(int outerStream, int innerStream) {
        checkArgs(outerStream, innerStream);
        Set<Integer> outerStreams = getOuter(innerStream);

        if (outerStreams == null) {
            return false;
        }
        return outerStreams.contains(outerStream);
    }

    /**
     * Prints out collection.
     *
     * @return textual output of keys and values
     */
    public String print() {
        StringBuilder buffer = new StringBuilder();
        String delimiter = "";

        for (Integer key : streamToInnerMap.keySet()) {
            Set<Integer> set = streamToInnerMap.get(key);

            buffer.append(delimiter);
            buffer.append(key);
            buffer.append('=');
            buffer.append(Arrays.toString(set.toArray()));

            delimiter = ", ";
        }
        return buffer.toString();
    }

    public Map<Integer, Set<Integer>> getUnqualifiedNavigableStreams() {
        return unqualifiedNavigableStreams;
    }

    public void addUnqualifiedNavigable(int streamOne, int streamTwo) {
        addUnqualifiedInternal(streamOne, streamTwo);
        addUnqualifiedInternal(streamTwo, streamOne);
    }

    private void addUnqualifiedInternal(int streamOne, int streamTwo) {
        Set<Integer> set = unqualifiedNavigableStreams.get(streamOne);
        if (set == null) {
            set = new HashSet<Integer>();
            unqualifiedNavigableStreams.put(streamOne, set);
        }
        set.add(streamTwo);
    }

    private void checkArgs(int stream) {
        if ((stream >= numStreams) || (stream < 0)) {
            throw new IllegalArgumentException("Out of bounds parameter for stream num");
        }
    }

    private void checkArgs(int outerStream, int innerStream) {
        if ((outerStream >= numStreams) || (innerStream >= numStreams) ||
                (outerStream < 0) || (innerStream < 0)) {
            throw new IllegalArgumentException("Out of bounds parameter for inner or outer stream num");
        }
        if (outerStream == innerStream) {
            throw new IllegalArgumentException("Unexpected equal stream num for inner and outer stream");
        }
    }
}
