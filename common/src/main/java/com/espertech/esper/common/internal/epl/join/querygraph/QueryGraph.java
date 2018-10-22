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

import java.util.Collections;
import java.util.Map;

public class QueryGraph {
    private final Map<UniformPair<Integer>, QueryGraphValue> streamJoinMap;

    public QueryGraph(Map<UniformPair<Integer>, QueryGraphValue> streamJoinMap) {
        this.streamJoinMap = streamJoinMap;
    }

    public QueryGraphValue getGraphValue(int streamLookup, int streamIndexed) {
        UniformPair<Integer> key = new UniformPair<>(streamLookup, streamIndexed);
        QueryGraphValue value = streamJoinMap.get(key);
        if (value != null) {
            return value;
        }
        return new QueryGraphValue(Collections.emptyList());
    }
}
