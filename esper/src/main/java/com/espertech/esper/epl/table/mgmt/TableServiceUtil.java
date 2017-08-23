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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.lookup.IndexedPropDesc;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TableServiceUtil {

    public static String getTableNameFromEventType(EventType type) {
        if (!(type instanceof EventTypeSPI)) {
            return null;
        }
        EventTypeSPI spi = (EventTypeSPI) type;
        if (spi.getMetadata().getTypeClass() == EventTypeMetadata.TypeClass.TABLE) {
            return spi.getMetadata().getPrimaryName();
        }
        return null;
    }

    public static StreamTypeServiceImpl streamTypeFromTableColumn(TableMetadataColumnAggregation column, String engineURI) {
        if (column.getOptionalEventType() == null) {
            throw new IllegalArgumentException("Required event type not provided");
        }
        return new StreamTypeServiceImpl(column.getOptionalEventType(), column.getOptionalEventType().getName(), false, engineURI);
    }

    public static Pair<int[], IndexMultiKey> getIndexMultikeyForKeys(Map<String, TableMetadataColumn> items, ObjectArrayEventType eventType) {
        List<IndexedPropDesc> indexFields = new ArrayList<IndexedPropDesc>();
        List<Integer> keyIndexes = new ArrayList<Integer>();
        int count = 0;
        for (Map.Entry<String, TableMetadataColumn> entry : items.entrySet()) {
            if (entry.getValue().isKey()) {
                indexFields.add(new IndexedPropDesc(entry.getKey(), eventType.getPropertyType(entry.getKey())));
                keyIndexes.add(count + 1);
            }
            count++;
        }
        int[] keyColIndexes = CollectionUtil.intArray(keyIndexes);
        return new Pair<int[], IndexMultiKey>(keyColIndexes, new IndexMultiKey(true, indexFields, Collections.<IndexedPropDesc>emptyList(), null));
    }
}
