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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMap;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeWithSupertype;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventAdapterException;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.*;

public class EventTypeRepositoryMapTypeUtil {
    public static void buildMapTypes(EventTypeRepositoryImpl repo,
                                     Map<String, ConfigurationCommonEventTypeMap> mapTypeConfigurations,
                                     Map<String, Properties> mapTypes,
                                     Map<String, Map<String, Object>> nestableMapEvents,
                                     BeanEventTypeFactory beanEventTypeFactory,
                                     ClasspathImportService classpathImportService) {
        List<String> creationOrder = EventTypeRepositoryUtil.getCreationOrder(mapTypes.keySet(), nestableMapEvents.keySet(), mapTypeConfigurations);
        for (String mapName : creationOrder) {
            if (repo.getTypeByName(mapName) != null) {
                continue;
            }

            ConfigurationCommonEventTypeMap mapConfig = mapTypeConfigurations.get(mapName);
            Properties propertiesUnnested = mapTypes.get(mapName);
            if (propertiesUnnested != null) {
                Map<String, Object> propertyTypes = createPropertyTypes(propertiesUnnested, classpathImportService);
                LinkedHashMap<String, Object> propertyTypesCompiled = EventTypeUtility.compileMapTypeProperties(propertyTypes, repo);
                addNestableMapType(mapName, propertyTypesCompiled, mapConfig, repo, beanEventTypeFactory, repo);
            }

            Map<String, Object> propertiesNestable = nestableMapEvents.get(mapName);
            if (propertiesNestable != null) {
                LinkedHashMap<String, Object> propertiesNestableCompiled = EventTypeUtility.compileMapTypeProperties(propertiesNestable, repo);
                addNestableMapType(mapName, propertiesNestableCompiled, mapConfig, repo, beanEventTypeFactory, repo);
            }
        }
    }

    protected static Map<String, Set<String>> toTypesReferences(Map<String, ? extends ConfigurationCommonEventTypeWithSupertype> mapTypeConfigurations) {
        Map<String, Set<String>> result = new LinkedHashMap<String, Set<String>>();
        for (Map.Entry<String, ? extends ConfigurationCommonEventTypeWithSupertype> entry : mapTypeConfigurations.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getSuperTypes());
        }
        return result;
    }

    private static void addNestableMapType(String eventTypeName,
                                           LinkedHashMap<String, Object> propertyTypesMayHavePrimitive,
                                           ConfigurationCommonEventTypeMap optionalConfig,
                                           EventTypeRepositoryImpl repo,
                                           BeanEventTypeFactory beanEventTypeFactory,
                                           EventTypeNameResolver eventTypeNameResolver) throws EventAdapterException {
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.APPLICATION, EventTypeApplicationType.MAP, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, false, new EventTypeIdPair(CRC32Util.computeCRC32(eventTypeName), -1));

        LinkedHashMap<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        String[] superTypes = null;
        if (optionalConfig != null && optionalConfig.getSuperTypes() != null && !optionalConfig.getSuperTypes().isEmpty()) {
            superTypes = optionalConfig.getSuperTypes().toArray(new String[optionalConfig.getSuperTypes().size()]);
        }
        MapEventType newEventType = beanEventTypeFactory.getEventTypeFactory().createMap(metadata, propertyTypes,
            superTypes,
            optionalConfig != null ? optionalConfig.getStartTimestampPropertyName() : null,
            optionalConfig != null ? optionalConfig.getEndTimestampPropertyName() : null,
            beanEventTypeFactory, eventTypeNameResolver);

        EventType existingType = repo.getTypeByName(eventTypeName);
        if (existingType != null) {
            // The existing type must be the same as the type createdStatement
            if (newEventType.equalsCompareType(existingType) != null) {
                ExprValidationException message = newEventType.compareEquals(existingType);
                throw new EPException("Event type named '" + eventTypeName +
                    "' has already been declared with differing column name or type information: " + message.getMessage(), message);
            }
            return;
        }

        repo.addType(newEventType);
    }

    private static Map<String, Object> createPropertyTypes(Properties properties, ClasspathImportService classpathImportService) {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry entry : properties.entrySet()) {
            String property = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class clazz = resolveClassForTypeName(className, classpathImportService);
            if (clazz != null) {
                propertyTypes.put(property, clazz);
            }
        }
        return propertyTypes;
    }

    private static Class resolveClassForTypeName(String type, ClasspathImportService classpathImportService) {
        boolean isArray = false;
        if (type != null && EventTypeUtility.isPropertyArray(type)) {
            isArray = true;
            type = EventTypeUtility.getPropertyRemoveArray(type);
        }

        if (type == null) {
            throw new ConfigurationException("A null value has been provided for the type");
        }
        Class clazz = JavaClassHelper.getClassForSimpleName(type, classpathImportService.getClassForNameProvider());
        if (clazz == null) {
            throw new ConfigurationException("The type '" + type + "' is not a recognized type");
        }

        if (isArray) {
            clazz = Array.newInstance(clazz, 0).getClass();
        }
        return clazz;
    }
}
