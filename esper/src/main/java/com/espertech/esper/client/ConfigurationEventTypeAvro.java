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

import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;

public class ConfigurationEventTypeAvro extends ConfigurationEventTypeWithSupertype implements MetaDefItem, Serializable
{
    private String avroSchemaText;
    private Object avroSchema;

    public ConfigurationEventTypeAvro() {
    }

    public ConfigurationEventTypeAvro(Object avroSchema) {
        this.avroSchema = avroSchema;
    }

    public Object getAvroSchema() {
        return avroSchema;
    }

    public ConfigurationEventTypeAvro setAvroSchema(Object avroSchema) {
        this.avroSchema = avroSchema;
        return this;
    }

    public String getAvroSchemaText() {
        return avroSchemaText;
    }

    public ConfigurationEventTypeAvro setAvroSchemaText(String avroSchemaText) {
        this.avroSchemaText = avroSchemaText;
        return this;
    }
}
