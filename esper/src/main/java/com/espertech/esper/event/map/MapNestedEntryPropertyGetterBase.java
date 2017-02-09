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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

public abstract class MapNestedEntryPropertyGetterBase implements MapEventPropertyGetter {

    protected final String propertyMap;
    protected final EventType fragmentType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyMap         the property to look at
     * @param eventAdapterService factory for event beans and event types
     * @param fragmentType        type of the entry returned
     */
    public MapNestedEntryPropertyGetterBase(String propertyMap, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyMap = propertyMap;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public abstract Object handleNestedValue(Object value);

    public abstract Object handleNestedValueFragment(Object value);

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValue(value);
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

    public Object getFragment(EventBean obj) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValueFragment(value);
    }
}
