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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestInsertIntoPopulateUndStreamSelect extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNamedWindowInheritsMap() throws Exception {
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
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new Object[] {"ID1", "INSERT"}, "ChildEvent");
        EventBean event = epService.getEPAdministrator().getStatement("window").iterator().next();
        Object[] underlying = (Object[]) event.getUnderlying();
        assertEquals("ChildIncident", underlying[0]);
        Object[] underlyingInner = (Object[]) ((EventBean) underlying[1]).getUnderlying();
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {"ID1", "INSERT"}, underlyingInner);
    }

    public void testNamedWindowOA() {
        runAssertionNamedWindow(TypeTested.OA);
    }

    public void testNamedWindowMap() {
        runAssertionNamedWindow(TypeTested.MAP);
    }

    public void testStreamInsertWWidenOA() {
        runAssertionStreamInsertWWidenMap(TypeTested.OA);
    }

    public void testStreamInsertWWidenMap() {
        runAssertionStreamInsertWWidenMap(TypeTested.MAP);
    }

    public void testInvalidOA() {
        runAssertionInvalid(TypeTested.OA);
    }

    public void testInvalidMap() {
        runAssertionInvalid(TypeTested.MAP);
    }

    private void runAssertionNamedWindow(TypeTested typeTested) {
        if (typeTested == TypeTested.MAP) {
            Map<String, Object> typeinfo = new HashMap<String, Object>();
            typeinfo.put("myint", int.class);
            typeinfo.put("mystr", String.class);
            epService.getEPAdministrator().getConfiguration().addEventType("A", typeinfo);
            epService.getEPAdministrator().createEPL("create map schema C as (addprop int) inherits A");
        }
        else if (typeTested == TypeTested.OA) {
            epService.getEPAdministrator().getConfiguration().addEventType("A", new String[]{"myint", "mystr"}, new Object[]{int.class, String.class});
            epService.getEPAdministrator().createEPL("create objectarray schema C as (addprop int) inherits A");
        }

        epService.getEPAdministrator().createEPL("create window MyWindow#time(5 days) as C");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyWindow");
        stmt.addListener(listener);

        // select underlying
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyWindow select mya.* from A as mya");
        if (typeTested == TypeTested.MAP) {
            epService.getEPRuntime().sendEvent(makeMap(123, "abc"), "A");
        }
        else if (typeTested == TypeTested.OA) {
            epService.getEPRuntime().sendEvent(new Object[]{123, "abc"}, "A");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{123, "abc", null});
        stmtInsert.destroy();

        // select underlying plus property
        epService.getEPAdministrator().createEPL("insert into MyWindow select mya.*, 1 as addprop from A as mya");
        if (typeTested == TypeTested.MAP) {
            epService.getEPRuntime().sendEvent(makeMap(456, "def"), "A");
        }
        else if (typeTested == TypeTested.OA) {
            epService.getEPRuntime().sendEvent(new Object[] {456, "def"}, "A");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{456, "def", 1});
    }

    private void runAssertionStreamInsertWWidenMap(TypeTested typeTested) {

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider();
        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema Src as (myint int, mystr string)");

        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema D1 as (myint int, mystr string, addprop long)");
        String eplOne = "insert into D1 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplOne, "myint,mystr,addprop", new Object[]{123, "abc", 1L});

        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema D2 as (mystr string, myint int, addprop double)");
        String eplTwo = "insert into D2 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplTwo, "myint,mystr,addprop", new Object[]{123, "abc", 1d});

        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema D3 as (mystr string, addprop int)");
        String eplThree = "insert into D3 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplThree, "mystr,addprop", new Object[]{"abc", 1});

        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema D4 as (myint int, mystr string)");
        String eplFour = "insert into D4 select mysrc.* from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplFour, "myint,mystr", new Object[]{123, "abc"});

        String eplFive = "insert into D4 select mysrc.*, 999 as myint, 'xxx' as mystr from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplFive, "myint,mystr", new Object[]{999, "xxx"});
        String eplSix = "insert into D4 select 999 as myint, 'xxx' as mystr, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(typeTested, eplSix, "myint,mystr", new Object[]{999, "xxx"});
    }

    public void runAssertionInvalid(TypeTested typeTested) {
        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema Src as (myint int, mystr string)");

        // mismatch in type
        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema E1 as (myint long)");
        tryInvalid("insert into E1 select mysrc.* from Src as mysrc",
                "Error starting statement: Type by name 'E1' in property 'myint' expected class java.lang.Integer but receives class java.lang.Long [insert into E1 select mysrc.* from Src as mysrc]");

        // mismatch in column name
        epService.getEPAdministrator().createEPL("create " + typeTested.getText() + " schema E2 as (someprop long)");
        tryInvalid("insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc",
                "Error starting statement: Failed to find column 'otherprop' in target type 'E2' [insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc]");
    }

    private void runStreamInsertAssertion(TypeTested typeTested, String epl, String fields, Object[] expected) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        if (TypeTested.MAP == typeTested) {
            epService.getEPRuntime().sendEvent(makeMap(123, "abc"), "Src");
        }
        else {
            epService.getEPRuntime().sendEvent(new Object[] {123, "abc"}, "Src");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields.split(","), expected);
        stmt.destroy();
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private Map<String, Object> makeMap(int myint, String mystr) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("myint", myint);
        event.put("mystr", mystr);
        return event;
    }

    private static enum TypeTested {
        MAP("map"),
        OA("objectarray");
        
        private final String text;

        private TypeTested(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}
