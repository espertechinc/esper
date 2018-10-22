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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newArrayWithInit;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationGroupByRollupDesc {
    private final AggregationGroupByRollupLevel[] levels;
    private final int numLevelsAggregation;

    public AggregationGroupByRollupDesc(AggregationGroupByRollupLevel[] levels) {
        this.levels = levels;

        int count = 0;
        for (AggregationGroupByRollupLevel level : levels) {
            if (!level.isAggregationTop()) {
                count++;
            }
        }
        numLevelsAggregation = count;
    }

    public static AggregationGroupByRollupDesc make(int[][] indexes) {
        List<AggregationGroupByRollupLevel> levels = new ArrayList<AggregationGroupByRollupLevel>();
        int countOffset = 0;
        int countNumber = -1;
        for (int[] mki : indexes) {
            countNumber++;
            if (mki.length == 0) {
                levels.add(new AggregationGroupByRollupLevel(countNumber, -1, null));
            } else {
                levels.add(new AggregationGroupByRollupLevel(countNumber, countOffset, mki));
                countOffset++;
            }
        }
        AggregationGroupByRollupLevel[] levelsarr = levels.toArray(new AggregationGroupByRollupLevel[levels.size()]);
        return new AggregationGroupByRollupDesc(levelsarr);
    }

    public AggregationGroupByRollupLevel[] getLevels() {
        return levels;
    }

    public int getNumLevelsAggregation() {
        return numLevelsAggregation;
    }

    public int getNumLevels() {
        return levels.length;
    }

    public CodegenExpression codegen() {
        CodegenExpression[] level = new CodegenExpression[levels.length];
        for (int i = 0; i < levels.length; i++) {
            level[i] = levels[i].toExpression();
        }
        return newInstance(AggregationGroupByRollupDesc.class, newArrayWithInit(AggregationGroupByRollupLevel.class, level));
    }
}
