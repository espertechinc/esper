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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;

public class LinearAggregationFactoryDesc {

    private final AggregationMethodFactory factory;
    private final EventType enumerationEventType;
    private final Class scalarCollectionType;

    public LinearAggregationFactoryDesc(AggregationMethodFactory factory, EventType enumerationEventType, Class scalarCollectionType) {
        this.factory = factory;
        this.enumerationEventType = enumerationEventType;
        this.scalarCollectionType = scalarCollectionType;
    }

    public AggregationMethodFactory getFactory() {
        return factory;
    }

    public EventType getEnumerationEventType() {
        return enumerationEventType;
    }

    public Class getScalarCollectionType() {
        return scalarCollectionType;
    }
}
