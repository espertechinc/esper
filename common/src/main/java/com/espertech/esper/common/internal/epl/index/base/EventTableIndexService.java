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
package com.espertech.esper.common.internal.epl.index.base;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public interface EventTableIndexService {
    boolean allowInitIndex(boolean isRecoveringResilient);

    EventTableFactory createHashedOnly(int indexedStreamNum, EventType eventType, String[] indexProps, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde keySerde, boolean unique, String optionalIndexName, EventPropertyValueGetter getter, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings);

    EventTableFactory createUnindexed(int indexedStreamNum, EventType eventType, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings);

    EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, EPTypeClass indexType, EventPropertyValueGetter getter, DataInputOutputSerde serde, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings);

    EventTableFactory createComposite(int indexedStreamNum,
                                      EventType eventType, String[] indexProps, EPTypeClass[] indexCoercionTypes, EventPropertyValueGetter indexGetter,
                                      MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde keySerde,
                                      String[] rangeProps, EPTypeClass[] rangeCoercionTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde[] rangeSerdes,
                                      DataInputOutputSerde optionalValueSerde, boolean isFireAndForget);

    EventTableFactory createInArray(int streamNum, EventType eventType, String[] propertyNames, EPTypeClass[] indexTypes, DataInputOutputSerde[] indexSerdes, boolean unique, EventPropertyValueGetter[] getters, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings);

    EventTableFactory createCustom(String optionalIndexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc);
}
