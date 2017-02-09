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
package com.espertech.esper.epl.agg.rollup;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupByRollupNodeCombinedExpr extends GroupByRollupNodeBase {

    private final List<ExprNode> expressions;

    public GroupByRollupNodeCombinedExpr(List<ExprNode> expressions) {
        this.expressions = expressions;
    }

    public List<int[]> evaluate(GroupByRollupEvalContext context)
            throws GroupByRollupDuplicateException {
        int[] result = new int[expressions.size()];
        for (int i = 0; i < expressions.size(); i++) {
            int index = context.getIndex(expressions.get(i));
            result[i] = index;
        }

        Arrays.sort(result);

        // find dups
        for (int i = 0; i < result.length - 1; i++) {
            if (result[i] == result[i + 1]) {
                throw new GroupByRollupDuplicateException(new int[]{result[i]});
            }
        }

        return Collections.singletonList(result);
    }
}
