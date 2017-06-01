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
import com.espertech.esper.client.FragmentEventType;

/**
 * Descriptor of a property item.
 */
public class PropertySetDescriptorItem {
    private EventPropertyDescriptor propertyDescriptor;
    private Class simplePropertyType;
    private EventPropertyGetterSPI propertyGetter;
    private FragmentEventType fragmentEventType;

    public PropertySetDescriptorItem(EventPropertyDescriptor propertyDescriptor, Class simplePropertyType, EventPropertyGetterSPI propertyGetter, FragmentEventType fragmentEventType) {
        this.propertyDescriptor = propertyDescriptor;
        this.simplePropertyType = simplePropertyType;
        this.propertyGetter = propertyGetter;
        this.fragmentEventType = fragmentEventType;
    }

    public EventPropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    public Class getSimplePropertyType() {
        return simplePropertyType;
    }

    public EventPropertyGetterSPI getPropertyGetter() {
        return propertyGetter;
    }

    public FragmentEventType getFragmentEventType() {
        return fragmentEventType;
    }
}
