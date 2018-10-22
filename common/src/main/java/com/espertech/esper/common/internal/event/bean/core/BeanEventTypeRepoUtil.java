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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import java.util.*;

public class BeanEventTypeRepoUtil {

    public static BeanEventTypeStemService makeBeanEventTypeStemService(Configuration configurationSnapshot, Map<String, Class> resolvedBeanEventTypes, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        Map<Class, List<String>> publicClassToTypeNames = Collections.emptyMap();
        if (!resolvedBeanEventTypes.isEmpty()) {
            publicClassToTypeNames = new HashMap<>();
            for (Map.Entry<String, Class> entry : resolvedBeanEventTypes.entrySet()) {
                List<String> names = publicClassToTypeNames.get(entry.getValue());
                if (names == null) {
                    names = new ArrayList<>(1);
                    publicClassToTypeNames.put(entry.getValue(), names);
                }
                names.add(entry.getKey());
            }
        }
        return new BeanEventTypeStemService(publicClassToTypeNames, eventBeanTypedEventFactory, configurationSnapshot.getCommon().getEventMeta().getClassPropertyResolutionStyle(), configurationSnapshot.getCommon().getEventMeta().getDefaultAccessorStyle());
    }

    public static Map<String, Class> resolveBeanEventTypes(Map<String, String> typeToClassName, ClasspathImportService classpathImportService) {
        if (typeToClassName.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Class> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : typeToClassName.entrySet()) {
            Class clazz;
            try {
                clazz = classpathImportService.resolveClassForBeanEventType(entry.getValue());
            } catch (ClasspathImportException ex) {
                throw new ConfigurationException("Class named '" + entry.getValue() + "' was not found", ex);
            }
            resolved.put(entry.getKey(), clazz);
        }
        return resolved;
    }
}
