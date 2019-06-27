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
package com.espertech.esper.common.internal.event.bean.introspect;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanEventTypeStem {
    private final Class clazz;
    private final ConfigurationCommonEventTypeBean optionalLegacyDef;
    private final String[] propertyNames;
    private final Map<String, PropertyInfo> simpleProperties;
    private final Map<String, PropertyStem> mappedPropertyDescriptors;
    private final Map<String, PropertyStem> indexedPropertyDescriptors;
    private final Class[] superTypes;
    private final Set<Class> deepSuperTypes;
    private final PropertyResolutionStyle propertyResolutionStyle;

    private final Map<String, List<PropertyInfo>> simpleSmartPropertyTable;
    private final Map<String, List<PropertyInfo>> indexedSmartPropertyTable;
    private final Map<String, List<PropertyInfo>> mappedSmartPropertyTable;

    private final EventPropertyDescriptor[] propertyDescriptors;
    private final Map<String, EventPropertyDescriptor> propertyDescriptorMap;

    public BeanEventTypeStem(Class clazz, ConfigurationCommonEventTypeBean optionalLegacyDef, String[] propertyNames, Map<String, PropertyInfo> simpleProperties, Map<String, PropertyStem> mappedPropertyDescriptors, Map<String, PropertyStem> indexedPropertyDescriptors, Class[] superTypes, Set<Class> deepSuperTypes, PropertyResolutionStyle propertyResolutionStyle, Map<String, List<PropertyInfo>> simpleSmartPropertyTable, Map<String, List<PropertyInfo>> indexedSmartPropertyTable, Map<String, List<PropertyInfo>> mappedSmartPropertyTable, EventPropertyDescriptor[] propertyDescriptors, Map<String, EventPropertyDescriptor> propertyDescriptorMap) {
        this.clazz = clazz;
        this.optionalLegacyDef = optionalLegacyDef;
        this.propertyNames = propertyNames;
        this.simpleProperties = simpleProperties;
        this.mappedPropertyDescriptors = mappedPropertyDescriptors;
        this.indexedPropertyDescriptors = indexedPropertyDescriptors;
        this.superTypes = superTypes;
        this.deepSuperTypes = deepSuperTypes;
        this.propertyResolutionStyle = propertyResolutionStyle;
        this.simpleSmartPropertyTable = simpleSmartPropertyTable;
        this.indexedSmartPropertyTable = indexedSmartPropertyTable;
        this.mappedSmartPropertyTable = mappedSmartPropertyTable;
        this.propertyDescriptors = propertyDescriptors;
        this.propertyDescriptorMap = propertyDescriptorMap;
    }

    public Class getClazz() {
        return clazz;
    }

    public ConfigurationCommonEventTypeBean getOptionalLegacyDef() {
        return optionalLegacyDef;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public Map<String, PropertyInfo> getSimpleProperties() {
        return simpleProperties;
    }

    public Map<String, PropertyStem> getMappedPropertyDescriptors() {
        return mappedPropertyDescriptors;
    }

    public Map<String, PropertyStem> getIndexedPropertyDescriptors() {
        return indexedPropertyDescriptors;
    }

    public Class[] getSuperTypes() {
        return superTypes;
    }

    public Set<Class> getDeepSuperTypes() {
        return deepSuperTypes;
    }

    public PropertyResolutionStyle getPropertyResolutionStyle() {
        return propertyResolutionStyle;
    }

    public Map<String, List<PropertyInfo>> getSimpleSmartPropertyTable() {
        return simpleSmartPropertyTable;
    }

    public Map<String, List<PropertyInfo>> getIndexedSmartPropertyTable() {
        return indexedSmartPropertyTable;
    }

    public Map<String, List<PropertyInfo>> getMappedSmartPropertyTable() {
        return mappedSmartPropertyTable;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public Map<String, EventPropertyDescriptor> getPropertyDescriptorMap() {
        return propertyDescriptorMap;
    }

    public boolean isPublicFields() {
        return optionalLegacyDef != null && optionalLegacyDef.getAccessorStyle() == AccessorStyle.PUBLIC;
    }
}
