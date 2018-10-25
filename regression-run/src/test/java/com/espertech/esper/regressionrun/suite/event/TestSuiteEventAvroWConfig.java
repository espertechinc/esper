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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.regressionlib.suite.event.avro.EventAvroHook;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithLocalDateTime;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithZonedDateTime;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;

import static org.apache.avro.SchemaBuilder.record;
import static org.apache.avro.SchemaBuilder.unionOf;

public class TestSuiteEventAvroWConfig extends TestCase {

    public void testEventAvroHook() {
        RegressionSession session = RegressionRunner.session();

        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportEventWithLocalDateTime.class, SupportEventWithZonedDateTime.class}) {
            session.getConfiguration().getCommon().addEventType(clazz);
        }
        session.getConfiguration().getCommon().getEventMeta().getAvroSettings().setTypeRepresentationMapperClass(EventAvroHook.MyTypeRepresentationMapper.class.getName());
        session.getConfiguration().getCommon().getEventMeta().getAvroSettings().setObjectValueTypeWidenerFactoryClass(EventAvroHook.MyObjectValueTypeWidenerFactory.class.getName());

        EventAvroHook.MySupportBeanWidener.supportBeanSchema = record("SupportBeanSchema").fields().requiredString("theString").requiredInt("intPrimitive").endRecord();
        Schema schemaMyEventPopulate = record("MyEventSchema").fields().name("sb").type(EventAvroHook.MySupportBeanWidener.supportBeanSchema).noDefault().endRecord();
        session.getConfiguration().getCommon().addEventTypeAvro("MyEventPopulate", new ConfigurationCommonEventTypeAvro(schemaMyEventPopulate));

        Schema schemaMyEventSchema = record("MyEventSchema").fields().requiredString("isodate").endRecord();
        session.getConfiguration().getCommon().addEventTypeAvro("MyEvent", new ConfigurationCommonEventTypeAvro(schemaMyEventSchema));

        EventAvroHook.MySupportBeanWidener.supportBeanSchema = record("SupportBeanSchema").fields().requiredString("theString").requiredInt("intPrimitive").endRecord();
        Schema schemaMyEventWSchema = record("MyEventSchema").fields().name("sb").type(unionOf().nullType().and().type(EventAvroHook.MySupportBeanWidener.supportBeanSchema).endUnion()).noDefault().endRecord();
        session.getConfiguration().getCommon().addEventTypeAvro("MyEventWSchema", new ConfigurationCommonEventTypeAvro(schemaMyEventWSchema));

        RegressionRunner.run(session, EventAvroHook.executions());

        session.destroy();
    }
}
