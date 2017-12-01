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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.avro.core.AvroConstant;
import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.builder;
import static org.apache.avro.SchemaBuilder.record;

public class ExecJoinEventRepresentation implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> typeInfo = new HashMap<String, Object>();
        typeInfo.put("id", String.class);
        typeInfo.put("p00", int.class);
        configuration.addEventType("MapS0", typeInfo);
        configuration.addEventType("MapS1", typeInfo);
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionJoinEventRepresentations(epService);
        runAssertionJoinMapEventNotUnique(epService);
        runAssertionJoinWrapperEventNotUnique(epService);
    }

    private void runAssertionJoinEventRepresentations(EPServiceProvider epService) {
        String eplOne = "select S0.id as S0_id, S1.id as S1_id, S0.p00 as S0_p00, S1.p00 as S1_p00 from S0#keepall as S0, S1#keepall as S1 where S0.id = S1.id";
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryJoinAssertion(epService, eplOne, rep, "S0_id,S1_id,S0_p00,S1_p00");
        }

        String eplTwo = "select * from S0#keepall as S0, S1#keepall as S1 where S0.id = S1.id";
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryJoinAssertion(epService, eplTwo, rep, "S0.id,S1.id,S0.p00,S1.p00");
        }
    }

    private void tryJoinAssertion(EPServiceProvider epService, String epl, EventRepresentationChoice rep, String columnNames) {
        if (rep.isMapEvent()) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("id", String.class);
            typeInfo.put("p00", int.class);
            epService.getEPAdministrator().getConfiguration().addEventType("S0", typeInfo);
            epService.getEPAdministrator().getConfiguration().addEventType("S1", typeInfo);
        } else if (rep.isObjectArrayEvent()) {
            String[] names = "id,p00".split(",");
            Object[] types = new Object[]{String.class, int.class};
            epService.getEPAdministrator().getConfiguration().addEventType("S0", names, types);
            epService.getEPAdministrator().getConfiguration().addEventType("S1", names, types);
        } else if (rep.isAvroEvent()) {
            Schema schema = record("name").fields()
                    .name("id").type(builder().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                    .requiredInt("p00").endRecord();
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S0", new ConfigurationEventTypeAvro().setAvroSchema(schema));
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S1", new ConfigurationEventTypeAvro().setAvroSchema(schema));
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL(rep.getAnnotationText() + epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendRepEvent(epService, rep, "S0", "a", 1);
        assertFalse(listener.isInvoked());

        sendRepEvent(epService, rep, "S1", "a", 2);
        EventBean output = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(output, columnNames.split(","), new Object[]{"a", "a", 1, 2});
        assertTrue(rep.matchesClass(output.getUnderlying().getClass()));

        sendRepEvent(epService, rep, "S1", "b", 3);
        sendRepEvent(epService, rep, "S0", "c", 4);
        assertFalse(listener.isInvoked());

        stmt.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("S0", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("S1", true);
    }

    private void runAssertionJoinMapEventNotUnique(EPServiceProvider epService) {
        // Test for Esper-122
        String joinStatement = "select S0.id, S1.id, S0.p00, S1.p00 from MapS0#keepall as S0, MapS1#keepall as S1" +
                " where S0.id = S1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++) {
            if (i % 2 == 1) {
                sendMapEvent(epService, "MapS0", "a", 1);
            } else {
                sendMapEvent(epService, "MapS1", "a", 1);
            }
        }

        stmt.destroy();
    }

    private void runAssertionJoinWrapperEventNotUnique(EPServiceProvider epService) {
        // Test for Esper-122
        epService.getEPAdministrator().createEPL("insert into S0 select 's0' as streamone, * from " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("insert into S1 select 's1' as streamtwo, * from " + SupportBean.class.getName());
        String joinStatement = "select * from S0#keepall as a, S1#keepall as b where a.intBoxed = b.intBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendMapEvent(EPServiceProvider epService, String name, String id, int p00) {
        Map<String, Object> theEvent = new HashMap<String, Object>();
        theEvent.put("id", id);
        theEvent.put("p00", p00);
        epService.getEPRuntime().sendEvent(theEvent, name);
    }

    private void sendRepEvent(EPServiceProvider epService, EventRepresentationChoice rep, String name, String id, int p00) {
        if (rep.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            epService.getEPRuntime().sendEvent(theEvent, name);
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{id, p00}, name);
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, name));
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            epService.getEPRuntime().sendEventAvro(theEvent, name);
        } else {
            fail();
        }
    }
}
