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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.*;

public class ExecNamedWindowProcessingOrder implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("Event", SupportBean.class);

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionDispatchBackQueue(epService, rep);
        }

        runAssertionOrderedDeleteAndSelect(epService);
    }

    private void runAssertionDispatchBackQueue(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema StartValueEvent as (dummy string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TestForwardEvent as (prop1 string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TestInputEvent as (dummy string)");
        epService.getEPAdministrator().createEPL("insert into TestForwardEvent select'V1' as prop1 from TestInputEvent");

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window NamedWin#unique(prop1) (prop1 string, prop2 string)");

        epService.getEPAdministrator().createEPL("insert into NamedWin select 'V1' as prop1, 'O1' as prop2 from StartValueEvent");

        epService.getEPAdministrator().createEPL("on TestForwardEvent update NamedWin as work set prop2 = 'U1' where work.prop1 = 'V1'");

        String[] fields = "prop1,prop2".split(",");
        String eplSelect = "select irstream prop1, prop2 from NamedWin";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(eplSelect).addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"dummyValue"}, "StartValueEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(new HashMap<String, String>(), "StartValueEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            epService.getEPRuntime().sendEventAvro(new GenericData.Record(record("soemthing").fields().endRecord()), "StartValueEvent");
        } else {
            fail();
        }

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"V1", "O1"});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"dummyValue"}, "TestInputEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(new HashMap<String, String>(), "TestInputEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            epService.getEPRuntime().sendEventAvro(new GenericData.Record(record("soemthing").fields().endRecord()), "TestInputEvent");
        } else {
            fail();
        }

        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"V1", "O1"});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fields, new Object[]{"V1", "U1"});
        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "StartValueEvent,TestForwardEvent,TestInputEvent,NamedWin".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionOrderedDeleteAndSelect(EPServiceProvider epService) {
        String stmtText;
        stmtText = "create window MyWindow#lastevent as select * from Event";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "insert into MyWindow select * from Event";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 7";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 5";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "on MyWindow e insert into ResultStream select e.* from MyWindow";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "select * from ResultStream";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 7));
        assertFalse("E1", listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 8));
        assertEquals("E2", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        assertFalse("E3", listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 6));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));
    }
}
