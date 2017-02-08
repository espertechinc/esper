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

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class AggregationLocalGroupByColumn {
    private final boolean defaultGroupLevel;
    private final ExprEvaluator[] partitionEvaluators;
    private final int methodOffset;
    private final boolean methodAgg;
    private final AggregationAccessorSlotPair pair;
    private final int levelNum;

    public AggregationLocalGroupByColumn(boolean defaultGroupLevel, ExprEvaluator[] partitionEvaluators, int methodOffset, boolean methodAgg, AggregationAccessorSlotPair pair, int levelNum) {
        this.defaultGroupLevel = defaultGroupLevel;
        this.partitionEvaluators = partitionEvaluators;
        this.methodOffset = methodOffset;
        this.methodAgg = methodAgg;
        this.pair = pair;
        this.levelNum = levelNum;
    }

    public ExprEvaluator[] getPartitionEvaluators() {
        return partitionEvaluators;
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

    public AggregationAccessorSlotPair getPair() {
        return pair;
    }

    public int getLevelNum() {
        return levelNum;
    }
}
