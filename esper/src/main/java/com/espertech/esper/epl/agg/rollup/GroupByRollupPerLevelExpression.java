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

import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class GroupByRollupPerLevelExpression {
    private final SelectExprProcessor[] selectExprProcessor;
    private final ExprEvaluator[] optionalHavingNodes;

    public GroupByRollupPerLevelExpression(SelectExprProcessor[] selectExprProcessor, ExprEvaluator[] optionalHavingNodes) {
        this.selectExprProcessor = selectExprProcessor;
        this.optionalHavingNodes = optionalHavingNodes;
    }

    public SelectExprProcessor[] getSelectExprProcessor() {
        return selectExprProcessor;
    }

    public ExprEvaluator[] getOptionalHavingNodes() {
        return optionalHavingNodes;
    }
}
