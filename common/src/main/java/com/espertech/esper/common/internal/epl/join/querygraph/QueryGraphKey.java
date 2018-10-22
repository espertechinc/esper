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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.collection.UniformPair;

/**
 * Key consisting of 2 integer stream numbers, for use by {@link QueryGraphForge}.
 */
public class QueryGraphKey {
    private UniformPair<Integer> streams;

    /**
     * Ctor.
     *
     * @param streamOne - from stream
     * @param streamTwo - to stream
     */
    public QueryGraphKey(int streamOne, int streamTwo) {
        streams = new UniformPair<Integer>(streamOne, streamTwo);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof QueryGraphKey)) {
            return false;
        }

        QueryGraphKey other = (QueryGraphKey) obj;
        return other.streams.equals(this.streams);
    }

    public int hashCode() {
        return streams.hashCode();
    }

    public UniformPair<Integer> getStreams() {
        return streams;
    }

    public String toString() {
        return "QueryGraphKey " + streams.getFirst() + " and " + streams.getSecond();
    }
}

