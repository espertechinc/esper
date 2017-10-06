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
package com.espertech.esper.epl.agg.util;

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;
import com.espertech.esper.epl.expression.core.ExprForge;

public class AggregationLocalGroupByColumnForge {
    private final boolean defaultGroupLevel;
    private final ExprForge[] partitionForges;
    private final int methodOffset;
    private final boolean methodAgg;
    private final AggregationAccessorSlotPairForge pair;
    private final int levelNum;

    public AggregationLocalGroupByColumnForge(boolean defaultGroupLevel, ExprForge[] partitionForges, int methodOffset, boolean methodAgg, AggregationAccessorSlotPairForge pair, int levelNum) {
        this.defaultGroupLevel = defaultGroupLevel;
        this.partitionForges = partitionForges;
        this.methodOffset = methodOffset;
        this.methodAgg = methodAgg;
        this.pair = pair;
        this.levelNum = levelNum;
    }

    public ExprForge[] getPartitionForges() {
        return partitionForges;
    }

    public int getMethodOffset() {
        return methodOffset;
    }

    public boolean isDefaultGroupLevel() {
        return defaultGroupLevel;
    }

    public boolean isMethodAgg() {
        return methodAgg;
    }

    public AggregationAccessorSlotPairForge getPair() {
        return pair;
    }

    public int getLevelNum() {
        return levelNum;
    }
}
