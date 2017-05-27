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
import com.espertech.esper.plugin.PlugInEventBeanFactory;

import java.net.URI;
import java.util.List;
import java.util.Properties;

public class MyPlugInPropertiesBeanFactory implements PlugInEventBeanFactory {
    private final List<MyPlugInPropertiesEventType> knownTypes;

    public MyPlugInPropertiesBeanFactory(List<MyPlugInPropertiesEventType> types) {
        knownTypes = types;
    }

    public EventBean create(Object theEvent, URI resolutionURI) {
        Properties properties = (Properties) theEvent;

        // use the known types to determine the type of the object
        for (MyPlugInPropertiesEventType type : knownTypes) {
            // if there is one property the event does not contain, then its not the right type
            boolean hasAllProperties = true;
            for (String prop : type.getPropertyNames()) {
                if (!properties.containsKey(prop)) {
                    hasAllProperties = false;
                    break;
                }
            }

            if (hasAllProperties) {
                return new MyPlugInPropertiesEventBean(type, properties);
            }
        }

        return null; // none match, unknown event
    }
}
