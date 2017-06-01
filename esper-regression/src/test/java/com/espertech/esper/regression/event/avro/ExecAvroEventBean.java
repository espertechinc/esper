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

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class ExecAvroEventBean implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDynamicProp(epService);
        runNestedMap(epService);
    }

    private void runNestedMap(EPServiceProvider epService) {
        Schema innerSchema = record("InnerSchema").fields()
                .name("mymap").type().map().values().stringType().noDefault()
                .endRecord();
        Schema recordSchema = record("InnerSchema").fields()
                .name("i").type(innerSchema).noDefault()
                .endRecord();
        ConfigurationEventTypeAvro avro = new ConfigurationEventTypeAvro(recordSchema);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("MyNestedMap", avro);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select i.mymap('x') as c0 from MyNestedMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        GenericData.Record inner = new GenericData.Record(innerSchema);
        inner.put("mymap", Collections.singletonMap("x", "y"));
        GenericData.Record record = new GenericData.Record(recordSchema);
        record.put("i", inner);
        epService.getEPRuntime().sendEventAvro(record, "MyNestedMap");
        assertEquals("y", listener.assertOneGetNewAndReset().get("c0"));
    }

    private void runAssertionDynamicProp(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create avro schema MyEvent()");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Schema schema = ((AvroEventType) stmt.getEventType()).getSchemaAvro();
        epService.getEPRuntime().sendEventAvro(new GenericData.Record(schema), "MyEvent");
        EventBean event = listener.assertOneGetNewAndReset();

        assertEquals(null, event.get("a?.b"));

        Schema innerSchema = record("InnerSchema").fields()
                .name("b").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .endRecord();
        GenericData.Record inner = new GenericData.Record(innerSchema);
        inner.put("b", "X");
        Schema recordSchema = record("RecordSchema").fields()
                .name("a").type(innerSchema).noDefault()
                .endRecord();
        GenericData.Record record = new GenericData.Record(recordSchema);
        record.put("a", inner);
        epService.getEPRuntime().sendEventAvro(record, "MyEvent");
        event = listener.assertOneGetNewAndReset();
        assertEquals("X", event.get("a?.b"));
    }
}
