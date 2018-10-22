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
package com.espertech.esper.regressionlib.suite.event.avro;

import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class EventAvroSampleConfigDocOutputSchema implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        // schema from statement
        String epl = "@name('s0') " + EventRepresentationChoice.AVRO.getAnnotationText() + "select 1 as carId, 'abc' as carType from SupportBean";
        env.compileDeploy(epl);
        Schema schema = (Schema) ((AvroSchemaEventType) env.statement("s0").getEventType()).getSchema();
        assertEquals("{\"type\":\"record\",\"name\":\"stmt0_out0\",\"fields\":[{\"name\":\"carId\",\"type\":\"int\"},{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}", schema.toString());
        env.undeployAll();

        // schema to-string Avro
        Schema schemaTwo = record("MyAvroEvent").fields()
            .requiredInt("carId")
            .name("carType").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .endRecord();
        assertEquals("{\"type\":\"record\",\"name\":\"MyAvroEvent\",\"fields\":[{\"name\":\"carId\",\"type\":\"int\"},{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}", schemaTwo.toString());
        env.undeployAll();

        env.compileDeploy("@name('s0') select count(*) from CarLocUpdateEvent(direction = 1)#time(1 min)").addListener("s0");
        Schema schemaCarLocUpd = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("CarLocUpdateEvent"));
        GenericData.Record event = new GenericData.Record(schemaCarLocUpd);
        event.put("carId", "A123456");
        event.put("direction", 1);
        env.sendEventAvro(event, "CarLocUpdateEvent");
        assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("count(*)"));

        env.undeployAll();
    }
}
