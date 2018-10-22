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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.lang.reflect.Constructor;

public class EventBeanManufacturerCtor implements EventBeanManufacturer {

    private final Constructor constructor;
    private final BeanEventType beanEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public EventBeanManufacturerCtor(Constructor constructor, EventType beanEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.constructor = constructor;
        this.beanEventType = (BeanEventType) beanEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public EventBean make(Object[] properties) {
        Object instance = makeUnderlying(properties);
        return eventBeanTypedEventFactory.adapterForTypedBean(instance, beanEventType);
    }

    public Object makeUnderlying(Object[] properties) {
        return InstanceManufacturerFastCtor.makeUnderlyingFromFastCtor(properties, constructor, beanEventType.getUnderlyingType());
    }
}
