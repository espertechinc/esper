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
package com.espertech.esper.client;

import java.util.Set;

/**
 * Configuration object for Object array-based event types.
 */
public class ConfigurationEventTypeObjectArray extends ConfigurationEventTypeWithSupertype {
    private static final long serialVersionUID = -5404892001689512405L;

    /**
     * Message for single supertype for object-arrays.
     */
    public static final String SINGLE_SUPERTYPE_MSG = "Object-array event types only allow a single supertype";

    /**
     * Ctor.
     *
     * @param superTypes super types
     */
    public ConfigurationEventTypeObjectArray(Set<String> superTypes) {
        super(superTypes);
        if (superTypes.size() > 1) {
            throw new ConfigurationException("Object-array event types may not have multiple supertypes");
        }
    }

    /**
     * Ctor.
     */
    public ConfigurationEventTypeObjectArray() {
    }
}
