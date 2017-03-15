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
package com.espertech.esper.client.hook;

/**
 * For Avro schemas for mapping a given type to a given Avro schema.
 */
public interface TypeRepresentationMapper {
    /**
     * Return Avro schema for type information provided.
     * @param context type and contextual information
     * @return schema
     */
    Object map(TypeRepresentationMapperContext context);
}
