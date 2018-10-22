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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;

public class AggregationLinearFactoryDesc {

    private final AggregationForgeFactory factory;
    private final EventType enumerationEventType;
    private final Class scalarCollectionType;
    private final int streamNum;

    public AggregationLinearFactoryDesc(AggregationForgeFactory factory, EventType enumerationEventType, Class scalarCollectionType, int streamNum) {
        this.factory = factory;
        this.enumerationEventType = enumerationEventType;
        this.scalarCollectionType = scalarCollectionType;
        this.streamNum = streamNum;
    }

    public AggregationForgeFactory getFactory() {
        return factory;
    }

    public EventType getEnumerationEventType() {
        return enumerationEventType;
    }

    public Class getScalarCollectionType() {
        return scalarCollectionType;
    }

    public int getStreamNum() {
        return streamNum;
    }
}
