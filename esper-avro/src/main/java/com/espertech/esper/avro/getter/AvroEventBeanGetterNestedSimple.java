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
package com.espertech.esper.avro.getter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.generic.GenericData;

public class AvroEventBeanGetterNestedSimple implements EventPropertyGetter {
    private final int posTop;
    private final int posInner;
    private final EventType fragmentType;
    private final EventAdapterService eventAdapterService;

    public AvroEventBeanGetterNestedSimple(int posTop, int posInner, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.posTop = posTop;
        this.posInner = posInner;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(posTop);
        if (inner == null) {
            return null;
        }
        return inner.get(posInner);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        if (fragmentType == null) {
            return null;
        }
        Object value = get(eventBean);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentType);
    }
}
