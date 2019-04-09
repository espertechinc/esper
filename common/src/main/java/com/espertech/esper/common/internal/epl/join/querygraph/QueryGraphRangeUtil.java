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

import java.util.HashMap;
import java.util.Map;

/**
 * Property lists stored as a value for each stream-to-stream relationship, for use by {@link QueryGraphForge}.
 */
public class QueryGraphRangeUtil {
    private static final Map<UniformPair<QueryGraphRangeEnum>, QueryGraphRangeConsolidateDesc> OPS_TABLE = new HashMap<>();

    static {
        add(QueryGraphRangeEnum.LESS_OR_EQUAL, QueryGraphRangeEnum.GREATER_OR_EQUAL, QueryGraphRangeEnum.RANGE_CLOSED);
        add(QueryGraphRangeEnum.LESS, QueryGraphRangeEnum.GREATER, QueryGraphRangeEnum.RANGE_OPEN);
        add(QueryGraphRangeEnum.LESS_OR_EQUAL, QueryGraphRangeEnum.GREATER, QueryGraphRangeEnum.RANGE_HALF_CLOSED);
        add(QueryGraphRangeEnum.LESS, QueryGraphRangeEnum.GREATER_OR_EQUAL, QueryGraphRangeEnum.RANGE_HALF_OPEN);
    }

    private static void add(QueryGraphRangeEnum opOne, QueryGraphRangeEnum opTwo, QueryGraphRangeEnum range) {
        UniformPair<QueryGraphRangeEnum> keyOne = getKey(opOne, opTwo);
        OPS_TABLE.put(keyOne, new QueryGraphRangeConsolidateDesc(range, false));
        UniformPair<QueryGraphRangeEnum> keyRev = getKey(opTwo, opOne);
        OPS_TABLE.put(keyRev, new QueryGraphRangeConsolidateDesc(range, true));
    }

    private static UniformPair<QueryGraphRangeEnum> getKey(QueryGraphRangeEnum op1, QueryGraphRangeEnum op2) {
        return new UniformPair<>(op1, op2);
    }

    public static QueryGraphRangeConsolidateDesc getCanConsolidate(QueryGraphRangeEnum op1, QueryGraphRangeEnum op2) {
        return OPS_TABLE.get(getKey(op1, op2));
    }
}

