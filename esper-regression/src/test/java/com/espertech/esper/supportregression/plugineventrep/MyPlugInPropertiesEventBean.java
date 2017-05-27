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
package com.espertech.esper.supportregression.plugineventrep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;

import java.util.Properties;

public class MyPlugInPropertiesEventBean implements EventBean {
    private final MyPlugInPropertiesEventType eventType;
    private final Properties properties;

    public MyPlugInPropertiesEventBean(MyPlugInPropertiesEventType eventType, Properties properties) {
        this.eventType = eventType;
        this.properties = properties;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter != null) {
            return getter.get(this);
        }
        return null;
    }

    public Object getUnderlying() {
        return properties;
    }

    protected Properties getProperties() {
        return properties;
    }

    public Object getFragment(String property) {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter != null) {
            return getter.getFragment(this);
        }
        return null;
    }
}
