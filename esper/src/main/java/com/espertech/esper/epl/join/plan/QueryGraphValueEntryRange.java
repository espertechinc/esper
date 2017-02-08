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

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

public abstract class QueryGraphValueEntryRange implements QueryGraphValueEntry, Serializable {

    private static final long serialVersionUID = 8991572541148988925L;

    private final QueryGraphRangeEnum type;

    protected QueryGraphValueEntryRange(QueryGraphRangeEnum type) {
        this.type = type;
    }

    public QueryGraphRangeEnum getType() {
        return type;
    }

    public abstract String toQueryPlan();

    public abstract ExprNode[] getExpressions();

    public static String toQueryPlan(List<QueryGraphValueEntryRange> rangeKeyPairs) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (QueryGraphValueEntryRange item : rangeKeyPairs) {
            writer.write(delimiter);
            writer.write(item.toQueryPlan());
            delimiter = ", ";
        }
        return writer.toString();
    }
}
