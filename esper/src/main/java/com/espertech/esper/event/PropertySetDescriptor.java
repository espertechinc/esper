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
package com.espertech.esper.event;

import com.espertech.esper.client.EventPropertyDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Descriptor of a property set.
 */
public class PropertySetDescriptor {
    private final List<String> propertyNameList;
    private final List<EventPropertyDescriptor> propertyDescriptors;
    private final Map<String, PropertySetDescriptorItem> propertyItems;
    private final Map<String, Object> nestableTypes;

    public PropertySetDescriptor(List<String> propertyNameList, List<EventPropertyDescriptor> propertyDescriptors, Map<String, PropertySetDescriptorItem> propertyItems, Map<String, Object> nestableTypes) {
        this.propertyNameList = propertyNameList;
        this.propertyDescriptors = propertyDescriptors;
        this.propertyItems = propertyItems;
        this.nestableTypes = nestableTypes;
    }

    public Map<String, PropertySetDescriptorItem> getPropertyItems() {
        return propertyItems;
    }

    /**
     * Returns property name list.
     *
     * @return property name list
     */
    public List<String> getPropertyNameList() {
        return propertyNameList;
    }

    /**
     * Returns the property descriptors.
     *
     * @return property descriptors
     */
    public List<EventPropertyDescriptor> getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public Map<String, Object> getNestableTypes() {
        return nestableTypes;
    }

    public String[] getPropertyNameArray() {
        return propertyNameList.toArray(new String[propertyNameList.size()]);
    }
}
