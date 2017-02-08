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

import com.espertech.esper.epl.core.OrderByElement;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class GroupByRollupPerLevelExpression {
    private final SelectExprProcessor[] selectExprProcessor;
    private final ExprEvaluator[] optionalHavingNodes;
    private final OrderByElement[][] optionalOrderByElements;

    public GroupByRollupPerLevelExpression(SelectExprProcessor[] selectExprProcessor, ExprEvaluator[] optionalHavingNodes, OrderByElement[][] optionalOrderByElements) {
        this.selectExprProcessor = selectExprProcessor;
        this.optionalHavingNodes = optionalHavingNodes;
        this.optionalOrderByElements = optionalOrderByElements;
    }

    public SelectExprProcessor[] getSelectExprProcessor() {
        return selectExprProcessor;
    }

    public ExprEvaluator[] getOptionalHavingNodes() {
        return optionalHavingNodes;
    }

    public OrderByElement[][] getOptionalOrderByElements() {
        return optionalOrderByElements;
    }
}
