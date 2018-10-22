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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStemBuilder;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanEventTypeStemService {
    private final Map<Class, List<String>> publicClassToTypeNames;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final PropertyResolutionStyle defaultPropertyResolutionStyle;
    private final AccessorStyle defaultAccessorStyle;

    private final Map<Class, BeanEventTypeStem> stems = new HashMap<>();

    public BeanEventTypeStemService(Map<Class, List<String>> publicClassToTypeNames, EventBeanTypedEventFactory eventBeanTypedEventFactory, PropertyResolutionStyle defaultPropertyResolutionStyle, AccessorStyle defaultAccessorStyle) {
        this.publicClassToTypeNames = publicClassToTypeNames;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.defaultPropertyResolutionStyle = defaultPropertyResolutionStyle;
        this.defaultAccessorStyle = defaultAccessorStyle;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanTypedEventFactory;
    }

    public Map<Class, List<String>> getPublicClassToTypeNames() {
        return publicClassToTypeNames;
    }

    public BeanEventTypeStem getCreateStem(Class clazz, ConfigurationCommonEventTypeBean optionalConfiguration) {
        BeanEventTypeStem stem = stems.get(clazz);
        if (stem != null) {
            return stem;
        }

        if ((optionalConfiguration == null) && (defaultAccessorStyle != AccessorStyle.JAVABEAN)) {
            optionalConfiguration = new ConfigurationCommonEventTypeBean();
            optionalConfiguration.setAccessorStyle(defaultAccessorStyle);
        }

        stem = new BeanEventTypeStemBuilder(optionalConfiguration, defaultPropertyResolutionStyle).make(clazz);
        stems.put(clazz, stem);
        return stem;
    }
}
