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

import com.espertech.esper.epl.expression.core.ExprNode;

public class ExprAggregateNodeParamDesc {
    private final ExprNode[] positionalParams;
    private final ExprAggregateLocalGroupByDesc optLocalGroupBy;
    private final ExprNode optionalFilter;

    public ExprAggregateNodeParamDesc(ExprNode[] positionalParams, ExprAggregateLocalGroupByDesc optLocalGroupBy, ExprNode optionalFilter) {
        this.positionalParams = positionalParams;
        this.optLocalGroupBy = optLocalGroupBy;
        this.optionalFilter = optionalFilter;
    }

    public ExprNode[] getPositionalParams() {
        return positionalParams;
    }

    public ExprAggregateLocalGroupByDesc getOptLocalGroupBy() {
        return optLocalGroupBy;
    }

    public ExprNode getOptionalFilter() {
        return optionalFilter;
    }
}
