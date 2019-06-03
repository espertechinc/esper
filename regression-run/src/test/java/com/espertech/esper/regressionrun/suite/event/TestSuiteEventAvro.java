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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.regressionlib.suite.event.avro.EventAvroEventBean;
import com.espertech.esper.regressionlib.suite.event.avro.EventAvroJsonWithSchema;
import com.espertech.esper.regressionlib.suite.event.avro.EventAvroSampleConfigDocOutputSchema;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.event.avro.EventAvroSupertypeInsertInto;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.record;

public class TestSuiteEventAvro extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventAvroSampleConfigDocOutputSchema() {
        RegressionRunner.run(session, new EventAvroSampleConfigDocOutputSchema());
    }

    public void testEventAvroJsonWithSchema() {
        RegressionRunner.run(session, new EventAvroJsonWithSchema());
    }

    public void testEventAvroEventBean() {
        RegressionRunner.run(session, new EventAvroEventBean());
    }

    public void testEventAvroSupertypeInsertInto() {
        RegressionRunner.run(session, new EventAvroSupertypeInsertInto());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        String schemaUser =
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
        Schema schema = new Schema.Parser().parse(schemaUser);
        configuration.getCommon().addEventTypeAvro("User", new ConfigurationCommonEventTypeAvro(schema));

        Schema schemaCarLocUpdateEvent = record("CarLocUpdateEvent").fields()
            .name("carId").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .requiredInt("direction")
            .endRecord();
        ConfigurationCommonEventTypeAvro avroEvent = new ConfigurationCommonEventTypeAvro(schemaCarLocUpdateEvent);
        configuration.getCommon().addEventTypeAvro("CarLocUpdateEvent", avroEvent);

        ConfigurationCommonEventTypeAvro avro = new ConfigurationCommonEventTypeAvro(EventAvroEventBean.RECORD_SCHEMA);
        configuration.getCommon().addEventTypeAvro("MyNestedMap", avro);
    }
}
