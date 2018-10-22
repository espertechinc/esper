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
package com.espertech.esper.common.internal.epl.join.exec.composite;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;

import java.util.ArrayList;
import java.util.List;

public class CompositeIndexQueryFactory {

    public static CompositeIndexQuery makeSubordinate(boolean isNWOnTrigger, int numOuterStreams,
                                                      ExprEvaluator hashEval, QueryGraphValueEntryRange[] rangeEvals) {
        // construct chain
        List<CompositeIndexQuery> queries = new ArrayList<CompositeIndexQuery>();
        if (hashEval != null) {
            queries.add(new CompositeIndexQueryKeyed(isNWOnTrigger, -1, numOuterStreams, hashEval));
        }
        for (QueryGraphValueEntryRange rangeProp : rangeEvals) {
            queries.add(new CompositeIndexQueryRange(isNWOnTrigger, -1, numOuterStreams, rangeProp));
        }

        // Hook up as chain for remove
        CompositeIndexQuery last = null;
        for (CompositeIndexQuery action : queries) {
            if (last != null) {
                last.setNext(action);
            }
            last = action;
        }
        return queries.get(0);
    }

    public static CompositeIndexQuery makeJoinSingleLookupStream(boolean isNWOnTrigger, int lookupStream, ExprEvaluator hashGetter, QueryGraphValueEntryRange[] rangeProps) {
        // construct chain
        List<CompositeIndexQuery> queries = new ArrayList<CompositeIndexQuery>();
        if (hashGetter != null) {
            queries.add(new CompositeIndexQueryKeyed(false, lookupStream, -1, hashGetter));
        }
        for (QueryGraphValueEntryRange rangeProp : rangeProps) {
            queries.add(new CompositeIndexQueryRange(isNWOnTrigger, lookupStream, -1, rangeProp));
        }

        // Hook up as chain for remove
        CompositeIndexQuery last = null;
        for (CompositeIndexQuery action : queries) {
            if (last != null) {
                last.setNext(action);
            }
            last = action;
        }
        return queries.get(0);
    }
}
