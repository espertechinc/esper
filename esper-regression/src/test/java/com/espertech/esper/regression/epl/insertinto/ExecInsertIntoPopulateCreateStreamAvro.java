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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.avro.core.AvroConstant;
import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.*;

public class ExecInsertIntoPopulateCreateStreamAvro implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        runAssertionCompatExisting(epService);
        runAssertionNewSchema(epService);
    }

    private void runAssertionCompatExisting(EPServiceProvider epService) {

        String epl = "insert into AvroExistingType select 1 as myLong," +
                "{1L, 2L} as myLongArray," +
                this.getClass().getName() + ".makeByteArray() as myByteArray, " +
                this.getClass().getName() + ".makeMapStringString() as myMap " +
                "from SupportBean";

        Schema schema = record("name").fields()
                .requiredLong("myLong")
                .name("myLongArray").type(array().items(builder().longType())).noDefault()
                .name("myByteArray").type("bytes").noDefault()
                .name("myMap").type(map().values().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .endRecord();
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("AvroExistingType", new ConfigurationEventTypeAvro(schema));

        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        SupportAvroUtil.avroToJson(event);
        assertEquals(1L, event.get("myLong"));
        EPAssertionUtil.assertEqualsExactOrder(new Long[]{1L, 2L}, ((Collection) event.get("myLongArray")).toArray());
        assertTrue(Arrays.equals(new byte[]{1, 2, 3}, ((ByteBuffer) event.get("myByteArray")).array()));
        assertEquals("{k1=v1}", ((Map) event.get("myMap")).toString());

        statement.destroy();
    }

    private void runAssertionNewSchema(EPServiceProvider epService) {

        String epl = EventRepresentationChoice.AVRO.getAnnotationText() + " select 1 as myInt," +
                "{1L, 2L} as myLongArray," +
                this.getClass().getName() + ".makeByteArray() as myByteArray, " +
                this.getClass().getName() + ".makeMapStringString() as myMap " +
                "from SupportBean";

        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
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

        statement.destroy();
    }

    public static byte[] makeByteArray() {
        return new byte[]{1, 2, 3};
    }

    public static Map<String, String> makeMapStringString() {
        return Collections.singletonMap("k1", "v1");
    }
}
