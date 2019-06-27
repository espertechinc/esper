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
package com.espertech.esper.common.internal.event.bean.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.*;

public class BeanEventTypeFactoryPrivate implements BeanEventTypeFactory {
    private final EventBeanTypedEventFactory typedEventFactory;
    private final EventTypeFactory eventTypeFactory;
    private final BeanEventTypeStemService stemFactory;

    private final Map<Class, BeanEventType> types = new HashMap<>();

    public BeanEventTypeFactoryPrivate(EventBeanTypedEventFactory typedEventFactory, EventTypeFactory eventTypeFactory, BeanEventTypeStemService stemFactory) {
        this.typedEventFactory = typedEventFactory;
        this.eventTypeFactory = eventTypeFactory;
        this.stemFactory = stemFactory;
    }

    public BeanEventType getCreateBeanType(Class clazz, boolean publicFields) {
        BeanEventType existing = types.get(clazz);
        if (existing != null) {
            return existing;
        }

        // check-allocate bean-stem
        ConfigurationCommonEventTypeBean config = null;
        if (publicFields) {
            config = new ConfigurationCommonEventTypeBean();
            config.setAccessorStyle(AccessorStyle.PUBLIC);
        }

        BeanEventTypeStem stem = stemFactory.getCreateStem(clazz, config);

        // metadata
        EventTypeMetadata metadata = new EventTypeMetadata(clazz.getName(), null, EventTypeTypeClass.BEAN_INCIDENTAL, EventTypeApplicationType.CLASS, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, computeTypeId(clazz.getName()));

        // supertypes
        EventType[] superTypes = getSuperTypes(stem.getSuperTypes());
        Set<EventType> deepSuperTypes = getDeepSupertypes(stem.getDeepSuperTypes());

        // bean type
        BeanEventType eventType = eventTypeFactory.createBeanType(stem, metadata, this, superTypes, deepSuperTypes, null, null);

        types.put(clazz, eventType);
        return eventType;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return typedEventFactory;
    }

    public NameAccessModifier getVisibility() {
        return NameAccessModifier.TRANSIENT;
    }

    public EventTypeFactory getEventTypeFactory() {
        return eventTypeFactory;
    }

    public EventTypeIdPair computeTypeId(String eventTypeName) {
        long id = CRC32Util.computeCRC32(eventTypeName);
        return new EventTypeIdPair(0, id);
    }

    private EventType[] getSuperTypes(Class[] superTypes) {
        if (superTypes == null || superTypes.length == 0) {
            return null;
        }
        EventType[] types = new EventType[superTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = getCreateBeanType(superTypes[i], false);
        }
        return types;
    }

    private Set<EventType> getDeepSupertypes(Set<Class> superTypes) {
        if (superTypes == null || superTypes.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<EventType> supers = new LinkedHashSet<>(4);
        for (Class clazz : superTypes) {
            supers.add(getCreateBeanType(clazz, false));
        }
        return supers;
    }
}
