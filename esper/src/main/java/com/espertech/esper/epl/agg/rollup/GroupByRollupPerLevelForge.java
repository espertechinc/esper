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

import com.espertech.esper.epl.core.orderby.OrderByElementForge;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprForge;

public class GroupByRollupPerLevelForge {
    private final SelectExprProcessorForge[] selectExprProcessorForges;
    private final ExprForge[] optionalHavingForges;
    private final OrderByElementForge[][] optionalOrderByElements;

    public GroupByRollupPerLevelForge(SelectExprProcessorForge[] selectExprProcessorForges, ExprForge[] optionalHavingForges, OrderByElementForge[][] optionalOrderByElements) {
        this.selectExprProcessorForges = selectExprProcessorForges;
        this.optionalHavingForges = optionalHavingForges;
        this.optionalOrderByElements = optionalOrderByElements;
    }

    public SelectExprProcessorForge[] getSelectExprProcessorForges() {
        return selectExprProcessorForges;
    }

    public ExprForge[] getOptionalHavingForges() {
        return optionalHavingForges;
    }

    public OrderByElementForge[][] getOptionalOrderByElements() {
        return optionalOrderByElements;
    }
}
