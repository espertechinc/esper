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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.index.service.EventAdvancedIndexProvisionDesc;
import com.espertech.esper.epl.join.table.EventTableFactory;

public interface EventTableIndexService {
    boolean allowInitIndex(boolean isRecoveringResilient);

    EventTableFactory createUnindexed(int indexedStreamNum, Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createSingle(int indexedStreamNum, EventType eventType, String indexProp, boolean unique, String optionalIndexName, Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createSingleCoerceAdd(int indexedStreamNum, EventType eventType, String indexProp, Class indexCoercionType, Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createSingleCoerceAll(int indexedStreamNum, EventType eventType, String indexProp, Class indexCoercionType, Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createMultiKey(int indexedStreamNum, EventType eventType, String[] indexProps, boolean unique, String optionalIndexName, Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createMultiKeyCoerceAdd(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexCoercionTypes, boolean isFireAndForget);

    EventTableFactory createMultiKeyCoerceAll(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexCoercionTypes, boolean isFireAndForget);

    EventTableFactory createComposite(int indexedStreamNum, EventType eventType, String[] indexedKeyProps, Class[] coercionKeyTypes, String[] indexedRangeProps, Class[] coercionRangeTypes, boolean isFireAndForget);

    EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, boolean isFireAndForget);

    EventTableFactory createSortedCoerce(int indexedStreamNum, EventType eventType, String indexedProp, Class indexCoercionType, boolean isFireAndForget);

    EventTableFactory createInArray(int indexedStreamNum, EventType eventType, String[] indexedProp, boolean unique);

    EventTableFactory createCustom(String indexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc);
}
