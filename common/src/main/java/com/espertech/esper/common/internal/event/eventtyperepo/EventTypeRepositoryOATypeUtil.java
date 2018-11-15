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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeObjectArray;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventAdapterException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.CRC32Util;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventTypeRepositoryOATypeUtil {
    public static void buildOATypes(EventTypeRepositoryImpl repo, Map<String, ConfigurationCommonEventTypeObjectArray> objectArrayTypeConfigurations, Map<String, Map<String, Object>> nestableObjectArrayNames, BeanEventTypeFactory beanEventTypeFactory, ClasspathImportService classpathImportService) {
        List<String> creationOrder = EventTypeRepositoryUtil.getCreationOrder(Collections.emptySet(), nestableObjectArrayNames.keySet(), objectArrayTypeConfigurations);

        for (String objectArrayName : creationOrder) {
            if (repo.getTypeByName(objectArrayName) != null) {
                continue;
            }

            ConfigurationCommonEventTypeObjectArray objectArrayConfig = objectArrayTypeConfigurations.get(objectArrayName);
            Map<String, Object> propertyTypes = nestableObjectArrayNames.get(objectArrayName);
            propertyTypes = resolveClassesForStringPropertyTypes(propertyTypes, classpathImportService);
            LinkedHashMap<String, Object> propertyTypesCompiled = EventTypeUtility.compileMapTypeProperties(propertyTypes, repo);

            addNestableObjectArrayType(objectArrayName, propertyTypesCompiled, objectArrayConfig, beanEventTypeFactory, repo);
        }
    }

    private static void addNestableObjectArrayType(String eventTypeName, LinkedHashMap<String, Object> propertyTypesMayHavePrimitive, ConfigurationCommonEventTypeObjectArray optionalConfig, BeanEventTypeFactory beanEventTypeFactory, EventTypeRepositoryImpl repo) {
        if (optionalConfig != null && optionalConfig.getSuperTypes().size() > 1) {
            throw new EventAdapterException(ConfigurationCommonEventTypeObjectArray.SINGLE_SUPERTYPE_MSG);
        }
        LinkedHashMap<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(propertyTypesMayHavePrimitive);
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.APPLICATION, EventTypeApplicationType.OBJECTARR, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, false, new EventTypeIdPair(CRC32Util.computeCRC32(eventTypeName), -1));
        String[] superTypes = null;
        if (optionalConfig != null && optionalConfig.getSuperTypes() != null && !optionalConfig.getSuperTypes().isEmpty()) {
            superTypes = optionalConfig.getSuperTypes().toArray(new String[optionalConfig.getSuperTypes().size()]);
        }
        ObjectArrayEventType newEventType = beanEventTypeFactory.getEventTypeFactory().createObjectArray(metadata, propertyTypes, superTypes,
            optionalConfig != null ? optionalConfig.getStartTimestampPropertyName() : null,
            optionalConfig != null ? optionalConfig.getEndTimestampPropertyName() : null,
            beanEventTypeFactory, repo);

        EventType existingType = repo.getTypeByName(eventTypeName);
        if (existingType != null) {
            // The existing type must be the same as the type createdStatement
            if (newEventType.equalsCompareType(existingType) != null) {
                ExprValidationException message = newEventType.compareEquals(existingType);
                throw new EPException("Event type named '" + eventTypeName +
                    "' has already been declared with differing column name or type information: " + message.getMessage(), message);
            }

            // Since it's the same, return the existing type
            return;
        }

        repo.addType(newEventType);
    }

    private static Map<String, Object> resolveClassesForStringPropertyTypes(Map<String, Object> properties, ClasspathImportService classpathImportService) {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry entry : properties.entrySet()) {
            String property = (String) entry.getKey();
            propertyTypes.put(property, entry.getValue());
            if (!(entry.getValue() instanceof String)) {
                continue;
            }
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
            return null;
        }

        if (isArray) {
            clazz = Array.newInstance(clazz, 0).getClass();
        }
        return clazz;
    }
}
