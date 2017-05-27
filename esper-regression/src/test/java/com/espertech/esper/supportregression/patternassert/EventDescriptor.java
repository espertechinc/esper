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
package com.espertech.esper.supportregression.patternassert;

import java.util.HashMap;
import java.util.Map;

public class EventDescriptor {
    private Map<String, Object> eventProperties;

    public EventDescriptor() {
        eventProperties = new HashMap<String, Object>();
    }

    public Map<String, Object> getEventProperties() {
        return eventProperties;
    }

    public void put(String propertyName, Object value) {
        eventProperties.put(propertyName, value);
    }
}
