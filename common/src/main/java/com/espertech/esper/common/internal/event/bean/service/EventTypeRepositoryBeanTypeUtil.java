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
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.metric.RuntimeMetric;
import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepository;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;


public class EventTypeRepositoryBeanTypeUtil {
    public static void buildBeanTypes(BeanEventTypeStemService beanEventTypeStemService,
                                      EventTypeRepositoryImpl repo,
                                      Map<String, Class> beanTypes,
                                      BeanEventTypeFactoryPrivate privateFactory,
                                      Map<String, ConfigurationCommonEventTypeBean> configs) {

        if (beanTypes.isEmpty()) {
            beanTypes = new HashMap<>();
        }
        addPredefinedBeanEventTypes(beanTypes);

        for (Map.Entry<String, Class> beanType : beanTypes.entrySet()) {
            if (repo.getTypeByName(beanType.getKey()) != null) {
                continue;
            }
            buildPublicBeanType(beanEventTypeStemService, repo, beanType.getKey(), beanType.getValue(), privateFactory, configs);
        }
    }

    private static void buildPublicBeanType(BeanEventTypeStemService beanEventTypeStemService, EventTypeRepository repo, String eventTypeName, Class clazz, BeanEventTypeFactoryPrivate privateFactory, Map<String, ConfigurationCommonEventTypeBean> configs) {

        // check existing type
        EventType existingType = repo.getTypeByName(eventTypeName);
        if (existingType != null) {
            if (existingType.getMetadata().getApplicationType() != EventTypeApplicationType.CLASS) {
                throw new ConfigurationException("Event type named '" + eventTypeName +
                        "' has already been declared with differing underlying type information: Class " + existingType.getUnderlyingType().getName() +
                        " versus " + clazz.getName());
            }
            BeanEventType beanEventType = (BeanEventType) existingType;
            if (beanEventType.getUnderlyingType() != clazz) {
                throw new ConfigurationException("Event type named '" + eventTypeName +
                        "' has already been declared with differing underlying type information: Class " + existingType.getUnderlyingType().getName() +
                        " versus " + beanEventType.getUnderlyingType());
            }
            return;
        }

        ConfigurationCommonEventTypeBean optionalConfig = configs.get(eventTypeName);

        // check-allocate bean-stem
        BeanEventTypeStem stem = beanEventTypeStemService.getCreateStem(clazz, optionalConfig);

        // metadata
        long publicId = CRC32Util.computeCRC32(eventTypeName);
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.NONBUS, false, new EventTypeIdPair(publicId, -1));

        // supertypes
        EventType[] superTypes = getSuperTypes(stem.getSuperTypes(), beanEventTypeStemService, repo, privateFactory, configs);
        Set<EventType> deepSuperTypes = getDeepSupertypes(stem.getDeepSuperTypes(), beanEventTypeStemService, repo, privateFactory, configs);

        // bean type
        String startTS = optionalConfig == null ? null : optionalConfig.getStartTimestampPropertyName();
        String endTS = optionalConfig == null ? null : optionalConfig.getEndTimestampPropertyName();
        BeanEventType eventType = privateFactory.getEventTypeFactory().createBeanType(stem, metadata, privateFactory, superTypes, deepSuperTypes, startTS, endTS);

        repo.addType(eventType);
    }

    private static EventType[] getSuperTypes(Class[] superTypes, BeanEventTypeStemService beanEventTypeStemService, EventTypeRepository repo, BeanEventTypeFactoryPrivate privateFactory, Map<String, ConfigurationCommonEventTypeBean> configs) {
        if (superTypes == null || superTypes.length == 0) {
            return null;
        }
        EventType[] types = new EventType[superTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = getBuildSuperType(superTypes[i], beanEventTypeStemService, repo, privateFactory, configs);
        }
        return types;
    }

    private static Set<EventType> getDeepSupertypes(Set<Class> superTypes, BeanEventTypeStemService beanEventTypeStemService, EventTypeRepository repo, BeanEventTypeFactoryPrivate privateFactory, Map<String, ConfigurationCommonEventTypeBean> configs) {
        if (superTypes == null || superTypes.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<EventType> supers = new LinkedHashSet<>(4);
        for (Class clazz : superTypes) {
            supers.add(getBuildSuperType(clazz, beanEventTypeStemService, repo, privateFactory, configs));
        }
        return supers;
    }

    public static EventType getBuildSuperType(Class clazz, BeanEventTypeStemService beanEventTypeStemService, EventTypeRepository repo, BeanEventTypeFactoryPrivate privateFactory, Map<String, ConfigurationCommonEventTypeBean> configs) {
        List<String> existingSuperTypeNames = beanEventTypeStemService.getPublicClassToTypeNames().get(clazz);
        if (existingSuperTypeNames != null) {
            EventType eventType = repo.getTypeByName(existingSuperTypeNames.get(0));
            if (eventType != null) {
                return eventType;
            }
        }
        buildPublicBeanType(beanEventTypeStemService, repo, clazz.getName(), clazz, privateFactory, configs);
        return repo.getTypeByName(clazz.getName());
    }

    private static void addPredefinedBeanEventTypes(Map<String, Class> resolvedBeanEventTypes) {
        addPredefinedBeanEventType(StatementMetric.class, resolvedBeanEventTypes);
        addPredefinedBeanEventType(RuntimeMetric.class, resolvedBeanEventTypes);
    }

    private static void addPredefinedBeanEventType(Class clazz, Map<String, Class> resolvedBeanEventTypes) {
        Class existing = resolvedBeanEventTypes.get(clazz.getName());
        if (existing != null && existing != clazz) {
            throw new ConfigurationException("Predefined event type " + clazz.getName() + " expected class " + JavaClassHelper.getClassNameFullyQualPretty(clazz) + " but is already defined to another class " + existing.getName());
        }
        resolvedBeanEventTypes.put(clazz.getName(), clazz);
    }
}
