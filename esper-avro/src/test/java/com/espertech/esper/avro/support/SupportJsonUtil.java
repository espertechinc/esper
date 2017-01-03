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

package com.espertech.esper.avro.support;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SupportJsonUtil {
    public static GenericData.Record parseQuoted(Schema schema, String json) {
        return parse(schema, json.replace("'", "\""));
    }

    public static GenericData.Record parse(Schema schema, String json) {
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);
        try {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
            DatumReader<Object> reader = new GenericDatumReader<>(schema);
            return (GenericData.Record) reader.read(null, decoder);
        }
        catch (IOException ex) {
            throw new RuntimeException("Failed to parse json: " + ex.getMessage(), ex);
        }
    }
}
