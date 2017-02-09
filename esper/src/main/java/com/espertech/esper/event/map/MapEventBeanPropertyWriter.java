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
import com.espertech.esper.event.EventPropertyWriter;
import com.espertech.esper.event.MappedEventBean;

import java.util.Map;

public class MapEventBeanPropertyWriter implements EventPropertyWriter {

    protected final String propertyName;

    public MapEventBeanPropertyWriter(String propertyName) {
        this.propertyName = propertyName;
    }

    public void write(Object value, EventBean target) {
        MappedEventBean map = (MappedEventBean) target;
        write(value, map.getProperties());
    }

    public void write(Object value, Map<String, Object> map) {
        map.put(propertyName, value);
    }
}
