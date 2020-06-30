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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

/**
 * Descriptor for a variant stream property.
 */
public class VariantPropertyDesc {
    private final EPType propertyType;
    private final EventPropertyGetterSPI getter;
    private final boolean isProperty;

    /**
     * Ctor.
     *
     * @param propertyType type or null if not exists
     * @param getter       the getter or null if not exists
     * @param property     the boolean indicating whether it exists or not
     */
    public VariantPropertyDesc(EPType propertyType, EventPropertyGetterSPI getter, boolean property) {
        if (propertyType == null) {
            throw new IllegalArgumentException("Null property type");
        }
        this.propertyType = propertyType;
        this.getter = getter;
        isProperty = property;
    }

    /**
     * True if the property exists, false if not.
     *
     * @return indicator whether property exists
     */
    public boolean isProperty() {
        return isProperty;
    }

    /**
     * Returns the property type.
     *
     * @return property type
     */
    public EPType getPropertyType() {
        return propertyType;
    }

    /**
     * Returns the getter for the property.
     *
     * @return property getter
     */
    public EventPropertyGetterSPI getGetter() {
        return getter;
    }
}
