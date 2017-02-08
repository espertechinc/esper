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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;

import java.util.LinkedHashMap;

public enum ExpressionViewOAFieldEnum {
    CURRENT_COUNT("current_count"),
    OLDEST_TIMESTAMP("oldest_timestamp"),
    NEWEST_TIMESTAMP("newest_timestamp"),
    EXPIRED_COUNT("expired_count"),
    VIEW_REFERENCE("view_reference"),
    NEWEST_EVENT("newest_event"),
    OLDEST_EVENT("oldest_event");

    private final String fieldName;

    private ExpressionViewOAFieldEnum(String fieldName) {
        this.fieldName = fieldName;
    }

    public static LinkedHashMap<String, Object> asMapOfTypes(EventType eventType) {
        LinkedHashMap<String, Object> builtinTypeDef = new LinkedHashMap<String, Object>();
        builtinTypeDef.put(ExpressionViewOAFieldEnum.CURRENT_COUNT.fieldName, Integer.class);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.OLDEST_TIMESTAMP.fieldName, Long.class);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.NEWEST_TIMESTAMP.fieldName, Long.class);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.EXPIRED_COUNT.fieldName, Integer.class);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.VIEW_REFERENCE.fieldName, Object.class);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.NEWEST_EVENT.fieldName, eventType);
        builtinTypeDef.put(ExpressionViewOAFieldEnum.OLDEST_EVENT.fieldName, eventType);
        return builtinTypeDef;
    }

    public static void populate(Object[] properties,
                                int windowSize,
                                long oldestEventTimestamp,
                                long newestEventTimestamp,
                                Object viewReference,
                                int expiredCount,
                                EventBean oldestEvent,
                                EventBean newestEvent) {
        properties[ExpressionViewOAFieldEnum.CURRENT_COUNT.ordinal()] = windowSize;
        properties[ExpressionViewOAFieldEnum.OLDEST_TIMESTAMP.ordinal()] = oldestEventTimestamp;
        properties[ExpressionViewOAFieldEnum.NEWEST_TIMESTAMP.ordinal()] = newestEventTimestamp;
        properties[ExpressionViewOAFieldEnum.VIEW_REFERENCE.ordinal()] = viewReference;
        properties[ExpressionViewOAFieldEnum.EXPIRED_COUNT.ordinal()] = expiredCount;
        properties[ExpressionViewOAFieldEnum.OLDEST_EVENT.ordinal()] = oldestEvent;
        properties[ExpressionViewOAFieldEnum.NEWEST_EVENT.ordinal()] = newestEvent;
    }

    public static Object[] getPrototypeOA() {
        return new Object[ExpressionViewOAFieldEnum.values().length];
    }
}
