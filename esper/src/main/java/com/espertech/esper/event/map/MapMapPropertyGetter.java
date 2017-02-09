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

import java.util.Map;

/**
 * A getter that interrogates a given property in a map which may itself contain nested maps or indexed entries.
 */
public class MapMapPropertyGetter implements MapEventPropertyGetter {
    private final String propertyMap;
    private final MapEventPropertyGetter getter;

    /**
     * Ctor.
     *
     * @param propertyMap is the property returning the map to interrogate
     * @param getter      is the getter to use to interrogate the property in the map
     */
    public MapMapPropertyGetter(String propertyMap, MapEventPropertyGetter getter) {
        if (getter == null) {
            throw new IllegalArgumentException("Getter is a required parameter");
        }
        this.propertyMap = propertyMap;
        this.getter = getter;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object valueTopObj = map.get(propertyMap);
        if (!(valueTopObj instanceof Map)) {
            return null;
        }
        return getter.getMap((Map) valueTopObj);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        Object valueTopObj = map.get(propertyMap);
        if (!(valueTopObj instanceof Map)) {
            return false;
        }
        return getter.isMapExistsProperty((Map) valueTopObj);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isMapExistsProperty(BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean));
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }
}
