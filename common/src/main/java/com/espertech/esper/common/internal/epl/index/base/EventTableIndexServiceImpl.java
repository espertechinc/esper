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
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventTableFactoryCustomIndex;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTableFactory;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;
import com.espertech.esper.common.internal.epl.index.inkeyword.PropertyHashedArrayFactory;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTableFactory;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTableFactory;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public class EventTableIndexServiceImpl implements EventTableIndexService {

    public final static EventTableIndexServiceImpl INSTANCE = new EventTableIndexServiceImpl();

    public boolean allowInitIndex(boolean isRecoveringResilient) {
        return true;
    }

    public EventTableFactory createHashedOnly(int indexedStreamNum, EventType eventType, String[] indexProps, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde keySerde, boolean unique, String optionalIndexName, EventPropertyValueGetter getter, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        return new PropertyHashedEventTableFactory(indexedStreamNum, indexProps, unique, optionalIndexName, getter, transformFireAndForget);
    }

    public EventTableFactory createUnindexed(int indexedStreamNum, EventType eventType, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        return new UnindexedEventTableFactory(indexedStreamNum);
    }

    public EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, EPTypeClass indexType, EventPropertyValueGetter getter, DataInputOutputSerde serde, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        return new PropertySortedEventTableFactory(indexedStreamNum, indexedProp, getter, indexType);
    }

    public EventTableFactory createComposite(int indexedStreamNum, EventType eventType, String[] indexProps, EPTypeClass[] indexCoercionTypes, EventPropertyValueGetter indexGetter, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde keySerde, String[] rangeProps, EPTypeClass[] rangeCoercionTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde[] rangeSerdes, DataInputOutputSerde optionalValueSerde, boolean isFireAndForget) {
        return new PropertyCompositeEventTableFactory(indexedStreamNum, indexProps, indexCoercionTypes, indexGetter, transformFireAndForget, rangeProps, rangeCoercionTypes, rangeGetters);
    }

    public EventTableFactory createInArray(int streamNum, EventType eventType, String[] propertyNames, EPTypeClass[] indexTypes, DataInputOutputSerde[] indexSerdes, boolean unique, EventPropertyValueGetter[] getters, boolean isFireAndForget, StateMgmtSetting stateMgmtSettings) {
        return new PropertyHashedArrayFactory(streamNum, propertyNames, unique, null, getters);
    }

    public EventTableFactory createCustom(String indexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc) {
        return new EventTableFactoryCustomIndex(indexName, indexedStreamNum, eventType, unique, advancedIndexProvisionDesc);
    }
}
