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

public class AggregationAttributionKeyView implements AggregationAttributionKey {
    private final int streamNum;
    private final Integer subqueryNum;
    private final int[] grouping;

    public AggregationAttributionKeyView(int streamNum, Integer subqueryNum, int[] grouping) {
        this.streamNum = streamNum;
        this.subqueryNum = subqueryNum;
        this.grouping = grouping;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public Integer getSubqueryNum() {
        return subqueryNum;
    }

    public int[] getGrouping() {
        return grouping;
    }

    public <T> T accept(AggregationAttributionKeyVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
