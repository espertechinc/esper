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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecEPLSelectExprEventBeanAnnotation implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionEventBeanAnnotation(epService, rep);
        }
        runAssertionSubquery(epService);
    }

    private void runAssertionEventBeanAnnotation(EPServiceProvider epService, EventRepresentationChoice rep) {
        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema MyEvent(col1 string)");
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        String eplInsert = "insert into DStream select " +
                "last(*) @eventbean as c0, " +
                "window(*) @eventbean as c1, " +
                "prevwindow(s0) @eventbean as c2 " +
                "from MyEvent#length(2) as s0";
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(eplInsert);
        stmtInsert.addListener(listenerInsert);

        for (String prop : "c0,c1,c2".split(",")) {
            assertFragment(prop, stmtInsert.getEventType(), "MyEvent", prop.equals("c1") || prop.equals("c2"));
        }

        // test consuming statement
        String[] fields = "f0,f1,f2,f3,f4,f5".split(",");
        SupportUpdateListener listenerProps = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select " +
                "c0 as f0, " +
                "c0.col1 as f1, " +
                "c1 as f2, " +
                "c1.lastOf().col1 as f3, " +
                "c1 as f4, " +
                "c1.lastOf().col1 as f5 " +
                "from DStream").addListener(listenerProps);

        Object eventOne = sendEvent(epService, rep, "E1");
        assertTrue(((Map) listenerInsert.assertOneGetNewAndReset().getUnderlying()).get("c0") instanceof EventBean);
        EPAssertionUtil.assertProps(listenerProps.assertOneGetNewAndReset(), fields, new Object[]{eventOne, "E1", new Object[]{eventOne}, "E1", new Object[]{eventOne}, "E1"});

        Object eventTwo = sendEvent(epService, rep, "E2");
        EPAssertionUtil.assertProps(listenerProps.assertOneGetNewAndReset(), fields, new Object[]{eventTwo, "E2", new Object[]{eventOne, eventTwo}, "E2", new Object[]{eventOne, eventTwo}, "E2"});

        // test SODA
        SupportModelHelper.compileCreate(epService, eplInsert);

        // test invalid
        try {
            epService.getEPAdministrator().createEPL("select last(*) @xxx from MyEvent");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Failed to recognize select-expression annotation 'xxx', expected 'eventbean' in text 'last(*) @xxx' [select last(*) @xxx from MyEvent]", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("DStream", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyEvent", false);
    }

    private void runAssertionSubquery(EPServiceProvider epService) {
        // test non-named-window
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(col1 string, col2 string)");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String eplInsert = "insert into DStream select " +
                "(select * from MyEvent#keepall) @eventbean as c0 " +
                "from SupportBean";
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(eplInsert);

        for (String prop : "c0".split(",")) {
            assertFragment(prop, stmtInsert.getEventType(), "MyEvent", true);
        }

        // test consuming statement
        String[] fields = "f0,f1".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select " +
                "c0 as f0, " +
                "c0.lastOf().col1 as f1 " +
                "from DStream").addListener(listener);

        Object[] eventOne = new Object[]{"E1", null};
        epService.getEPRuntime().sendEvent(eventOne, "MyEvent");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean out = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(out, fields, new Object[]{new Object[]{eventOne}, "E1"});

        Object[] eventTwo = new Object[]{"E2", null};
        epService.getEPRuntime().sendEvent(eventTwo, "MyEvent");
        epService.getEPRuntime().sendEvent(new SupportBean());
        out = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(out, fields, new Object[]{new Object[]{eventOne, eventTwo}, "E2"});
    }

    private void assertFragment(String prop, EventType eventType, String fragmentTypeName, boolean indexed) {
        EventPropertyDescriptor desc = eventType.getPropertyDescriptor(prop);
        assertEquals(true, desc.isFragment());
        FragmentEventType fragment = eventType.getFragmentType(prop);
        assertEquals(fragmentTypeName, fragment.getFragmentType().getName());
        assertEquals(false, fragment.isNative());
        assertEquals(indexed, fragment.isIndexed());
    }

    private Object sendEvent(EPServiceProvider epService, EventRepresentationChoice rep, String value) {
        Object eventOne;
        if (rep.isMapEvent()) {
            Map<String, Object> event = Collections.singletonMap("col1", value);
            epService.getEPRuntime().sendEvent(event, "MyEvent");
            eventOne = event;
        } else if (rep.isObjectArrayEvent()) {
            Object[] event = new Object[]{value};
            epService.getEPRuntime().sendEvent(event, "MyEvent");
            eventOne = event;

        } else if (rep.isAvroEvent()) {
            Schema schema = SupportAvroUtil.getAvroSchema(epService, "MyEvent");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("col1", value);
            epService.getEPRuntime().sendEventAvro(event, "MyEvent");
            eventOne = event;
        } else {
            throw new IllegalStateException();
        }
        return eventOne;
    }
}
