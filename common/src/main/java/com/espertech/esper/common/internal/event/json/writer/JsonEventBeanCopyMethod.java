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
package com.espertech.esper.common.internal.event.json.writer;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Copy method for Json-underlying events.
 */
public class JsonEventBeanCopyMethod implements EventBeanCopyMethod {
    private final JsonEventType eventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final int[] regularFieldsCopy;
    private final int[] mapIndexesToCopy;
    private final int[] arrayIndexesToCopy;

    public JsonEventBeanCopyMethod(JsonEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, int[] regularFieldsCopy, int[] mapIndexesToCopy, int[] arrayIndexesToCopy) {
        this.eventType = eventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.regularFieldsCopy = regularFieldsCopy;
        this.mapIndexesToCopy = mapIndexesToCopy;
        this.arrayIndexesToCopy = arrayIndexesToCopy;
    }

    public EventBean copy(EventBean theEvent) {
        JsonEventObjectBase source = (JsonEventObjectBase) theEvent.getUnderlying();
        JsonEventObjectBase copy;
        try {
            copy = (JsonEventObjectBase) eventType.getUnderlyingType().newInstance();
        } catch (Exception ex) {
            throw new EPException("Failed to instantiate Json event: " + ex.getMessage(), ex);
        }

        for (int index : regularFieldsCopy) {
            copy.setNativeValue(index, source.getNativeValue(index));
        }

        for (int index : mapIndexesToCopy) {
            Map<String, Object> innerMap = (Map<String, Object>) source.getNativeValue(index);
            if (innerMap != null) {
                Map<String, Object> copyMap = new HashMap<>(innerMap);
                copy.setNativeValue(index, copyMap);
            }
        }

        for (int index : arrayIndexesToCopy) {
            Object array = source.getNativeValue(index);
            if (array != null && array.getClass().isArray() && Array.getLength(array) != 0) {
                Object copied = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
                System.arraycopy(array, 0, copied, 0, Array.getLength(array));
                copy.setNativeValue(index, copied);
            }
        }

        if (source.getJsonValues().isEmpty()) {
            for (Map.Entry<String, Object> entry : source.getJsonValues().entrySet()) {
                copy.addJsonValue(entry.getKey(), entry.getValue());
            }
        }

        return eventBeanTypedEventFactory.adapterForTypedJson(copy, eventType);
    }
}
