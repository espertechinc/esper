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
 * Descriptor for a type and its generic type, if any.
 */
public class GenericPropertyDesc {
    private static final GenericPropertyDesc OBJECT_GENERIC = new GenericPropertyDesc(Object.class);

    private final Class type;
    private final Class generic;

    /**
     * Ctor.
     *
     * @param type    the type
     * @param generic its generic type parameter, if any
     */
    public GenericPropertyDesc(Class type, Class generic) {
        this.type = type;
        this.generic = generic;
    }

    /**
     * Ctor.
     *
     * @param type the type
     */
    public GenericPropertyDesc(Class type) {
        this.type = type;
        this.generic = null;
    }

    /**
     * Returns the type.
     *
     * @return type
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns the generic parameter, or null if none.
     *
     * @return generic parameter
     */
    public Class getGeneric() {
        return generic;
    }

    /**
     * Object.class type.
     *
     * @return type descriptor
     */
    public static GenericPropertyDesc getObjectGeneric() {
        return OBJECT_GENERIC;
    }
}
