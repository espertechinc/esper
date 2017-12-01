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
package com.espertech.esper.event.property;

/**
 * All properties have a property name and this is the abstract base class that serves up the property name.
 */
public abstract class PropertyBase implements Property {
    /**
     * Property name.
     */
    protected String propertyNameAtomic;

    /**
     * Ctor.
     *
     * @param propertyName is the name of the property
     */
    public PropertyBase(String propertyName) {
        this.propertyNameAtomic = PropertyParser.unescapeBacktickForProperty(propertyName);
    }

    /**
     * Returns the atomic property name, which is a part of all of the full (complex) property name.
     *
     * @return atomic name of property
     */
    public String getPropertyNameAtomic() {
        return propertyNameAtomic;
    }

    public boolean isDynamic() {
        return false;
    }
}
