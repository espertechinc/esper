/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

public class AvroEventBean implements EventBean, AvroBackedEventBean {
    private final GenericData.Record record;
    private final EventType eventType;

    public AvroEventBean(GenericData.Record record, EventType eventType) {
        this.record = record;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object getUnderlying() {
        return record;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        }
        return getter.get(this);
    }

    public Object getFragment(String propertyExpression) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }

    public GenericData.Record getProperties() {
        return record;
    }
}
