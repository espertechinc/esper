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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.annotation.AvroSchemaField;

import java.lang.annotation.Annotation;

public class AvroSchemaFieldHook implements AvroSchemaField {
    private final String name;
    private final String schema;

    public AvroSchemaFieldHook(String name, String schema) {
        this.name = name;
        this.schema = schema;
    }

    public String name() {
        return name;
    }

    public String schema() {
        return schema;
    }

    public Class<? extends Annotation> annotationType() {
        return AvroSchemaField.class;
    }
}
