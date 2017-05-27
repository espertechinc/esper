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
package com.espertech.esper.regression.event.avro;

import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
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

public class ExecEventAvroJsonWithSchema implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        String schemaText =
                "{\"namespace\": \"example.avro\",\n" +
                        " \"type\": \"record\",\n" +
                        " \"name\": \"User\",\n" +
                        " \"fields\": [\n" +
                        "     {\"name\": \"name\",  \"type\": {\n" +
                        "                              \"type\": \"string\",\n" +
                        "                              \"avro.java.string\": \"String\"\n" +
                        "                            }},\n" +
                        "     {\"name\": \"favorite_number\",  \"type\": \"int\"},\n" +
                        "     {\"name\": \"favorite_color\",  \"type\": {\n" +
                        "                              \"type\": \"string\",\n" +
                        "                              \"avro.java.string\": \"String\"\n" +
                        "                            }}\n" +
                        " ]\n" +
                        "}";
        Schema schema = new Schema.Parser().parse(schemaText);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("User", new ConfigurationEventTypeAvro(schema));

        String fields = "name,favorite_number,favorite_color";
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " + fields + " from User");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String eventOneJson = "{\"name\": \"Jane\", \"favorite_number\": 256, \"favorite_color\": \"red\"}";
        GenericData.Record record = parse(schema, eventOneJson);
        epService.getEPRuntime().sendEventAvro(record, "User");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[]{"Jane", 256, "red"});

        String eventTwoJson = "{\"name\": \"Hans\", \"favorite_number\": -1, \"favorite_color\": \"green\"}";
        record = parse(schema, eventTwoJson);
        epService.getEPRuntime().sendEventAvro(record, "User");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[]{"Hans", -1, "green"});
    }

    private static GenericData.Record parse(Schema schema, String json) {
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);
        try {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
            DatumReader<Object> reader = new GenericDatumReader<>(schema);
            return (GenericData.Record) reader.read(null, decoder);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse json: " + ex.getMessage(), ex);
        }
    }
}
