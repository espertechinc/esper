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

import java.util.Map;

public class GroupByRollupEvalContext {

    private final Map<ExprNode, Integer> expressionToIndex;

    public GroupByRollupEvalContext(Map<ExprNode, Integer> expressionToIndex) {
        this.expressionToIndex = expressionToIndex;
    }

    public int getIndex(ExprNode node) {
        return expressionToIndex.get(node);
    }
}
