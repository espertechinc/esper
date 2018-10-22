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
package com.espertech.esper.common.client.util;

/**
 * Accessor style defines the methods of a class that are automatically exposed via event property.
 */
public enum AccessorStyle {     // ensure the names match the configuration schema type restriction defs
    /**
     * Expose JavaBean-style getter methods only, plus explicitly configured properties.
     */
    JAVABEAN,

    /**
     * Expose only the explicitly configured methods and public members as event properties.
     */
    EXPLICIT,

    /**
     * Expose all public methods and public members as event properties, plus explicitly configured properties.
     */
    PUBLIC
}
