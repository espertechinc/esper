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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Represents a type definition for use with the create-schema syntax for creating a new event type.
 */
public enum CreateSchemaClauseTypeDef implements Serializable {
    /**
     * Variant type.
     */
    VARIANT,

    /**
     * Map underlying type.
     */
    MAP,

    /**
     * Object-array underlying type.
     */
    OBJECTARRAY,

    /**
     * Avro-array underlying type.
     */
    AVRO,

    /**
     * Undefined (system default) underlying type.
     */
    NONE;

    /**
     * Write keyword according to type def.
     *
     * @param writer to write to
     */
    public void write(StringWriter writer) {
        if (this == VARIANT) {
            writer.write(" variant");
        } else if (this == MAP) {
            writer.write(" map");
        } else if (this == OBJECTARRAY) {
            writer.write(" objectarray");
        } else if (this == AVRO) {
            writer.write(" avro");
        }
    }
}
