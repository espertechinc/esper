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

/**
 * Descriptor for explicit properties for use with {@link BaseConfigurableEventType}.
 */
public class ExplicitPropertyDescriptor {
    private final EventPropertyGetterSPI getter;
    private final EventPropertyDescriptor descriptor;
    private final String optionalFragmentTypeName;
    private final boolean isFragmentArray;

    /**
     * Ctor.
     *
     * @param descriptor               property descriptor
     * @param getter                   getter for values
     * @param fragmentArray            true if array fragment
     * @param optionalFragmentTypeName null if not a fragment, else fragment type name
     */
    public ExplicitPropertyDescriptor(EventPropertyDescriptor descriptor, EventPropertyGetterSPI getter, boolean fragmentArray, String optionalFragmentTypeName) {
        this.descriptor = descriptor;
        this.getter = getter;
        isFragmentArray = fragmentArray;
        this.optionalFragmentTypeName = optionalFragmentTypeName;
    }

    /**
     * Returns the property descriptor.
     *
     * @return property descriptor
     */
    public EventPropertyDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the getter.
     *
     * @return getter
     */
    public EventPropertyGetterSPI getGetter() {
        return getter;
    }

    /**
     * Returns the fragment event type name, or null if none defined.
     *
     * @return fragment type name
     */
    public String getOptionalFragmentTypeName() {
        return optionalFragmentTypeName;
    }

    /**
     * Returns true if an indexed, or false if not indexed.
     *
     * @return fragment indicator
     */
    public boolean isFragmentArray() {
        return isFragmentArray;
    }

    public String toString() {
        return descriptor.getPropertyName();
    }
}
