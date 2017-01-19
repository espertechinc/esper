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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactory;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactoryContext;
import com.espertech.esper.client.hook.TypeRepresentationMapper;
import com.espertech.esper.client.hook.TypeRepresentationMapperContext;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.EventRepresentationChoice;
import com.espertech.esper.util.TypeWidener;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.record;
import static org.apache.avro.SchemaBuilder.unionOf;

public class TestAvroEventHook extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getEventMeta().getAvroSettings().setTypeRepresentationMapperClass(MyTypeRepresentationMapper.class.getName());
        configuration.getEngineDefaults().getEventMeta().getAvroSettings().setObjectValueTypeWidenerFactoryClass(MyObjectValueTypeWidenerFactory.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        for (Class clazz : Arrays.asList(SupportBean.class, SupportBean_S0.class, MyEventWithLocalDateTime.class, MyEventWithZonedDateTime.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    /**
     * Writeable-property tests: when a simple writable property needs to be converted
     */
    public void testSimpleWriteablePropertyCoerce() {
        Schema schema = record("MyEventSchema").fields().requiredString("isodate").endRecord();
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("MyEvent", new ConfigurationEventTypeAvro(schema));

        // invalid without explicit conversion
        SupportMessageAssertUtil.tryInvalid(epService, "insert into MyEvent(isodate) select zdt from MyEventWithZonedDateTime",
                "Error starting statement: Invalid assignment of column 'isodate' of type 'java.time.ZonedDateTime' to event property 'isodate' typed as 'java.lang.CharSequence', column and parameter types mismatch");

        // with hook
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into MyEvent(isodate) select ldt from MyEventWithLocalDateTime");
        stmt.addListener(listener);

        LocalDateTime now = LocalDateTime.now();
        epService.getEPRuntime().sendEvent(new MyEventWithLocalDateTime(now));
        assertEquals(DateTimeFormatter.ISO_DATE_TIME.format(now), listener.assertOneGetNewAndReset().get("isodate"));
    }

    /**
     * Schema-from-Class
     */
    public void testSchemaFromClass() {

        String epl = EventRepresentationChoice.AVRO.getAnnotationText() + "insert into MyEvent select " + this.getClass().getName() + ".makeLocalDateTime() as isodate from SupportBean as e1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Schema schema = SupportAvroUtil.getAvroSchema(stmt.getEventType());
        assertEquals("{\"type\":\"record\",\"name\":\"MyEvent\",\"fields\":[{\"name\":\"isodate\",\"type\":\"string\"}]}", schema.toString());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EventBean event = listener.assertOneGetNewAndReset();
        SupportAvroUtil.avroToJson(event);
        assertTrue(event.get("isodate").toString().length() > 10);
    }

    /**
     * Mapping of Class to GenericRecord
     */
    public void testPopulate() {
        MySupportBeanWidener.supportBeanSchema = record("SupportBeanSchema").fields().requiredString("theString").requiredInt("intPrimitive").endRecord();
        Schema schema = record("MyEventSchema").fields().name("sb").type(MySupportBeanWidener.supportBeanSchema).noDefault().endRecord();
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("MyEvent", new ConfigurationEventTypeAvro(schema));

        String epl = "insert into MyEvent(sb) select " + this.getClass().getName() + ".makeSupportBean() from SupportBean_S0 as e1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals("{\"sb\":{\"theString\":\"E1\",\"intPrimitive\":10}}", SupportAvroUtil.avroToJson(event));
    }

    public void testNamedWindowPropertyAssignment() {
        MySupportBeanWidener.supportBeanSchema = record("SupportBeanSchema").fields().requiredString("theString").requiredInt("intPrimitive").endRecord();
        Schema schema = record("MyEventSchema").fields().name("sb").type(unionOf().nullType().and().type(MySupportBeanWidener.supportBeanSchema).endUnion()).noDefault().endRecord();
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("MyEvent", new ConfigurationEventTypeAvro(schema));

        epService.getEPAdministrator().createEPL("@Name('NamedWindow') create window MyWindow#keepall as MyEvent");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from MyEvent");
        epService.getEPAdministrator().createEPL("on SupportBean thebean update MyWindow set sb = thebean");

        GenericData.Record event = new GenericData.Record(schema);
        epService.getEPRuntime().sendEventAvro(event, "MyEvent");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));

        EventBean eventBean = epService.getEPAdministrator().getStatement("NamedWindow").iterator().next();
        assertEquals("{\"sb\":{\"SupportBeanSchema\":{\"theString\":\"E1\",\"intPrimitive\":10}}}", SupportAvroUtil.avroToJson(eventBean));
    }

    public static LocalDateTime makeLocalDateTime() {
        return LocalDateTime.now();
    }

    public static SupportBean makeSupportBean() {
        return new SupportBean("E1", 10);
    }

    public static class MyEventWithLocalDateTime {
        private final LocalDateTime ldt;

        public MyEventWithLocalDateTime(LocalDateTime ldt) {
            this.ldt = ldt;
        }

        public LocalDateTime getLdt() {
            return ldt;
        }
    }

    public static class MyEventWithZonedDateTime {
        private final ZonedDateTime zdt;

        public MyEventWithZonedDateTime(ZonedDateTime zdt) {
            this.zdt = zdt;
        }

        public ZonedDateTime getZdt() {
            return zdt;
        }
    }

    public static class MyObjectValueTypeWidenerFactory implements ObjectValueTypeWidenerFactory {
        private static ObjectValueTypeWidenerFactoryContext context;

        public TypeWidener make(ObjectValueTypeWidenerFactoryContext context) {
            this.context = context;
            if (context.getClazz() == LocalDateTime.class) {
                return MyLDTTypeWidener.INSTANCE;
            }
            if (context.getClazz() == SupportBean.class) {
                return new MySupportBeanWidener();
            }
            return null;
        }

        public static ObjectValueTypeWidenerFactoryContext getContext() {
            return context;
        }
    }

    public static class MyLDTTypeWidener implements TypeWidener {

        public final static MyLDTTypeWidener INSTANCE = new MyLDTTypeWidener();

        private MyLDTTypeWidener() {
        }

        public Object widen(Object input) {
            LocalDateTime ldt = (LocalDateTime) input;
            return DateTimeFormatter.ISO_DATE_TIME.format(ldt);
        }
    }

    public static class MySupportBeanWidener implements TypeWidener {

        public static Schema supportBeanSchema;

        public Object widen(Object input) {
            SupportBean sb = (SupportBean) input;
            GenericData.Record record = new GenericData.Record(supportBeanSchema);
            record.put("theString", sb.getTheString());
            record.put("intPrimitive", sb.getIntPrimitive());
            return record;
        }
    }

    public static class MyTypeRepresentationMapper implements TypeRepresentationMapper {
        public Object map(TypeRepresentationMapperContext context) {
            if (context.getClazz() == LocalDateTime.class) {
                return builder().stringBuilder().endString();
            }
            return null;
        }
    }
}
