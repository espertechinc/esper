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
 * Annotation for defining the name of the functions returning external data window key and value objects for use with queries against external data windows.
 */
public @interface ExternalDWQuery {
    /**
     * Returns function name that return key objects.
     *
     * @return function name
     */
    String functionKeys() default "";

    /**
     * Returns function name that return value objects.
     *
     * @return function name
     */
    String functionValues() default "";
}
