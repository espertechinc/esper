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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.FragmentEventType;

/**
 * Descriptor of a property item.
 */
public class PropertySetDescriptorItem {
    private EventPropertyDescriptor propertyDescriptor;
    private EventPropertyGetterSPI propertyGetter;
    private FragmentEventType fragmentEventType;

    public PropertySetDescriptorItem(EventPropertyDescriptor propertyDescriptor, EventPropertyGetterSPI propertyGetter, FragmentEventType fragmentEventType) {
        this.propertyDescriptor = propertyDescriptor;
        this.propertyGetter = propertyGetter;
        this.fragmentEventType = fragmentEventType;
    }

    public EventPropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    public EventPropertyGetterSPI getPropertyGetter() {
        return propertyGetter;
    }

    public FragmentEventType getFragmentEventType() {
        return fragmentEventType;
    }
}
