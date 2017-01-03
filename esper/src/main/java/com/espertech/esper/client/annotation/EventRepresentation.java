/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.client.annotation;

/**
 * Annotation that can be attached to specify which underlying event representation to use for events.
 */
public @interface EventRepresentation
{
    /**
     * True for object-array, false for Map. May not be used with Avro flag.
     * @return array indicator
     */
    public boolean array() default false;

    /**
     * True for Avro. May not be used with array flag.
     * @return avro indicator
     */
    public boolean avro() default false;
}
