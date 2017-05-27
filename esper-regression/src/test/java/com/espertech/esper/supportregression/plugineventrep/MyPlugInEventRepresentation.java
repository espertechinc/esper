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

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.plugin.*;

import java.util.*;

public class MyPlugInEventRepresentation implements PlugInEventRepresentation {
    // Properties shared by all event types, for testing
    private Set<String> baseProps;

    // Since this implementation also tests dynamic event reflection, keep a list of event types
    private List<MyPlugInPropertiesEventType> types;

    public void init(PlugInEventRepresentationContext context) {
        // Load a fixed set of properties from a String initializer, in comma-separated list.
        // Each type we generate will have this base set of properties.
        String initialValues = (String) context.getRepresentationInitializer();
        String[] propertyList = (initialValues != null) ? initialValues.split(",") : new String[0];
        baseProps = new HashSet<String>(Arrays.asList(propertyList));

        types = new ArrayList<MyPlugInPropertiesEventType>();
    }

    public boolean acceptsType(PlugInEventTypeHandlerContext context) {
        return true;
    }

    public PlugInEventTypeHandler getTypeHandler(PlugInEventTypeHandlerContext eventTypeContext) {
        String typeProperyies = (String) eventTypeContext.getTypeInitializer();
        String[] propertyList = (typeProperyies != null) ? typeProperyies.split(",") : new String[0];

        // the set of properties know are the set of this name as well as the set for the base
        Set<String> typeProps = new HashSet<String>(Arrays.asList(propertyList));
        typeProps.addAll(baseProps);

        Map<String, EventPropertyDescriptor> metadata = new LinkedHashMap<String, EventPropertyDescriptor>();
        for (String prop : typeProps) {
            metadata.put(prop, new EventPropertyDescriptor(prop, String.class, null, false, false, false, false, false));
        }

        // save type for testing dynamic event object reflection
        MyPlugInPropertiesEventType eventType = new MyPlugInPropertiesEventType(null, eventTypeContext.getEventTypeId(), typeProps, metadata);

        types.add(eventType);

        return new MyPlugInPropertiesEventTypeHandler(eventType);
    }

    public boolean acceptsEventBeanResolution(PlugInEventBeanReflectorContext eventBeanContext) {
        return true;
    }

    public PlugInEventBeanFactory getEventBeanFactory(PlugInEventBeanReflectorContext eventBeanContext) {
        return new MyPlugInPropertiesBeanFactory(types);
    }
}
