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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class EventAvroEventBean implements RegressionExecution {

    public final static Schema INNER_SCHEMA = record("InnerSchema").fields()
        .name("mymap").type().map().values().stringType().noDefault()
        .endRecord();
    public final static Schema RECORD_SCHEMA = record("InnerSchema").fields()
        .name("i").type(INNER_SCHEMA).noDefault()
        .endRecord();

    public void run(RegressionEnvironment env) {
        runAssertionDynamicProp(env);
        runAssertionNestedMap(env);
    }

    private void runAssertionNestedMap(RegressionEnvironment env) {

        env.compileDeploy("@name('s0') select i.mymap('x') as c0 from MyNestedMap");
        env.addListener("s0");

        GenericData.Record inner = new GenericData.Record(INNER_SCHEMA);
        inner.put("mymap", Collections.singletonMap("x", "y"));
        GenericData.Record record = new GenericData.Record(RECORD_SCHEMA);
        record.put("i", inner);
        env.sendEventAvro(record, "MyNestedMap");
        assertEquals("y", env.listener("s0").assertOneGetNewAndReset().get("c0"));

        env.undeployAll();
    }

    private void runAssertionDynamicProp(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType("create avro schema MyEvent()", path);

        env.compileDeploy("@name('s0') select * from MyEvent", path).addListener("s0");

        Schema schema = ((AvroEventType) env.statement("s0").getEventType()).getSchemaAvro();
        env.sendEventAvro(new GenericData.Record(schema), "MyEvent");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();

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
        env.sendEventAvro(record, "MyEvent");
        event = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("X", event.get("a?.b"));

        env.undeployAll();
    }
}
