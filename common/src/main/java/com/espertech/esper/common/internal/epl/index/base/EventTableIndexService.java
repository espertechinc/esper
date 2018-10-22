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
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;

public interface EventTableIndexService {
    boolean allowInitIndex(boolean isRecoveringResilient);

    EventTableFactory createHashedOnly(int indexedStreamNum, EventType eventType, String[] indexProps, Class[] indexTypes, boolean unique, String optionalIndexName, EventPropertyValueGetter getter, Object optionalSerde, boolean isFireAndForget, StatementContext statementContext);

    EventTableFactory createUnindexed(int indexedStreamNum, EventType eventType, Object optionalSerde, boolean isFireAndForget, StatementContext statementContext);

    EventTableFactory createSorted(int indexedStreamNum, EventType eventType, String indexedProp, Class indexType, EventPropertyValueGetter getter, Object optionalSerde, boolean isFireAndForget, StatementContext statementContext);

    EventTableFactory createComposite(int indexedStreamNum,
                                      EventType eventType, String[] indexProps, Class[] indexCoercionTypes, EventPropertyValueGetter indexGetter,
                                      String[] rangeProps, Class[] rangeCoercionTypes, EventPropertyValueGetter[] rangeGetters,
                                      Object optionalSerde, boolean isFireAndForget);

    EventTableFactory createInArray(int streamNum, EventType eventType, String[] propertyNames, Class[] indexTypes, boolean unique, EventPropertyValueGetter[] getters, boolean isFireAndForget, StatementContext statementContext);

    EventTableFactory createCustom(String optionalIndexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc);
}
