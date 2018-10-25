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
import com.espertech.esper.common.client.hook.type.ObjectValueTypeWidenerFactory;
import com.espertech.esper.common.client.hook.type.ObjectValueTypeWidenerFactoryContext;
import com.espertech.esper.common.client.hook.type.TypeRepresentationMapper;
import com.espertech.esper.common.client.hook.type.TypeRepresentationMapperContext;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithLocalDateTime;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static org.apache.avro.SchemaBuilder.builder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventAvroHook {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventAvroHookSimpleWriteablePropertyCoerce());
        execs.add(new EventAvroHookSchemaFromClass());
        execs.add(new EventAvroHookPopulate());
        execs.add(new EventAvroHookNamedWindowPropertyAssignment());
        return execs;
    }

    /**
     * Writeable-property tests: when a simple writable property needs to be converted
     */
    private static class EventAvroHookSimpleWriteablePropertyCoerce implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // invalid without explicit conversion
            SupportMessageAssertUtil.tryInvalidCompile(env, "insert into MyEvent(isodate) select zdt from SupportEventWithZonedDateTime",
                "Invalid assignment of column 'isodate' of type 'java.time.ZonedDateTime' to event property 'isodate' typed as 'java.lang.CharSequence', column and parameter types mismatch");

            // with hook
            env.compileDeploy("@name('s0') insert into MyEvent(isodate) select ldt from SupportEventWithLocalDateTime").addListener("s0");

            LocalDateTime now = LocalDateTime.now();
            env.sendEventBean(new SupportEventWithLocalDateTime(now));
            assertEquals(DateTimeFormatter.ISO_DATE_TIME.format(now), env.listener("s0").assertOneGetNewAndReset().get("isodate"));

            env.undeployAll();
        }
    }

    /**
     * Schema-from-Class
     */
    private static class EventAvroHookSchemaFromClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') " + EventRepresentationChoice.AVRO.getAnnotationText() + "insert into MyEventOut select " + EventAvroHook.class.getName() + ".makeLocalDateTime() as isodate from SupportBean as e1";
            env.compileDeploy(epl).addListener("s0");

            Schema schema = SupportAvroUtil.getAvroSchema(env.statement("s0").getEventType());
            assertEquals("{\"type\":\"record\",\"name\":\"MyEventOut\",\"fields\":[{\"name\":\"isodate\",\"type\":\"string\"}]}", schema.toString());

            env.sendEventBean(new SupportBean("E1", 10));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            SupportAvroUtil.avroToJson(event);
            assertTrue(event.get("isodate").toString().length() > 10);

            env.undeployAll();
        }
    }

    /**
     * Mapping of Class to GenericRecord
     * }
     */
    private static class EventAvroHookPopulate implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') insert into MyEventPopulate(sb) select " + EventAvroHook.class.getName() + ".makeSupportBean() from SupportBean_S0 as e1";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(10));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("{\"sb\":{\"theString\":\"E1\",\"intPrimitive\":10}}", SupportAvroUtil.avroToJson(event));

            env.undeployAll();
        }
    }

    private static class EventAvroHookNamedWindowPropertyAssignment implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('NamedWindow') create window MyWindow#keepall as MyEventWSchema", path);
            env.compileDeploy("insert into MyWindow select * from MyEventWSchema", path);
            env.compileDeploy("on SupportBean thebean update MyWindow set sb = thebean", path);

            Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("MyEventWSchema"));
            GenericData.Record event = new GenericData.Record(schema);
            env.sendEventAvro(event, "MyEventWSchema");
            env.sendEventBean(new SupportBean("E1", 10));

            EventBean eventBean = env.iterator("NamedWindow").next();
            assertEquals("{\"sb\":{\"SupportBeanSchema\":{\"theString\":\"E1\",\"intPrimitive\":10}}}", SupportAvroUtil.avroToJson(eventBean));

            env.undeployAll();
        }
    }

    public static LocalDateTime makeLocalDateTime() {
        return LocalDateTime.now();
    }

    public static SupportBean makeSupportBean() {
        return new SupportBean("E1", 10);
    }

    public static class MyObjectValueTypeWidenerFactory implements ObjectValueTypeWidenerFactory {
        private static ObjectValueTypeWidenerFactoryContext context;

        public TypeWidenerSPI make(ObjectValueTypeWidenerFactoryContext context) {
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

    public static class MyLDTTypeWidener implements TypeWidenerSPI {

        public final static MyLDTTypeWidener INSTANCE = new MyLDTTypeWidener();

        private MyLDTTypeWidener() {
        }

        public Object widen(Object input) {
            LocalDateTime ldt = (LocalDateTime) input;
            return DateTimeFormatter.ISO_DATE_TIME.format(ldt);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return exprDotMethod(enumValue(DateTimeFormatter.class, "ISO_DATE_TIME"), "format", cast(LocalDateTime.class, expression));
        }
    }

    public static class MySupportBeanWidener implements TypeWidenerSPI {

        public static Schema supportBeanSchema;

        public Object widen(Object input) {
            return widenInput(input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return staticMethod(MySupportBeanWidener.class, "widenInput", expression);
        }

        public static Object widenInput(Object input) {
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
