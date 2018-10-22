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

public class DataInputOutputSerdeProviderDefault implements DataInputOutputSerdeProvider {
    public final static DataInputOutputSerdeProviderDefault INSTANCE = new DataInputOutputSerdeProviderDefault();

    private DataInputOutputSerdeProviderDefault() {
    }

    public DataInputOutputSerdeWCollation valueNullable(Class type) {
        return null;
    }

    public DataInputOutputSerdeWCollation refCountedSet(Class type) {
        return null;
    }

    public DataInputOutputSerdeWCollation sortedRefCountedSet(Class type) {
        return null;
    }

    public DataInputOutputSerdeWCollation listValues(Class type) {
        return null;
    }

    public DataInputOutputSerdeWCollation listEvents(EventType eventType) {
        return null;
    }

    public DataInputOutputSerdeWCollation linkedHashMapEventsAndInt(EventType eventType) {
        return null;
    }

    public DataInputOutputSerdeWCollation eventNullable(EventType eventType) {
        return null;
    }

    public DataInputOutputSerdeWCollation objectArrayMayNullNull(Class[] types) {
        return null;
    }

    public DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(Class[] valueTypes, EventType eventType) {
        return null;
    }

    public DataInputOutputSerdeWCollation refCountedSetAtomicInteger(EventType eventType) {
        return null;
    }
}
