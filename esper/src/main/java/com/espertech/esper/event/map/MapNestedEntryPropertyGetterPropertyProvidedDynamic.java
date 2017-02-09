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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

public class MapNestedEntryPropertyGetterPropertyProvidedDynamic extends MapNestedEntryPropertyGetterBase {

    private final EventPropertyGetter nestedGetter;

    public MapNestedEntryPropertyGetterPropertyProvidedDynamic(String propertyMap, EventType fragmentType, EventAdapterService eventAdapterService, EventPropertyGetter nestedGetter) {
        super(propertyMap, fragmentType, eventAdapterService);
        this.nestedGetter = nestedGetter;
    }

    public Object handleNestedValue(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return ((MapEventPropertyGetter) nestedGetter).getMap((Map<String, Object>) value);
        }
        return null;
    }

    @Override
    public boolean isExistsProperty(EventBean eventBean) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        Object value = map.get(propertyMap);
        if (value == null || !(value instanceof Map)) {
            return false;
        }
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return ((MapEventPropertyGetter) nestedGetter).isMapExistsProperty((Map) value);
        }
        return false;
    }

    public Object handleNestedValueFragment(Object value) {
        return null;
    }
}
