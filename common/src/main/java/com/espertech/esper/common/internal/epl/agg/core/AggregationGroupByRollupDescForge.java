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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newArrayWithInit;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationGroupByRollupDescForge {
    private final AggregationGroupByRollupLevelForge[] levels;
    private final int numLevelsAggregation;

    public AggregationGroupByRollupDescForge(AggregationGroupByRollupLevelForge[] levels) {
        this.levels = levels;

        int count = 0;
        for (AggregationGroupByRollupLevelForge level : levels) {
            if (!level.isAggregationTop()) {
                count++;
            }
        }
        numLevelsAggregation = count;
    }

    public AggregationGroupByRollupLevelForge[] getLevels() {
        return levels;
    }

    public int getNumLevelsAggregation() {
        return numLevelsAggregation;
    }

    public int getNumLevels() {
        return levels.length;
    }

    public CodegenExpression codegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenExpression[] level = new CodegenExpression[levels.length];
        for (int i = 0; i < levels.length; i++) {
            level[i] = levels[i].codegen(parent, classScope);
        }
        return newInstance(AggregationGroupByRollupDesc.class, newArrayWithInit(AggregationGroupByRollupLevel.class, level));
    }
}
