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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.map.MapEventPropertyGetter;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class ObjectArrayNestedEntryPropertyGetterArrayMap extends ObjectArrayNestedEntryPropertyGetterBase {

    private final int index;
    private final MapEventPropertyGetter getter;

    public ObjectArrayNestedEntryPropertyGetterArrayMap(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService, int index, MapEventPropertyGetter getter) {
        super(propertyIndex, fragmentType, eventAdapterService);
        this.index = index;
        this.getter = getter;
    }

    public Object handleNestedValue(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMap(value, index, getter);
    }

    public Object handleNestedValueFragment(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMapFragment(value, index, getter, eventAdapterService, fragmentType);
    }

    public boolean handleNestedValueExists(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMapExists(value, index, getter);
    }
}
