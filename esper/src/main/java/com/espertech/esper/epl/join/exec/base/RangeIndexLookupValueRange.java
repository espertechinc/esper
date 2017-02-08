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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.epl.join.plan.QueryGraphRangeEnum;

public class RangeIndexLookupValueRange extends RangeIndexLookupValue {
    private QueryGraphRangeEnum operator;
    private boolean isAllowRangeReverse;

    public RangeIndexLookupValueRange(Object value, QueryGraphRangeEnum operator, boolean allowRangeReverse) {
        super(value);
        this.operator = operator;
        isAllowRangeReverse = allowRangeReverse;
    }

    public QueryGraphRangeEnum getOperator() {
        return operator;
    }

    public boolean isAllowRangeReverse() {
        return isAllowRangeReverse;
    }
}
