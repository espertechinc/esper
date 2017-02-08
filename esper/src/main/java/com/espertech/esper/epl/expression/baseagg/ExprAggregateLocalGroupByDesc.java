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

public class ExprAggregateLocalGroupByDesc {
    private ExprNode[] partitionExpressions;

    public ExprAggregateLocalGroupByDesc(ExprNode[] partitionExpressions) {
        this.partitionExpressions = partitionExpressions;
    }

    public ExprNode[] getPartitionExpressions() {
        return partitionExpressions;
    }
}
