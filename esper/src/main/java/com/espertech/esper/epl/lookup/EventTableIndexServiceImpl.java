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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.join.table.*;
import com.espertech.esper.util.CollectionUtil;

public class EventTableIndexServiceImpl implements EventTableIndexService {
    public boolean allowInitIndex() {
        return true;
    }

    public EventTableFactory createSingleCoerceAll(int indexedStreamNum, EventType eventType, String indexProp, Class indexCoercionType) {
        return new PropertyIndexedEventTableSingleCoerceAllFactory(indexedStreamNum, eventType, indexProp, indexCoercionType);
    }

    public EventTableFactory createSingle(int indexedStreamNum, EventType eventType, String propertyName, boolean unique, String optionalIndexName) {
        return new PropertyIndexedEventTableSingleFactory(indexedStreamNum, eventType, propertyName, unique, optionalIndexName);
    }

    public EventTableFactory createUnindexed(int indexedStreamNum) {
        return new UnindexedEventTableFactory(indexedStreamNum);
    }
}
