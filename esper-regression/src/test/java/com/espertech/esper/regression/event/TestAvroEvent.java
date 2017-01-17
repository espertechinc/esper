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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.expr.TestArrayExpression;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
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

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.array;
import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.record;

public class TestAvroEvent extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener = new SupportUpdateListener();

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener.reset();
    }

    public void testSampleConfigDocOutputSchema() {
        // schema from statement
        String epl = EventRepresentationChoice.AVRO.getAnnotationText() + "select 1 as carId, 'abc' as carType from java.lang.Object";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        Schema schema = (Schema) ((AvroSchemaEventType) stmt.getEventType()).getSchema();
        assertEquals("{\"type\":\"record\",\"name\":\"anonymous_1_result_\",\"fields\":[{\"name\":\"carId\",\"type\":\"int\"},{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}", schema.toString());
        stmt.destroy();

        // schema to-string Avro
        Schema schemaTwo = record("MyAvroEvent").fields()
                .requiredInt("carId")
                .name("carType").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .endRecord();
        assertEquals("{\"type\":\"record\",\"name\":\"MyAvroEvent\",\"fields\":[{\"name\":\"carId\",\"type\":\"int\"},{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}", schemaTwo.toString());

        // Define CarLocUpdateEvent event type (example for runtime-configuration interface)
        Schema schemaThree = record("CarLocUpdateEvent").fields()
                .name("carId").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .requiredInt("direction")
                .endRecord();
        ConfigurationEventTypeAvro avroEvent = new ConfigurationEventTypeAvro(schemaThree);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("CarLocUpdateEvent", avroEvent);

        stmt = epService.getEPAdministrator().createEPL("select count(*) from CarLocUpdateEvent(direction = 1)#time(1 min)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        GenericData.Record event = new GenericData.Record(schemaThree);
        event.put("carId", "A123456");
        event.put("direction", 1);
        epService.getEPRuntime().sendEventAvro(event, "CarLocUpdateEvent");
        assertEquals(1L, listener.assertOneGetNewAndReset().get("count(*)"));
    }

    public void testJsonWithSchema() throws IOException {
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
        stmt.addListener(listener);

        String eventOneJson = "{\"name\": \"Jane\", \"favorite_number\": 256, \"favorite_color\": \"red\"}";
        GenericData.Record record = parse(schema, eventOneJson);
        epService.getEPRuntime().sendEventAvro(record, "User");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[] {"Jane", 256, "red"});

        String eventTwoJson = "{\"name\": \"Hans\", \"favorite_number\": -1, \"favorite_color\": \"green\"}";
        record = parse(schema, eventTwoJson);
        epService.getEPRuntime().sendEventAvro(record, "User");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), new Object[] {"Hans", -1, "green"});
    }

    private static GenericData.Record parse(Schema schema, String json) {
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
