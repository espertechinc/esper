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
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventTableFactoryCustomIndex;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTableFactory;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;
import com.espertech.esper.common.internal.epl.index.inkeyword.PropertyHashedArrayFactory;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTableFactory;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTableFactory;

public class EventTableIndexServiceImpl implements EventTableIndexService {

    public final static EventTableIndexServiceImpl INSTANCE = new EventTableIndexServiceImpl();

    public boolean allowInitIndex(boolean isRecoveringResilient) {
        return true;
    }

    public EventTableFactory createHashedOnly(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexTypes, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> keySerde, boolean unique, String optionalIndexName, EventPropertyValueGetter getter, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return new PropertyHashedEventTableFactory(indexedStreamNum, indexProps, unique, optionalIndexName, getter, transformFireAndForget);
    }

    public EventTableFactory createUnindexed(int indexedStreamNum, EventType eventType, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return new UnindexedEventTableFactory(indexedStreamNum);
    }

    public EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, Class indexType, EventPropertyValueGetter getter, DataInputOutputSerde<Object> serde, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return new PropertySortedEventTableFactory(indexedStreamNum, indexedProp, getter, indexType);
    }

    public EventTableFactory createComposite(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexCoercionTypes, EventPropertyValueGetter indexGetter, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> keySerde, String[] rangeProps, Class[] rangeCoercionTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde<Object>[] rangeSerdes, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget) {
        return new PropertyCompositeEventTableFactory(indexedStreamNum, indexProps, indexCoercionTypes, indexGetter, transformFireAndForget, rangeProps, rangeCoercionTypes, rangeGetters);
    }

    public EventTableFactory createInArray(int streamNum, EventType eventType, String[] propertyNames, Class[] indexTypes, DataInputOutputSerde<Object>[] indexSerdes, boolean unique, EventPropertyValueGetter[] getters, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext) {
        return new PropertyHashedArrayFactory(streamNum, propertyNames, unique, null, getters);
    }

    public EventTableFactory createCustom(String indexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc) {
        return new EventTableFactoryCustomIndex(indexName, indexedStreamNum, eventType, unique, advancedIndexProvisionDesc);
    }
}
