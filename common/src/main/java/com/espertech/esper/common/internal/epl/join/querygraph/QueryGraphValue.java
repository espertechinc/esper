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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Property lists stored as a value for each stream-to-stream relationship, for use by {@link QueryGraphForge}.
 */
public class QueryGraphValue {
    private List<QueryGraphValueDesc> items;

    public QueryGraphValue(List<QueryGraphValueDesc> items) {
        this.items = items;
    }

    public List<QueryGraphValueDesc> getItems() {
        return items;
    }

    public QueryGraphValuePairHashKeyIndex getHashKeyProps() {
        List<QueryGraphValueEntryHashKeyed> keys = new ArrayList<>();
        Deque<String> indexed = new ArrayDeque<String>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryHashKeyed) {
                QueryGraphValueEntryHashKeyed keyprop = (QueryGraphValueEntryHashKeyed) desc.getEntry();
                keys.add(keyprop);
                indexed.add(desc.getIndexExprs()[0]);
            }
        }
        return new QueryGraphValuePairHashKeyIndex(indexed.toArray(new String[indexed.size()]), keys);
    }

    public QueryGraphValuePairRangeIndex getRangeProps() {
        Deque<String> indexed = new ArrayDeque<>();
        List<QueryGraphValueEntryRange> keys = new ArrayList<>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryRange) {
                QueryGraphValueEntryRange keyprop = (QueryGraphValueEntryRange) desc.getEntry();
                keys.add(keyprop);
                indexed.add(desc.getIndexExprs()[0]);
            }
        }
        return new QueryGraphValuePairRangeIndex(indexed.toArray(new String[indexed.size()]), keys);
    }

    public QueryGraphValuePairInKWSingleIdx getInKeywordSingles() {
        List<String> indexedProps = new ArrayList<>();
        List<QueryGraphValueEntryInKeywordSingleIdx> single = new ArrayList<>();
        for (QueryGraphValueDesc desc : items) {
            if (desc.getEntry() instanceof QueryGraphValueEntryInKeywordSingleIdx) {
                QueryGraphValueEntryInKeywordSingleIdx keyprop = (QueryGraphValueEntryInKeywordSingleIdx) desc.getEntry();
                single.add(keyprop);
                indexedProps.add(desc.getIndexExprs()[0]);
            }
        }
        return new QueryGraphValuePairInKWSingleIdx(indexedProps.toArray(new String[indexedProps.size()]), single);
    }
}

