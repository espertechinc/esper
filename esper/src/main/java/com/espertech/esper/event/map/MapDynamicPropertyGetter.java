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

import java.util.Map;

public class MapDynamicPropertyGetter implements MapEventPropertyGetter {

    private final String propertyName;

    public MapDynamicPropertyGetter(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return map.get(propertyName);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return map.containsKey(propertyName);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Map map = (Map) eventBean.getUnderlying();
        return map.get(propertyName);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Map map = (Map) eventBean.getUnderlying();
        return map.containsKey(propertyName);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

}
