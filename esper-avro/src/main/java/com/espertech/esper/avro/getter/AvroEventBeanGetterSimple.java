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

package com.espertech.esper.avro.getter;

import com.espertech.esper.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

public class AvroEventBeanGetterSimple implements AvroEventPropertyGetter
{
    private final int propertyIndex;
    private final EventType fragmentType;
    private final EventAdapterService eventAdapterService;

    public AvroEventBeanGetterSimple(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object getAvroFieldValue(GenericData.Record record) throws PropertyAccessException {
        return record.get(propertyIndex);
    }

    public Object get(EventBean theEvent) {
        return getAvroFieldValue((GenericData.Record) theEvent.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean obj) {
        Object value = get(obj);
        return getFragmentInternal(value);
    }

    public Object getAvroFragment(GenericData.Record record) {
        Object value = getAvroFieldValue(record);
        return getFragmentInternal(value);
    }

    private Object getFragmentInternal(Object value) {
        if (fragmentType == null) {
            return null;
        }
        if (value instanceof GenericData.Record) {
            return eventAdapterService.adapterForTypedAvro(value, fragmentType);
        }
        if (value instanceof Collection) {
            Collection coll = (Collection) value;
            EventBean[] events = new EventBean[coll.size()];
            int index = 0;
            for (Object item : coll) {
                events[index++] = eventAdapterService.adapterForTypedAvro(item, fragmentType);
            }
            return events;
        }
        return null;
    }
}

