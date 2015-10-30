/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableFactory;

public interface EventTableIndexService {
    boolean allowInitIndex(boolean isRecoveringResilient);
    EventTableFactory createSingleCoerceAll(int indexedStreamNum, EventType eventType, String indexProp, Class indexCoercionType);
    EventTableFactory createSingleCoerceAdd(int indexedStreamNum, EventType eventType, String indexProp, Class indexCoercionType);
    EventTableFactory createSingle(int indexedStreamNum, EventType eventType, String indexedPropertyName, boolean unique, String optionalIndexName);
    EventTableFactory createUnindexed(int indexedStreamNum);
}
