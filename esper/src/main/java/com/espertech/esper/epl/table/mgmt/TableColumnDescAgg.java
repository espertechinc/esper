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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;

public class TableColumnDescAgg extends TableColumnDesc {
    private final ExprAggregateNode aggregation;
    private final EventType optionalAssociatedType;

    public TableColumnDescAgg(int positionInDeclaration, String columnName, ExprAggregateNode aggregation, EventType optionalAssociatedType) {
        super(positionInDeclaration, columnName);
        this.aggregation = aggregation;
        this.optionalAssociatedType = optionalAssociatedType;
    }

    public ExprAggregateNode getAggregation() {
        return aggregation;
    }

    public EventType getOptionalAssociatedType() {
        return optionalAssociatedType;
    }
}
