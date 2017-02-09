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
package com.espertech.esper.client.annotation;

/**
 * Annotation for defining the name of the property or the function name returning the external data window key values.
 */
public @interface ExternalDWKey {
    /**
     * Property name acting as key.
     *
     * @return key property name
     */
    String property() default "";

    /**
     * Multiple property names acting as key (check for support in the documentation).
     *
     * @return property names array
     */
    String[] propertyNames() default {};

    /**
     * Key generator function.
     *
     * @return function name
     */
    String function() default "";
}
