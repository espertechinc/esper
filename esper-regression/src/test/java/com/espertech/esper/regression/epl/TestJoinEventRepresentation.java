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

package com.espertech.esper.regression.epl;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Map;
import java.util.HashMap;

import static org.apache.avro.SchemaBuilder.record;

public class TestJoinEventRepresentation extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();

        Map<String, Object> typeInfo = new HashMap<String, Object>();
        typeInfo.put("id", String.class);
        typeInfo.put("p00", int.class);
        config.addEventType("MapS0", typeInfo);
        config.addEventType("MapS1", typeInfo);

        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testJoinEventRepresentations() {
        String eplOne = "select S0.id as S0_id, S1.id as S1_id, S0.p00 as S0_p00, S1.p00 as S1_p00 from S0#keepall as S0, S1#keepall as S1 where S0.id = S1.id";
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertion(eplOne, rep, "S0_id,S1_id,S0_p00,S1_p00");
        }

        String eplTwo = "select * from S0#keepall as S0, S1#keepall as S1 where S0.id = S1.id";
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertion(eplTwo, rep, "S0.id,S1.id,S0.p00,S1.p00");
        }
    }

    private void runAssertion(String epl, EventRepresentationChoice rep, String columnNames)
    {
        if (rep.isMapEvent()) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("id", String.class);
            typeInfo.put("p00", int.class);
            epService.getEPAdministrator().getConfiguration().addEventType("S0", typeInfo);
            epService.getEPAdministrator().getConfiguration().addEventType("S1", typeInfo);
        }
        else if (rep.isObjectArrayEvent()) {
            String[] names = "id,p00".split(",");
            Object[] types = new Object[] {String.class, int.class};
            epService.getEPAdministrator().getConfiguration().addEventType("S0", names, types);
            epService.getEPAdministrator().getConfiguration().addEventType("S1", names, types);
        }
        else if (rep.isAvroEvent()) {
            Schema schema = record("name").fields().requiredString("id").requiredInt("p00").endRecord();
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S0", new ConfigurationEventTypeAvro().setAvroSchema(schema));
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S1", new ConfigurationEventTypeAvro().setAvroSchema(schema));
        }

        listener.reset();
        EPStatement stmt = epService.getEPAdministrator().createEPL(rep.getAnnotationText() + epl);
        stmt.addListener(listener);

        sendRepEvent(rep, "S0", "a", 1);
        assertFalse(listener.isInvoked());
        
        sendRepEvent(rep, "S1", "a", 2);
        EventBean output = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(output, columnNames.split(","), new Object[] {"a", "a", 1, 2});
        assertTrue(rep.matchesClass(output.getUnderlying().getClass()));

        sendRepEvent(rep, "S1", "b", 3);
        sendRepEvent(rep, "S0", "c", 4);
        assertFalse(listener.isInvoked());

        stmt.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("S0", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("S1", true);
    }

    public void testJoinMapEventNotUnique()
    {
        // Test for Esper-122
        String joinStatement = "select S0.id, S1.id, S0.p00, S1.p00 from MapS0#keepall as S0, MapS1#keepall as S1" +
                " where S0.id = S1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++)
        {
            if (i % 2 == 1)
            {
                sendMapEvent("MapS0", "a", 1);
            }
            else
            {
                sendMapEvent("MapS1", "a", 1);
            }
        }
    }

    public void testJoinWrapperEventNotUnique()
    {
        // Test for Esper-122
        epService.getEPAdministrator().createEPL("insert into S0 select 's0' as streamone, * from " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("insert into S1 select 's1' as streamtwo, * from " + SupportBean.class.getName());
        String joinStatement = "select * from S0#keepall as a, S1#keepall as b where a.intBoxed = b.intBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }
    }

    private void sendMapEvent(String name, String id, int p00)
    {
        Map<String, Object> theEvent = new HashMap<String, Object>();
        theEvent.put("id", id);
        theEvent.put("p00", p00);
        epService.getEPRuntime().sendEvent(theEvent, name);
    }

    private void sendRepEvent(EventRepresentationChoice rep, String name, String id, int p00)
    {
        if (rep.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<String, Object>();
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            epService.getEPRuntime().sendEvent(theEvent, name);
        }
        else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {id, p00}, name);
        }
        else if (rep.isAvroEvent()) {
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType(name)).getSchemaAvro();
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            epService.getEPRuntime().sendEventAvro(theEvent, name);
        }
        else {
            fail();
        }
    }
}
