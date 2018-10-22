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
package com.espertech.esper.common.internal.epl.expression.agg.base;

import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

/**
 * Base expression node that represents an aggregation function such as 'sum' or 'count'.
 */
public interface ExprAggregateNode extends ExprForge, ExprNode {
    public AggregationForgeFactory getFactory();

    public boolean isDistinct();

    public ExprAggregateLocalGroupByDesc getOptionalLocalGroupBy();

    public void validatePositionals(ExprValidationContext validationContext) throws ExprValidationException;

    public ExprNode[] getPositionalParams();

    public ExprNode getOptionalFilter();

    public void setColumn(int column);

    public int getColumn();
}
