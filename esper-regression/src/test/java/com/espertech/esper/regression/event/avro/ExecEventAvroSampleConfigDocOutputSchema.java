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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class ExecEventAvroSampleConfigDocOutputSchema implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
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
}
