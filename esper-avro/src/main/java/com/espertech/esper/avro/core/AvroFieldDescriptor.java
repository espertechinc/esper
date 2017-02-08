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
package com.espertech.esper.avro.core;

import org.apache.avro.Schema;

public class AvroFieldDescriptor {
    private final Schema.Field field;
    private final boolean dynamic;
    private final boolean accessedByIndex;
    private final boolean accessedByKey;

    public AvroFieldDescriptor(Schema.Field field, boolean dynamic, boolean accessedByIndex, boolean accessedByKey) {
        this.field = field;
        this.dynamic = dynamic;
        this.accessedByIndex = accessedByIndex;
        this.accessedByKey = accessedByKey;
    }

    public Schema.Field getField() {
        return field;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isAccessedByIndex() {
        return accessedByIndex;
    }

    public boolean isAccessedByKey() {
        return accessedByKey;
    }
}
