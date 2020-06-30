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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public abstract class QueryGraphValueEntryRange implements QueryGraphValueEntry {
    public final static EPTypeClass EPTYPE = new EPTypeClass(QueryGraphValueEntryRange.class);
    public final static EPTypeClass EPTYPEARRAY = new EPTypeClass(QueryGraphValueEntryRange[].class);

    protected final QueryGraphRangeEnum type;

    protected QueryGraphValueEntryRange(QueryGraphRangeEnum type) {
        this.type = type;
    }

    public QueryGraphRangeEnum getType() {
        return type;
    }

    public abstract ExprEvaluator[] getExpressions();
}
