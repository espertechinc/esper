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
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOUnsupportedSerde;

public class EventSerdeFactoryDefault implements EventSerdeFactory {

    public final static EventSerdeFactoryDefault INSTANCE = new EventSerdeFactoryDefault();

    private EventSerdeFactoryDefault() {
    }

    public void verifyHADeployment(boolean targetHA) throws ExprValidationException {
        // no verification required
    }

    public DataInputOutputSerde<EventBean> nullableEvent(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde<EventBean> nullableEventArray(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde<Object> listEvents(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde linkedHashMapEventsAndInt(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde refCountedSetAtomicInteger(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde<EventBean> nullableEventMayCollate(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde<Object> nullableEventOrUnderlying(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(DataInputOutputSerde[] criteriaSerdes, EventType eventType) {
        return null;
    }

    public DataInputOutputSerde<Object> objectArrayMayNullNull(DataInputOutputSerde[] serdes) {
        return DIOUnsupportedSerde.INSTANCE;
    }

    public DataInputOutputSerde<Object> nullableEventArrayOrUnderlying(EventType eventType) {
        return DIOUnsupportedSerde.INSTANCE;
    }
}
