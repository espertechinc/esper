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

public interface DataInputOutputSerdeProvider {
    DataInputOutputSerdeWCollation valueNullable(Class type);

    DataInputOutputSerdeWCollation refCountedSet(Class type);

    DataInputOutputSerdeWCollation sortedRefCountedSet(Class type);

    DataInputOutputSerdeWCollation objectArrayMayNullNull(Class[] types);

    DataInputOutputSerdeWCollation listEvents(EventType eventType);

    DataInputOutputSerdeWCollation linkedHashMapEventsAndInt(EventType eventType);

    DataInputOutputSerdeWCollation eventNullable(EventType eventType);

    DataInputOutputSerdeWCollation refCountedSetAtomicInteger(EventType eventType);

    DIOSerdeTreeMapEventsMayDeque treeMapEventsMayDeque(Class[] valueTypes, EventType eventType);
}
