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
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.rettype.EPType;

public class TableMetadataColumnAggregation extends TableMetadataColumn {

    private final AggregationMethodFactory factory;
    private final int methodOffset;
    private final AggregationAccessorSlotPair accessAccessorSlotPair;
    private final EPType optionalEnumerationType;
    private final EventType optionalEventType;

    public TableMetadataColumnAggregation(String columnName, AggregationMethodFactory factory, int methodOffset, AggregationAccessorSlotPair accessAccessorSlotPair, EPType optionalEnumerationType, EventType optionalEventType) {
        super(columnName, false);
        this.factory = factory;
        this.methodOffset = methodOffset;
        this.accessAccessorSlotPair = accessAccessorSlotPair;
        this.optionalEnumerationType = optionalEnumerationType;
        this.optionalEventType = optionalEventType;
    }

    public AggregationMethodFactory getFactory() {
        return factory;
    }

    public int getMethodOffset() {
        return methodOffset;
    }

    public AggregationAccessorSlotPair getAccessAccessorSlotPair() {
        return accessAccessorSlotPair;
    }

    public EPType getOptionalEnumerationType() {
        return optionalEnumerationType;
    }

    public EventType getOptionalEventType() {
        return optionalEventType;
    }
}
