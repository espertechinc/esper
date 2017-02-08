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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanManufacturer;
import net.sf.cglib.reflect.FastConstructor;

public class EventBeanManufacturerCtor implements EventBeanManufacturer {

    private final FastConstructor fastConstructor;
    private final BeanEventType beanEventType;
    private final EventAdapterService eventAdapterService;

    public EventBeanManufacturerCtor(FastConstructor fastConstructor, BeanEventType beanEventType, EventAdapterService eventAdapterService) {
        this.fastConstructor = fastConstructor;
        this.beanEventType = beanEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean make(Object[] properties) {
        Object instance = makeUnderlying(properties);
        return eventAdapterService.adapterForTypedBean(instance, beanEventType);
    }

    public Object makeUnderlying(Object[] properties) {
        return InstanceManufacturerFastCtor.makeUnderlyingFromFastCtor(properties, fastConstructor, beanEventType.getUnderlyingType());
    }
}
