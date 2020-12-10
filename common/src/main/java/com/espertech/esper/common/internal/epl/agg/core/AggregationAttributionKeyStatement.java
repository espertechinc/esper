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

public class AggregationAttributionKeyStatement implements AggregationAttributionKey {
    public final static AggregationAttributionKeyStatement INSTANCE = new AggregationAttributionKeyStatement();

    private AggregationAttributionKeyStatement() {
    }

    public <T> T accept(AggregationAttributionKeyVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
