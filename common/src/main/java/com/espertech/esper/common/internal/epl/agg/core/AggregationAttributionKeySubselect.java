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
package com.espertech.esper.common.internal.epl.agg.core;

public class AggregationAttributionKeySubselect implements AggregationAttributionKey {
    private final int subqueryNumber;

    public AggregationAttributionKeySubselect(int subqueryNumber) {
        this.subqueryNumber = subqueryNumber;
    }

    public int getSubqueryNumber() {
        return subqueryNumber;
    }

    public <T> T accept(AggregationAttributionKeyVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
