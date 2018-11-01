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

public interface DataInputOutputSerdeProvider {
    DataInputOutputSerde valueNullable(Class type);

    DataInputOutputSerde refCountedSet(Class type);

    DataInputOutputSerde sortedRefCountedSet(Class type);

    DataInputOutputSerde objectArrayMayNullNull(Class[] types);

    DataInputOutputSerde listEvents(EventType eventType);

    DataInputOutputSerde linkedHashMapEventsAndInt(EventType eventType);

    DataInputOutputSerde eventNullable(EventType eventType);

    DataInputOutputSerde refCountedSetAtomicInteger(EventType eventType);

    DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(Class[] valueTypes, EventType eventType);
}
