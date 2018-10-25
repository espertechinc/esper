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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.apache.avro.Schema;

import java.nio.ByteBuffer;
import java.util.*;

import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.*;

public class EPLInsertIntoPopulateCreateStreamAvro {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoCompatExisting());
        execs.add(new EPLInsertIntoNewSchema());
        return execs;
    }

    private static class EPLInsertIntoCompatExisting implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') insert into AvroExistingType select 1 as myLong," +
                "{1L, 2L} as myLongArray," +
                EPLInsertIntoPopulateCreateStreamAvro.class.getName() + ".makeByteArray() as myByteArray, " +
                EPLInsertIntoPopulateCreateStreamAvro.class.getName() + ".makeMapStringString() as myMap " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            SupportAvroUtil.avroToJson(event);
            assertEquals(1L, event.get("myLong"));
            EPAssertionUtil.assertEqualsExactOrder(new Long[]{1L, 2L}, ((Collection) event.get("myLongArray")).toArray());
            assertTrue(Arrays.equals(new byte[]{1, 2, 3}, ((ByteBuffer) event.get("myByteArray")).array()));
            assertEquals("{k1=v1}", ((Map) event.get("myMap")).toString());

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNewSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') " + EventRepresentationChoice.AVRO.getAnnotationText() + " select 1 as myInt," +
                "{1L, 2L} as myLongArray," +
                EPLInsertIntoPopulateCreateStreamAvro.class.getName() + ".makeByteArray() as myByteArray, " +
                EPLInsertIntoPopulateCreateStreamAvro.class.getName() + ".makeMapStringString() as myMap " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            String json = SupportAvroUtil.avroToJson(event);
            System.out.println(json);
            assertEquals(1, event.get("myInt"));
            EPAssertionUtil.assertEqualsExactOrder(new Long[]{1L, 2L}, ((Collection) event.get("myLongArray")).toArray());
            assertTrue(Arrays.equals(new byte[]{1, 2, 3}, ((ByteBuffer) event.get("myByteArray")).array()));
            assertEquals("{k1=v1}", ((Map) event.get("myMap")).toString());

            Schema designSchema = record("name").fields()
                .requiredInt("myInt")
                .name("myLongArray").type(array().items(unionOf().nullType().and().longType().endUnion())).noDefault()
                .name("myByteArray").type("bytes").noDefault()
                .name("myMap").type(map().values().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .endRecord();
            Schema assembledSchema = ((AvroEventType) event.getEventType()).getSchemaAvro();
            String compareMsg = SupportAvroUtil.compareSchemas(designSchema, assembledSchema);
            assertNull(compareMsg, compareMsg);

            env.undeployAll();
        }
    }

    public static byte[] makeByteArray() {
        return new byte[]{1, 2, 3};
    }

    public static Map<String, String> makeMapStringString() {
        return Collections.singletonMap("k1", "v1");
    }
}
