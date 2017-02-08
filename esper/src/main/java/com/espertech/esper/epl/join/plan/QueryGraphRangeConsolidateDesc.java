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

public class QueryGraphRangeConsolidateDesc {
    private QueryGraphRangeEnum type;
    private boolean isReverse;

    public QueryGraphRangeConsolidateDesc(QueryGraphRangeEnum type, boolean reverse) {
        this.type = type;
        isReverse = reverse;
    }

    public QueryGraphRangeEnum getType() {
        return type;
    }

    public boolean isReverse() {
        return isReverse;
    }
}
