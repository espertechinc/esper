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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;

import java.util.HashMap;
import java.util.Map;

public class WrapperEventTypeUtil {
    /**
     * Make a wrapper type.
     * A wrapper event type may indeed wrap another wrapper event type. This is so that a wrapper event bean can wrap another wrapper event bean.
     * If there were unwrapped the nesting information such as what is the nested wrapper type and how they are nested would be lost.
     *
     * @param metadata                   metadata
     * @param underlyingEventType        underlying event type
     * @param propertyTypesMayPrimitive  property types
     * @param eventBeanTypedEventFactory factory for instances
     * @param beanEventTypeFactory       bean event type factory
     * @param eventTypeNameResolver      type name resolver
     * @return wrapper type
     */
    public static WrapperEventType makeWrapper(EventTypeMetadata metadata, EventType underlyingEventType, Map<String, Object> propertyTypesMayPrimitive, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, EventTypeNameResolver eventTypeNameResolver) {
        // If we are wrapping an underlying type that is itself a wrapper, then this is a special case
        if (underlyingEventType instanceof WrapperEventType) {
            WrapperEventType underlyingWrapperType = (WrapperEventType) underlyingEventType;

            // the underlying type becomes the type already wrapped
            // properties are a superset of the wrapped properties and the additional properties
            underlyingEventType = underlyingWrapperType.getUnderlyingEventType();
            Map<String, Object> propertiesSuperset = new HashMap<String, Object>();
            propertiesSuperset.putAll(underlyingWrapperType.getUnderlyingMapType().getTypes());
            propertiesSuperset.putAll(propertyTypesMayPrimitive);
            propertyTypesMayPrimitive = propertiesSuperset;
        }

        Map<String, Object> verified = BaseNestableEventUtil.resolvePropertyTypes(propertyTypesMayPrimitive, eventTypeNameResolver);
        return new WrapperEventType(metadata, underlyingEventType, verified, eventBeanTypedEventFactory, beanEventTypeFactory);
    }
}
