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
package com.espertech.esper.common.internal.serde;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;

public class DataInputOutputSerdeProviderDefault implements DataInputOutputSerdeProvider {
    public final static DataInputOutputSerdeProviderDefault INSTANCE = new DataInputOutputSerdeProviderDefault();

    private DataInputOutputSerdeProviderDefault() {
    }

    public DataInputOutputSerde valueNullable(Class type) {
        return null;
    }

    public DataInputOutputSerde refCountedSet(Class type) {
        return null;
    }

    public DataInputOutputSerde sortedRefCountedSet(Class type) {
        return null;
    }

    public DataInputOutputSerde listValues(Class type) {
        return null;
    }

    public DataInputOutputSerde listEvents(EventType eventType) {
        return null;
    }

    public DataInputOutputSerde linkedHashMapEventsAndInt(EventType eventType) {
        return null;
    }

    public DataInputOutputSerde eventNullable(EventType eventType) {
        return null;
    }

    public DataInputOutputSerde objectArrayMayNullNull(Class[] types) {
        return null;
    }

    public DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(Class[] valueTypes, EventType eventType) {
        return null;
    }

    public DataInputOutputSerde refCountedSetAtomicInteger(EventType eventType) {
        return null;
    }
}
