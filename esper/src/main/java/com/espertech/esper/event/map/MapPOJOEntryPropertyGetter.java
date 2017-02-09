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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BaseNativePropertyGetter;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;

import java.util.Map;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class MapPOJOEntryPropertyGetter extends BaseNativePropertyGetter implements MapEventPropertyGetter {
    private final String propertyMap;
    private final BeanEventPropertyGetter mapEntryGetter;

    /**
     * Ctor.
     *
     * @param propertyMap         the property to look at
     * @param mapEntryGetter      the getter for the map entry
     * @param eventAdapterService for producing wrappers to objects
     * @param returnType          type of the entry returned
     * @param nestedComponentType nested type
     */
    public MapPOJOEntryPropertyGetter(String propertyMap, BeanEventPropertyGetter mapEntryGetter, EventAdapterService eventAdapterService, Class returnType, Class nestedComponentType) {
        super(eventAdapterService, returnType, nestedComponentType);
        this.propertyMap = propertyMap;
        this.mapEntryGetter = mapEntryGetter;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = map.get(propertyMap);

        if (value == null) {
            return null;
        }

        // Object within the map
        if (value instanceof EventBean) {
            return mapEntryGetter.get((EventBean) value);
        }
        return mapEntryGetter.getBeanProp(value);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
