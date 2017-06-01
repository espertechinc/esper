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

import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.util.Map;

/**
 * Property getter for Map-underlying events.
 */
public interface MapEventPropertyGetter extends EventPropertyGetterSPI {
    /**
     * Returns a property of an event.
     *
     * @param map to interrogate
     * @return property value
     * @throws PropertyAccessException for property access errors
     */
    public Object getMap(Map<String, Object> map) throws PropertyAccessException;

    /**
     * Exists-function for properties in a map-type event.
     *
     * @param map to interrogate
     * @return indicator
     */
    public boolean isMapExistsProperty(Map<String, Object> map);
}