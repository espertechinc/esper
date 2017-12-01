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
package com.espertech.esper.epl.join.exec.composite;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.lookup.SubordPropHashKey;
import com.espertech.esper.epl.lookup.SubordPropRangeKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompositeIndexQueryFactory {

    public static CompositeIndexQuery makeSubordinate(boolean isNWOnTrigger, int numOuterStreams, Collection<SubordPropHashKey> keyExpr, Class[] coercionKeyTypes, Collection<SubordPropRangeKey> rangeProps, Class[] rangeCoercionTypes, List<String> expressionTexts) {
        // construct chain
        List<CompositeIndexQuery> queries = new ArrayList<CompositeIndexQuery>();
        if (keyExpr.size() > 0) {
            List<QueryGraphValueEntryHashKeyed> hashKeys = new ArrayList<QueryGraphValueEntryHashKeyed>();
            for (SubordPropHashKey keyExp : keyExpr) {
                expressionTexts.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(keyExp.getHashKey().getKeyExpr()));
                hashKeys.add(keyExp.getHashKey());
            }
            queries.add(new CompositeIndexQueryKeyed(isNWOnTrigger, -1, numOuterStreams, hashKeys, coercionKeyTypes));
        }
        int count = 0;
        for (SubordPropRangeKey rangeProp : rangeProps) {
            Class coercionType = rangeCoercionTypes == null ? null : rangeCoercionTypes[count];
            queries.add(new CompositeIndexQueryRange(isNWOnTrigger, -1, numOuterStreams, rangeProp, coercionType, expressionTexts));
            count++;
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

    public static CompositeIndexQuery makeJoinSingleLookupStream(boolean isNWOnTrigger, int lookupStream, List<QueryGraphValueEntryHashKeyed> hashKeys, Class[] keyCoercionTypes, List<QueryGraphValueEntryRange> rangeProps, Class[] rangeCoercionTypes) {
        // construct chain
        List<CompositeIndexQuery> queries = new ArrayList<CompositeIndexQuery>();
        if (hashKeys.size() > 0) {
            queries.add(new CompositeIndexQueryKeyed(false, lookupStream, -1, hashKeys, keyCoercionTypes));
        }
        int count = 0;
        for (QueryGraphValueEntryRange rangeProp : rangeProps) {
            Class coercionType = rangeCoercionTypes == null ? null : rangeCoercionTypes[count];
            SubordPropRangeKey rkey = new SubordPropRangeKey(rangeProp, coercionType);
            queries.add(new CompositeIndexQueryRange(isNWOnTrigger, lookupStream, -1, rkey, coercionType, new ArrayList<String>()));
            count++;
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
