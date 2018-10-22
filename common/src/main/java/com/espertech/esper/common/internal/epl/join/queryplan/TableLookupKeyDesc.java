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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;

import java.util.List;

public class TableLookupKeyDesc {
    private List<QueryGraphValueEntryHashKeyedForge> hashes;
    private List<QueryGraphValueEntryRangeForge> ranges;

    public TableLookupKeyDesc(List<QueryGraphValueEntryHashKeyedForge> hashes, List<QueryGraphValueEntryRangeForge> ranges) {
        this.hashes = hashes;
        this.ranges = ranges;
    }

    public List<QueryGraphValueEntryHashKeyedForge> getHashes() {
        return hashes;
    }

    public ExprNode[] getHashExpressions() {
        ExprNode[] nodes = new ExprNode[hashes.size()];
        for (int i = 0; i < hashes.size(); i++) {
            nodes[i] = hashes.get(i).getKeyExpr();
        }
        return nodes;
    }

    public List<QueryGraphValueEntryRangeForge> getRanges() {
        return ranges;
    }

    public String toString() {

        return "TableLookupKeyDesc{" +
                "hash=" + QueryGraphValueEntryHashKeyedForge.toQueryPlan(hashes) +
                ", btree=" + QueryGraphValueEntryRangeForge.toQueryPlan(ranges) +
                '}';
    }
}
