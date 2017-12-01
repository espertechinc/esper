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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecInsertIntoPopulateUndStreamSelect implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNamedWindowInheritsMap(epService);
        runAssertionNamedWindowRep(epService);
        runAssertionStreamInsertWWidenOA(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionNamedWindowInheritsMap(EPServiceProvider epService) throws Exception {
        String epl = "create objectarray schema Event();\n" +
                "create objectarray schema ChildEvent(id string, action string) inherits Event;\n" +
                "create objectarray schema Incident(name string, event Event);\n" +
                "@Name('window') create window IncidentWindow#keepall as Incident;\n" +
                "\n" +
                "on ChildEvent e\n" +
                "    merge IncidentWindow w\n" +
                "    where e.id = cast(w.event.id? as string)\n" +
                "    when not matched\n" +
                "        then insert (name, event) select 'ChildIncident', e \n" +
                "            where e.action = 'INSERT'\n" +
                "    when matched\n" +
                "        then update set w.event = e \n" +
                "            where e.action = 'INSERT'\n" +
                "        then delete\n" +
                "            where e.action = 'CLEAR';";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new Object[]{"ID1", "INSERT"}, "ChildEvent");
        EventBean event = epService.getEPAdministrator().getStatement("window").iterator().next();
        Object[] underlying = (Object[]) event.getUnderlying();
        assertEquals("ChildIncident", underlying[0]);
        Object[] underlyingInner = (Object[]) ((EventBean) underlying[1]).getUnderlying();
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"ID1", "INSERT"}, underlyingInner);

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deployed.getDeploymentId());
    }

    private void runAssertionNamedWindowRep(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionNamedWindow(epService, rep);
        }
    }

    private void runAssertionStreamInsertWWidenOA(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionStreamInsertWWidenMap(epService, rep);
        }
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionInvalid(epService, rep);
        }
    }

    private void tryAssertionNamedWindow(EPServiceProvider epService, EventRepresentationChoice rep) {
        if (rep.isMapEvent()) {
            Map<String, Object> typeinfo = new HashMap<>();
            typeinfo.put("myint", int.class);
            typeinfo.put("mystr", String.class);
            epService.getEPAdministrator().getConfiguration().addEventType("A", typeinfo);
            epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema C as (addprop int) inherits A");
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType("A", new String[]{"myint", "mystr"}, new Object[]{int.class, String.class});
            epService.getEPAdministrator().createEPL("create objectarray schema C as (addprop int) inherits A");
        } else if (rep.isAvroEvent()) {
            Schema schemaA = record("A").fields().requiredInt("myint").requiredString("mystr").endRecord();
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("A", new ConfigurationEventTypeAvro().setAvroSchema(schemaA));
            epService.getEPAdministrator().createEPL("create avro schema C as (addprop int) inherits A");
        } else {
            fail();
        }

        epService.getEPAdministrator().createEPL("create window MyWindow#time(5 days) as C");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyWindow");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // select underlying
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow select mya.* from A as mya");
        if (rep.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMap(123, "abc"), "A");
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{123, "abc"}, "A");
        } else if (rep.isAvroEvent()) {
            epService.getEPRuntime().sendEventAvro(makeAvro(epService, 123, "abc"), "A");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{123, "abc", null});
        stmtInsert.destroy();

        // select underlying plus property
        epService.getEPAdministrator().createEPL("insert into MyWindow select mya.*, 1 as addprop from A as mya");
        if (rep.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMap(456, "def"), "A");
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{456, "def"}, "A");
        } else if (rep.isAvroEvent()) {
            epService.getEPRuntime().sendEventAvro(makeAvro(epService, 456, "def"), "A");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{456, "def", 1});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("A", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("C", false);
    }

    private void tryAssertionStreamInsertWWidenMap(EPServiceProvider epService, EventRepresentationChoice rep) {

        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema Src as (myint int, mystr string)");

        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema D1 as (myint int, mystr string, addprop long)");
        String eplOne = "insert into D1 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplOne, "myint,mystr,addprop", new Object[]{123, "abc", 1L});

        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema D2 as (mystr string, myint int, addprop double)");
        String eplTwo = "insert into D2 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplTwo, "myint,mystr,addprop", new Object[]{123, "abc", 1d});

        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema D3 as (mystr string, addprop int)");
        String eplThree = "insert into D3 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplThree, "mystr,addprop", new Object[]{"abc", 1});

        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema D4 as (myint int, mystr string)");
        String eplFour = "insert into D4 select mysrc.* from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplFour, "myint,mystr", new Object[]{123, "abc"});

        String eplFive = "insert into D4 select mysrc.*, 999 as myint, 'xxx' as mystr from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplFive, "myint,mystr", new Object[]{999, "xxx"});
        String eplSix = "insert into D4 select 999 as myint, 'xxx' as mystr, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(epService, rep, eplSix, "myint,mystr", new Object[]{999, "xxx"});

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : Arrays.asList("Src", "D1", "D2", "D3", "D4")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void tryAssertionInvalid(EPServiceProvider epService, EventRepresentationChoice rep) {
        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema Src as (myint int, mystr string)");

        // mismatch in type
        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema E1 as (myint long)");
        String message = !rep.isAvroEvent() ?
                "Error starting statement: Type by name 'E1' in property 'myint' expected class java.lang.Integer but receives class java.lang.Long" :
                "Error starting statement: Type by name 'E1' in property 'myint' expected schema '\"long\"' but received schema '\"int\"'";
        SupportMessageAssertUtil.tryInvalid(epService, "insert into E1 select mysrc.* from Src as mysrc", message);

        // mismatch in column name
        epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema E2 as (someprop long)");
        SupportMessageAssertUtil.tryInvalid(epService, "insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc",
                "Error starting statement: Failed to find column 'otherprop' in target type 'E2' [insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc]");

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : Arrays.asList("Src", "E1", "E2")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void runStreamInsertAssertion(EPServiceProvider epService, EventRepresentationChoice rep, String epl, String fields, Object[] expected) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        if (rep.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMap(123, "abc"), "Src");
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{123, "abc"}, "Src");
        } else if (rep.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "Src"));
            event.put("myint", 123);
            event.put("mystr", "abc");
            epService.getEPRuntime().sendEventAvro(event, "Src");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), expected);
        stmt.destroy();
    }

    private Map<String, Object> makeMap(int myint, String mystr) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("myint", myint);
        event.put("mystr", mystr);
        return event;
    }

    private GenericData.Record makeAvro(EPServiceProvider epService, int myint, String mystr) {
        GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "A"));
        record.put("myint", myint);
        record.put("mystr", mystr);
        return record;
    }
}
