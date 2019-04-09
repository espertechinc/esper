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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorSlotPairForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationLocalGroupByColumnForge {
    private final boolean defaultGroupLevel;
    private final ExprNode[] partitionForges;
    private final int methodOffset;
    private final boolean methodAgg;
    private final AggregationAccessorSlotPairForge pair;
    private final int levelNum;

    public AggregationLocalGroupByColumnForge(boolean defaultGroupLevel, ExprNode[] partitionForges, int methodOffset, boolean methodAgg, AggregationAccessorSlotPairForge pair, int levelNum) {
        this.defaultGroupLevel = defaultGroupLevel;
        this.partitionForges = partitionForges;
        this.methodOffset = methodOffset;
        this.methodAgg = methodAgg;
        this.pair = pair;
        this.levelNum = levelNum;
    }

    public ExprNode[] getPartitionForges() {
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

    public CodegenExpression toExpression(int fieldNum) {
        return newInstance(AggregationLocalGroupByColumn.class, constant(defaultGroupLevel), constant(fieldNum), constant(levelNum));
    }
}
