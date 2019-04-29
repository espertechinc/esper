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
package com.espertech.esper.common.internal.serde.runtime.event;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.serdeset.additional.DIOSerdeTreeMapEventsMayDeque;

public interface EventSerdeFactory {
    void verifyHADeployment(boolean targetHA) throws ExprValidationException;
    DataInputOutputSerde<EventBean> nullableEvent(EventType eventType);
    DataInputOutputSerde<EventBean> nullableEventArray(EventType eventType);
    DataInputOutputSerde<Object> nullableEventOrUnderlying(EventType eventType);
    DataInputOutputSerde<Object> nullableEventArrayOrUnderlying(EventType eventType);
    DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(DataInputOutputSerde[] criteriaSerdes, EventType eventType);
    DataInputOutputSerde<Object> objectArrayMayNullNull(DataInputOutputSerde[] serdes);
    DataInputOutputSerde<Object> listEvents(EventType eventType);
    DataInputOutputSerde linkedHashMapEventsAndInt(EventType eventType);
    DataInputOutputSerde refCountedSetAtomicInteger(EventType eventType);
    DataInputOutputSerde<EventBean> nullableEventMayCollate(EventType eventType);
}
