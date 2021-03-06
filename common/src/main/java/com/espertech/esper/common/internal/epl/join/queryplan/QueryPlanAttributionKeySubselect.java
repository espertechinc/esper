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

public class QueryPlanAttributionKeySubselect implements QueryPlanAttributionKey {
    private final int subqueryNum;

    public QueryPlanAttributionKeySubselect(int subqueryNum) {
        this.subqueryNum = subqueryNum;
    }

    public int getSubqueryNum() {
        return subqueryNum;
    }

    public <T> T accept(QueryPlanAttributionKeyVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
