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
package com.espertech.esper.client;

import java.io.Serializable;

/**
 * Configuration for Avro event types.
 */
public class ConfigurationEventTypeAvro extends ConfigurationEventTypeWithSupertype implements Serializable {
    private static final long serialVersionUID = 6514448750173344310L;
    private String avroSchemaText;
    private Object avroSchema;

    /**
     * Ctor.
     */
    public ConfigurationEventTypeAvro() {
    }

    /**
     * Ctor.
     * @param avroSchema avro schema
     */
    public ConfigurationEventTypeAvro(Object avroSchema) {
        this.avroSchema = avroSchema;
    }

    /**
     * Returns the avro schema
     * @return avro schema
     */
    public Object getAvroSchema() {
        return avroSchema;
    }

    /**
     * Sets the avro schema
     * @param avroSchema avro schema
     * @return this
     */
    public ConfigurationEventTypeAvro setAvroSchema(Object avroSchema) {
        this.avroSchema = avroSchema;
        return this;
    }

    /**
     * Returns the avro schema text
     * @return avro schema text
     */
    public String getAvroSchemaText() {
        return avroSchemaText;
    }

    /**
     * Returns the avro schema text
     * @param avroSchemaText avro schema text
     * @return this
     */
    public ConfigurationEventTypeAvro setAvroSchemaText(String avroSchemaText) {
        this.avroSchemaText = avroSchemaText;
        return this;
    }
}
