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

public interface EventTableIndexService {
    boolean allowInitIndex(boolean isRecoveringResilient);

    EventTableFactory createHashedOnly(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexTypes, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> keySerde, boolean unique, String optionalIndexName, EventPropertyValueGetter getter, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext);

    EventTableFactory createUnindexed(int indexedStreamNum, EventType eventType, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext);

    EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, Class indexType, EventPropertyValueGetter getter, DataInputOutputSerde<Object> serde, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext);

    EventTableFactory createComposite(int indexedStreamNum,
                                      EventType eventType, String[] indexProps, Class[] indexCoercionTypes, EventPropertyValueGetter indexGetter,
                                      MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> keySerde,
                                      String[] rangeProps, Class[] rangeCoercionTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde<Object>[] rangeSerdes,
                                      DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget);

    EventTableFactory createInArray(int streamNum, EventType eventType, String[] propertyNames, Class[] indexTypes, DataInputOutputSerde<Object>[] indexSerdes, boolean unique, EventPropertyValueGetter[] getters, boolean isFireAndForget, EventTableFactoryFactoryContext eventTableFactoryContext);

    EventTableFactory createCustom(String optionalIndexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc);
}
