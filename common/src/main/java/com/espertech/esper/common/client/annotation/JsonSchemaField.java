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
package com.espertech.esper.common.client.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for use with Json to provide an adapter for a given event property name.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSchemaField {
    /**
     * Returns the field name
     * @return field name
     */
    String name();

    /**
     * Returns the adapter class name
     * @return adapter class name
     */
    String adapter();
}
