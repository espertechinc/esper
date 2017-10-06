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
package com.espertech.esper.epl.expression.baseagg;

import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationResultFuture;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;

/**
 * Base expression node that represents an aggregation function such as 'sum' or 'count'.
 */
public interface ExprAggregateNode extends ExprEvaluator, ExprForge, ExprNode {
    public AggregationMethodFactory getFactory();

    public void setAggregationResultFuture(AggregationResultFuture aggregationResultFuture, int column);

    public boolean isDistinct();

    public ExprAggregateLocalGroupByDesc getOptionalLocalGroupBy();

    public void validatePositionals() throws ExprValidationException;

    public ExprNode[] getPositionalParams();

    public ExprNode getOptionalFilter();
}
