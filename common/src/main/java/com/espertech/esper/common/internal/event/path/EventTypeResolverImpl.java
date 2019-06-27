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
package com.espertech.esper.common.internal.event.path;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.serde.runtime.event.EventSerdeFactory;

import java.util.Collections;
import java.util.Map;

public class EventTypeResolverImpl implements EventTypeResolver, EventTypeNameResolver {
    private final Map<String, EventType> locals;
    private final PathRegistry<String, EventType> path;
    private final EventTypeNameResolver publics;
    private final BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate;
    private final EventSerdeFactory eventSerdeFactory;

    public EventTypeResolverImpl(Map<String, EventType> locals, PathRegistry<String, EventType> path, EventTypeNameResolver publics, BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate, EventSerdeFactory eventSerdeFactory) {
        this.locals = locals;
        this.path = path;
        this.publics = publics;
        this.beanEventTypeFactoryPrivate = beanEventTypeFactoryPrivate;
        this.eventSerdeFactory = eventSerdeFactory;
    }

    public EventType getTypeByName(String typeName) {
        EventType localType = locals.get(typeName);
        if (localType != null) {
            return localType;
        }
        EventType publicType = publics.getTypeByName(typeName);
        if (publicType != null) {
            return publicType;
        }
        try {
            Pair<EventType, String> pair = path.getAnyModuleExpectSingle(typeName, null);
            return pair == null ? null : pair.getFirst();
        } catch (PathException e) {
            throw new EPException("Event type name '" + typeName + "' is ambigous: " + e.getMessage(), e);
        }
    }

    public BeanEventType resolvePrivateBean(Class clazz, boolean publicFields) {
        return beanEventTypeFactoryPrivate.getCreateBeanType(clazz, publicFields);
    }

    public EventType resolve(EventTypeMetadata metadata) {
        return resolve(metadata, publics, locals, path);
    }

    public EventSerdeFactory getEventSerdeFactory() {
        return eventSerdeFactory;
    }

    public static EventType resolve(EventTypeMetadata metadata, EventTypeNameResolver publics, Map<String, EventType> locals, PathRegistry<String, EventType> path) {
        EventType type;
        // public can only see public
        if (metadata.getAccessModifier() == NameAccessModifier.PRECONFIGURED) {
            type = publics.getTypeByName(metadata.getName());

            // for create-schema the type may be defined by the same module
            if (type == null) {
                type = locals.get(metadata.getName());
            }
        } else if (metadata.getAccessModifier() == NameAccessModifier.PUBLIC || metadata.getAccessModifier() == NameAccessModifier.PROTECTED) {
            // path-visibility can be provided as local
            EventType local = locals.get(metadata.getName());
            if (local != null) {
                if (local.getMetadata().getAccessModifier() == NameAccessModifier.PUBLIC || local.getMetadata().getAccessModifier() == NameAccessModifier.PROTECTED) {
                    return local;
                }
            }
            try {
                Pair<EventType, String> pair = path.getAnyModuleExpectSingle(metadata.getName(), Collections.singleton(metadata.getModuleName()));
                type = pair == null ? null : pair.getFirst();
            } catch (PathException e) {
                throw new EPException(e.getMessage(), e);
            }
        } else {
            type = locals.get(metadata.getName());
        }
        if (type == null) {
            throw new EPException("Failed to find event type '" + metadata.getName() + "' among public types, modules-in-path or the current module itself");
        }
        return type;
    }

}
