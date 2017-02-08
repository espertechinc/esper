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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.AvroBackedBean;
import org.apache.avro.generic.GenericData;

public class AvroGenericDataEventBean implements EventBean, AvroGenericDataBackedEventBean, AvroBackedBean {
    private GenericData.Record record;
    private final EventType eventType;

    public AvroGenericDataEventBean(GenericData.Record record, EventType eventType) {
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

    public Object getGenericRecordDotData() {
        return record;
    }

    public void setGenericRecordDotData(Object genericRecordDotData) {
        this.record = (GenericData.Record) genericRecordDotData;
    }
}
