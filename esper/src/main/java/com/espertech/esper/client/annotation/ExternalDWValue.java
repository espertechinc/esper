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
 * Annotation for mapping of event-to-value and value-to-event for external data windows.
 */
public @interface ExternalDWValue {
    /**
     * Returns the function name of the function that maps event beans to value objects.
     *
     * @return event to value mapping function name
     */
    String functionBeanToValue();

    /**
     * Returns the function name of the function that maps values to event objects.
     *
     * @return value to event mapping function name
     */
    String functionValueToBean();
}
