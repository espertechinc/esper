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
 * Annotation for use with Avro to provide a schema for a given event property.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AvroSchemaField {
    /**
     * Property name.
     *
     * @return name
     */
    String name();

    /**
     * Schema text.
     *
     * @return schema text
     */
    String schema();
}
