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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.*;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriberMRD;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementFormatter;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestInfraOnMerge extends TestCase
{
    private String NEWLINE = System.getProperty("line.separator");

    private EPServiceProviderSPI epService;
    private SupportUpdateListener mergeListener;
    private SupportUpdateListener createListener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        mergeListener = new SupportUpdateListener();
        createListener = new SupportUpdateListener();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_A.class, SupportBean_B.class, SupportBean_S0.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        mergeListener = null;
        createListener = null;
    }

    public void testUpdateNestedEvent() throws Exception {
        runAssertionUpdateNestedEvent(true);
        runAssertionUpdateNestedEvent(false);

        // invalid assignment: wrong event type
        epService.getEPAdministrator().createEPL("create map schema Composite as (c0 int)");
        epService.getEPAdministrator().createEPL("create window AInfra#keepall as (c Composite)");
        epService.getEPAdministrator().createEPL("create map schema SomeOther as (c1 int)");
        epService.getEPAdministrator().createEPL("create map schema MyEvent as (so SomeOther)");

        SupportMessageAssertUtil.tryInvalid(epService, "on MyEvent as me update AInfra set c = me.so",
                "Error starting statement: Invalid assignment to property 'c' event type 'Composite' from event type 'SomeOther' [on MyEvent as me update AInfra set c = me.so]");
    }

    public void testInsertOtherStream() throws Exception {
        runAssertionInsertOtherStream(true);
        runAssertionInsertOtherStream(false);
    }

    private void runAssertionInsertOtherStream(boolean namedWindow) throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionInsertOtherStream(namedWindow, rep);
        }
    }

    public void testUpdateOrderOfFields() throws Exception {
        runAssertionUpdateOrderOfFields(true);
        runAssertionUpdateOrderOfFields(false);
    }

    public void testSubqueryNotMatched() {
        runAssertionSubqueryNotMatched(true);
        runAssertionSubqueryNotMatched(false);
    }

    public void testMultiactionDeleteUpdate() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);

        runAssertionMultiactionDeleteUpdate(true);
        runAssertionMultiactionDeleteUpdate(false);
    }

    public void testOnMergeInsertStream() throws Exception {
        runAssertionOnMergeInsertStream(true);
        runAssertionOnMergeInsertStream(false);
    }

    public void testPatternMultimatch() {
        runAssertionPatternMultimatch(true);
        runAssertionPatternMultimatch(false);
    }

    public void testInnerTypeAndVariable() {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionInnerTypeAndVariable(true, rep);
        }

        runAssertionInnerTypeAndVariable(false, EventRepresentationChoice.MAP);
        runAssertionInnerTypeAndVariable(false, EventRepresentationChoice.ARRAY);
        runAssertionInnerTypeAndVariable(false, EventRepresentationChoice.DEFAULT);
    }

    public void testInvalid() {
        runAssertionInvalid(true);
        runAssertionInvalid(false);
    }

    public void testNoWhereClause() {
        runAssertionNoWhereClause(true);
        runAssertionNoWhereClause(false);
    }

    public void testMultipleInsert() {
        runAssertionMultipleInsert(true);
        runAssertionMultipleInsert(false);
    }

    public void testFlow() throws Exception {
        runAssertionFlow(true);
        runAssertionFlow(false);
    }

    private void runAssertionFlow(boolean namedWindow) throws Exception
    {
        String[] fields = "theString,intPrimitive,intBoxed".split(",");
        String createEPL = namedWindow ?
                "@Name('Window') create window MyMergeInfra#unique(theString) as SupportBean" :
                "@Name('Window') create table MyMergeInfra (theString string primary key, intPrimitive int, intBoxed int)";
        EPStatement createStmt = epService.getEPAdministrator().createEPL(createEPL);
        createStmt.addListener(createListener);

        epService.getEPAdministrator().createEPL("@Name('Insert') insert into MyMergeInfra select theString, intPrimitive, intBoxed from SupportBean(boolPrimitive)");
        epService.getEPAdministrator().createEPL("@Name('Delete') on SupportBean_A delete from MyMergeInfra");

        String epl =  "@Name('Merge') on SupportBean(boolPrimitive=false) as up " +
                "merge MyMergeInfra as mv " +
                "where mv.theString=up.theString " +
                "when matched and up.intPrimitive<0 then " +
                "delete " +
                "when matched and up.intPrimitive=0 then " +
                "update set intPrimitive=0, intBoxed=0 " +
                "when matched then " +
                "update set intPrimitive=up.intPrimitive, intBoxed=up.intBoxed+mv.intBoxed " +
                "when not matched then " +
                "insert select " + (namedWindow ? "*" : "theString, intPrimitive, intBoxed");
        EPStatement merged = epService.getEPAdministrator().createEPL(epl);
        merged.addListener(mergeListener);

        runAssertionFlow(namedWindow, createStmt, fields);

        merged.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        createListener.reset();
        mergeListener.reset();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL().trim());
        merged = epService.getEPAdministrator().create(model);
        assertEquals(merged.getText().trim(), model.toEPL().trim());
        merged.addListener(mergeListener);

        runAssertionFlow(namedWindow, createStmt, fields);

        // test stream wildcard
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        merged.destroy();
        epl =  "on SupportBean(boolPrimitive = false) as up " +
                "merge MyMergeInfra as mv " +
                "where mv.theString = up.theString " +
                "when not matched then " +
                "insert select " + (namedWindow ? "up.*" : "theString, intPrimitive, intBoxed");
        merged = epService.getEPAdministrator().createEPL(epl);
        merged.addListener(mergeListener);

        sendSupportBeanEvent(false, "E99", 2, 3); // insert via merge
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E99", 2, 3}});

        // Test ambiguous columns.
        epService.getEPAdministrator().createEPL("create schema TypeOne (id long, mylong long, mystring long)");
        String eplCreateInfraTwo = namedWindow ?
                "create window MyInfraTwo#unique(id) as select * from TypeOne" :
                "create table MyInfraTwo (id long, mylong long, mystring long)";
        epService.getEPAdministrator().createEPL(eplCreateInfraTwo);

        // The "and not matched" should not complain if "mystring" is ambiguous.
        // The "insert" should not complain as column names have been provided.
        epl =  "on TypeOne as t1 merge MyInfraTwo nm where nm.id = t1.id\n" +
                "  when not matched and mystring = 0 then insert select *\n" +
                "  when not matched then insert (id, mylong, mystring) select 0L, 0L, 0L\n" +
                " ";
        epService.getEPAdministrator().createEPL(epl);

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyInfraTwo,TypeOne,MyMergeInfra".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void runAssertionFlow(boolean namedWindow, EPStatement createStmt, String[] fields) {
        createListener.reset();
        mergeListener.reset();

        sendSupportBeanEvent(true, "E1", 10, 200); // insert via insert-into
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 200});
        }
        else {
            assertFalse(createListener.isInvoked());
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 10, 200}});
        assertFalse(mergeListener.isInvoked());

        sendSupportBeanEvent(false, "E1", 11, 201);    // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNew(), fields, new Object[]{"E1", 11, 401});
            EPAssertionUtil.assertProps(createListener.assertOneGetOld(), fields, new Object[]{"E1", 10, 200});
            createListener.reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNew(), fields, new Object[]{"E1", 11, 401});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOld(), fields, new Object[]{"E1", 10, 200});
        mergeListener.reset();

        sendSupportBeanEvent(false, "E2", 13, 300); // insert via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 13, 300});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E2", 13, 300}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 13, 300});

        sendSupportBeanEvent(false, "E2", 14, 301); // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNew(), fields, new Object[]{"E2", 14, 601});
            EPAssertionUtil.assertProps(createListener.assertOneGetOld(), fields, new Object[]{"E2", 13, 300});
            createListener.reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E2", 14, 601}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNew(), fields, new Object[]{"E2", 14, 601});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOld(), fields, new Object[]{"E2", 13, 300});
        mergeListener.reset();

        sendSupportBeanEvent(false, "E2", 15, 302); // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNew(), fields, new Object[]{"E2", 15, 903});
            EPAssertionUtil.assertProps(createListener.assertOneGetOld(), fields, new Object[]{"E2", 14, 601});
            createListener.reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNew(), fields, new Object[]{"E2", 15, 903});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOld(), fields, new Object[]{"E2", 14, 601});
        mergeListener.reset();

        sendSupportBeanEvent(false, "E3", 40, 400); // insert via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 400});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}, {"E3", 40, 400}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 400});

        sendSupportBeanEvent(false, "E3", 0, 1000); // reset E3 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetNew(), fields, new Object[]{"E3", 0, 0});
            EPAssertionUtil.assertProps(createListener.assertOneGetOld(), fields, new Object[]{"E3", 40, 400});
            createListener.reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}, {"E3", 0, 0}});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNew(), fields, new Object[]{"E3", 0, 0});
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOld(), fields, new Object[]{"E3", 40, 400});
        mergeListener.reset();

        sendSupportBeanEvent(false, "E2", -1, 1000); // delete E2 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetOldAndReset(), fields, new Object[]{"E2", 15, 903});
        }
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOldAndReset(), fields, new Object[]{"E2", 15, 903});
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E1", 11, 401}, {"E3", 0, 0}});

        sendSupportBeanEvent(false, "E1", -1, 1000); // delete E1 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(createListener.assertOneGetOldAndReset(), fields, new Object[]{"E1", 11, 401});
            createListener.reset();
        }
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOldAndReset(), fields, new Object[]{"E1", 11, 401});
        EPAssertionUtil.assertPropsPerRowAnyOrder(createStmt.iterator(), fields, new Object[][]{{"E3", 0, 0}});
    }

    private void sendSupportBeanEvent(boolean boolPrimitive, String theString, int intPrimitive, Integer intBoxed) {
        SupportBean theEvent = new SupportBean(theString, intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setBoolPrimitive(boolPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void runAssertionMultipleInsert(boolean namedWindow) {

        String[] fields = "col1,col2".split(",");
        epService.getEPAdministrator().createEPL("create schema MyEvent as (in1 string, in2 int)");
        epService.getEPAdministrator().createEPL("create schema MySchema as (col1 string, col2 int)");
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as MySchema" :
                "create table MyInfra (col1 string primary key, col2 int)";
        epService.getEPAdministrator().createEPL(eplCreate);

        String epl =  "on MyEvent " +
                "merge MyInfra " +
                "where col1=in1 " +
                "when not matched and in1 like \"A%\" then " +
                "insert(col1, col2) select in1, in2 " +
                "when not matched and in1 like \"B%\" then " +
                "insert select in1 as col1, in2 as col2 " +
                "when not matched and in1 like \"C%\" then " +
                "insert select \"Z\" as col1, -1 as col2 " +
                "when not matched and in1 like \"D%\" then " +
                "insert select \"x\"||in1||\"x\" as col1, in2*-1 as col2 ";
        EPStatement merged = epService.getEPAdministrator().createEPL(epl);
        merged.addListener(mergeListener);

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "E1", 0);
        assertFalse(mergeListener.isInvoked());

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "A1", 1);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"A1", 1});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "B1", 2);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"B1", 2});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "C1", 3);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"Z", -1});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "D1", 4);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"xD1x", -4});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "B1", 2);
        assertFalse(mergeListener.isInvoked());

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl.trim(), model.toEPL().trim());
        merged = epService.getEPAdministrator().create(model);
        assertEquals(merged.getText().trim(), model.toEPL().trim());

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyEvent,MySchema,MyInfra".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void runAssertionNoWhereClause(boolean namedWindow) 
    {
        String[] fields = "col1,col2".split(",");
        epService.getEPAdministrator().createEPL("create schema MyEvent as (in1 string, in2 int)");
        epService.getEPAdministrator().createEPL("create schema MySchema as (col1 string, col2 int)");
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as MySchema" :
                "create table MyInfra (col1 string, col2 int)";
        EPStatement namedWindowStmt = epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyInfra");

        String epl =  "on MyEvent me " +
                "merge MyInfra mw " +
                "when not matched and me.in1 like \"A%\" then " +
                "insert(col1, col2) select me.in1, me.in2 " +
                "when not matched and me.in1 like \"B%\" then " +
                "insert select me.in1 as col1, me.in2 as col2 " +
                "when matched and me.in1 like \"C%\" then " +
                "update set col1='Z', col2=-1 " +
                "when not matched then " +
                "insert select \"x\" || me.in1 || \"x\" as col1, me.in2 * -1 as col2 ";
        epService.getEPAdministrator().createEPL(epl);

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "E1", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"xE1x", -2}});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "A1", 3);   // matched : no where clause
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"xE1x", -2}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("Ax1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, null);

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "A1", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A1", 4}});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "B1", 5);   // matched : no where clause
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A1", 4}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("Ax1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, null);

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "B1", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"B1", 5}});

        sendMyEvent(EventRepresentationChoice.getEngineDefault(epService), "C", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"Z", -1}});

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyEvent,MySchema,MyInfra".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void runAssertionInvalid(boolean namedWindow) {
        String epl;
        String eplCreateMergeInfra = namedWindow ?
                "create window MergeInfra#unique(theString) as SupportBean" :
                "create table MergeInfra as (theString string, intPrimitive int, boolPrimitive bool)";
        epService.getEPAdministrator().createEPL(eplCreateMergeInfra);
        epService.getEPAdministrator().createEPL("create schema ABCSchema as (val int)");
        String eplCreateABCInfra = namedWindow ?
                "create window ABCInfra#keepall as ABCSchema" :
                "create table ABCInfra (val int)";
        epService.getEPAdministrator().createEPL(eplCreateABCInfra);

        epl = "on SupportBean_A merge MergeInfra as windowevent where id = theString when not matched and exists(select * from MergeInfra mw where mw.theString = windowevent.theString) is not null then insert into ABC select '1'";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: On-Merge not-matched filter expression may not use properties that are provided by the named window event [on SupportBean_A merge MergeInfra as windowevent where id = theString when not matched and exists(select * from MergeInfra mw where mw.theString = windowevent.theString) is not null then insert into ABC select '1']");

        epl = "on SupportBean_A as up merge ABCInfra as mv when not matched then insert (col) select 1";
        if (namedWindow) {
            SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Validation failed in when-not-matched (clause 1): Event type named 'ABCInfra' has already been declared with differing column name or type information: The property 'val' is not provided but required [on SupportBean_A as up merge ABCInfra as mv when not matched then insert (col) select 1]");
        }
        else {
            SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Validation failed in when-not-matched (clause 1): Column 'col' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [");
        }

        epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then update set intPrimitive = 1";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Incorrect syntax near 'update' (a reserved keyword) expecting 'insert' but found 'update' at line 1 column 9");

        if (namedWindow) {
            epl = "on SupportBean_A as up merge MergeInfra as mv where mv.theString=id when matched then insert select *";
            SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Validation failed in when-not-matched (clause 1): Expression-returned event type 'SupportBean_A' with underlying type '" + SupportBean_A.class.getName() + "' cannot be converted to target event type 'MergeInfra' with underlying type '" + SupportBean.class.getName() + "' [on SupportBean_A as up merge MergeInfra as mv where mv.theString=id when matched then insert select *]");
        }

        epl = "on SupportBean as up merge MergeInfra as mv";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Unexpected end-of-input at line 1 column 4");

        epl = "on SupportBean as up merge MergeInfra as mv where a=b when matched";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Incorrect syntax near end-of-input ('matched' is a reserved keyword) expecting 'then' but found end-of-input at line 1 column 66 [");

        epl = "on SupportBean as up merge MergeInfra as mv where a=b when matched and then delete";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Incorrect syntax near 'then' (a reserved keyword) at line 1 column 71 [on SupportBean as up merge MergeInfra as mv where a=b when matched and then delete]");

        epl = "on SupportBean as up merge MergeInfra as mv where boolPrimitive=true when not matched then insert select *";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate where-clause expression 'boolPrimitive=true': Property named 'boolPrimitive' is ambiguous as is valid for more then one stream [on SupportBean as up merge MergeInfra as mv where boolPrimitive=true when not matched then insert select *]");

        epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select intPrimitive";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream [on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select intPrimitive]");

        epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select * where theString = 'A'";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate match where-clause expression 'theString=\"A\"': Property named 'theString' is not valid in any stream [on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select * where theString = 'A']");

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "ABCSchema,ABCInfra,MergeInfra".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, false);
        }
    }

    private void runAssertionInnerTypeAndVariable(boolean namedWindow, EventRepresentationChoice eventRepresentationEnum) {

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyInnerSchema(in1 string, in2 int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventSchema(col1 string, col2 MyInnerSchema)");
        String eplCreate = namedWindow ?
                eventRepresentationEnum.getAnnotationText() + " create window MyInfra#keepall as (c1 string, c2 MyInnerSchema)" :
                eventRepresentationEnum.getAnnotationText() + " create table MyInfra as (c1 string primary key, c2 MyInnerSchema)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("create variable boolean myvar");

        String epl =  "on MyEventSchema me " +
                "merge MyInfra mw " +
                "where me.col1 = mw.c1 " +
                " when not matched and myvar then " +
                "  insert select col1 as c1, col2 as c2 " +
                " when not matched and myvar = false then " +
                "  insert select 'A' as c1, null as c2 " +
                " when not matched and myvar is null then " +
                "  insert select 'B' as c1, me.col2 as c2 " +
                " when matched then " +
                "  delete";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(mergeListener);
        String[] fields = "c1,c2.in1,c2.in2".split(",");

        sendMyInnerSchemaEvent(eventRepresentationEnum, "X1", "Y1", 10);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"B", "Y1", 10});

        sendMyInnerSchemaEvent(eventRepresentationEnum, "B", "0", 0);    // delete
        EPAssertionUtil.assertProps(mergeListener.assertOneGetOldAndReset(), fields, new Object[]{"B", "Y1", 10});

        epService.getEPRuntime().setVariableValue("myvar", true);
        sendMyInnerSchemaEvent(eventRepresentationEnum, "X2", "Y2", 11);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"X2", "Y2", 11});

        epService.getEPRuntime().setVariableValue("myvar", false);
        sendMyInnerSchemaEvent(eventRepresentationEnum, "X3", "Y3", 12);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[]{"A", null, null});

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL(epl);
        SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
        stmt.setSubscriber(subscriber);
        epService.getEPRuntime().setVariableValue("myvar", true);

        sendMyInnerSchemaEvent(eventRepresentationEnum, "X4", "Y4", 11);
        Object[][] result = subscriber.getInsertStreamList().get(0);
        if (eventRepresentationEnum.isObjectArrayEvent() || !namedWindow) {
            Object[] row = (Object[]) result[0][0];
            assertEquals("X4", row[0]);
            EventBean theEvent = (EventBean) row[1];
            assertEquals("Y4", theEvent.get("in1"));
        }
        else if (eventRepresentationEnum.isMapEvent()){
            Map map = (Map) result[0][0];
            assertEquals("X4", map.get("c1"));
            EventBean theEvent = (EventBean) map.get("c2");
            assertEquals("Y4", theEvent.get("in1"));
        }
        else if (eventRepresentationEnum.isAvroEvent()){
            GenericData.Record avro = (GenericData.Record) result[0][0];
            assertEquals("X4", avro.get("c1"));
            GenericData.Record theEvent = (GenericData.Record) avro.get("c2");
            assertEquals("Y4", theEvent.get("in1"));
        }

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyInfra,MyEventSchema,MyInnerSchema,MyInfra,table_MyInfra__internal,table_MyInfra__public".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionPatternMultimatch(boolean namedWindow) {
        String[] fields = "c1,c2".split(",");
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as (c1 string, c2 string)" :
                "create table MyInfra as (c1 string primary key, c2 string primary key)";
        EPStatement namedWindowStmt = epService.getEPAdministrator().createEPL(eplCreate);

        String epl =  "on pattern[every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%', intPrimitive = a.intPrimitive)] me " +
                "merge MyInfra mw " +
                "where me.a.theString = mw.c1 and me.b.theString = mw.c2 " +
                "when not matched then " +
                "insert select me.a.theString as c1, me.b.theString as c2 ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(mergeListener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}, {"A3", "B2"}, {"A4", "B2"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionOnMergeInsertStream(boolean namedWindow) throws Exception {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        SupportUpdateListener listenerFour = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);

        epService.getEPAdministrator().createEPL("create schema WinSchema as (v1 string, v2 int)");

        String eplCreate = namedWindow ?
                "create window Win#keepall as WinSchema " :
                "create table Win as (v1 string primary key, v2 int)";
        EPStatement nmStmt = epService.getEPAdministrator().createEPL(eplCreate);
        String epl = "on SupportBean_ST0 as st0 merge Win as win where win.v1=st0.key0 " +
                "when not matched " +
                "then insert into StreamOne select * " +
                "then insert into StreamTwo select st0.id as id, st0.key0 as key0 " +
                "then insert into StreamThree(id, key0) select st0.id, st0.key0 " +
                "then insert into StreamFour select id, key0 where key0=\"K2\" " +
                "then insert into Win select key0 as v1, p00 as v2";
        epService.getEPAdministrator().createEPL(epl);

        epService.getEPAdministrator().createEPL("select * from StreamOne").addListener(listenerOne);
        epService.getEPAdministrator().createEPL("select * from StreamTwo").addListener(listenerTwo);
        epService.getEPAdministrator().createEPL("select * from StreamThree").addListener(listenerThree);
        epService.getEPAdministrator().createEPL("select * from StreamFour").addListener(listenerFour);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID1", "K1", 1));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
        EPAssertionUtil.assertProps(listenerThree.assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
        assertFalse(listenerFour.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ID1", "K2", 2));
        EPAssertionUtil.assertProps(listenerFour.assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K2"});

        EPAssertionUtil.assertPropsPerRow(nmStmt.iterator(), "v1,v2".split(","), new Object[][]{{"K1", 1}, {"K2", 2}});

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl.trim(), model.toEPL().trim());
        EPStatement merged = epService.getEPAdministrator().create(model);
        assertEquals(merged.getText().trim(), model.toEPL().trim());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("Win", false);
    }

    private void runAssertionMultiactionDeleteUpdate(boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window Win#keepall as SupportBean" :
                "create table Win (theString string primary key, intPrimitive int)";
        EPStatement nmStmt = epService.getEPAdministrator().createEPL(eplCreate);

        epService.getEPAdministrator().createEPL("insert into Win select theString, intPrimitive from SupportBean");
        String epl = "on SupportBean_ST0 as st0 merge Win as win where st0.key0=win.theString " +
                "when matched " +
                "then delete where intPrimitive<0 " +
                "then update set intPrimitive=st0.p00 where intPrimitive=3000 or p00=3000 " +
                "then update set intPrimitive=999 where intPrimitive=1000 " +
                "then delete where intPrimitive=1000 " +
                "then update set intPrimitive=1999 where intPrimitive=2000 " +
                "then delete where intPrimitive=2000 ";
        String eplFormatted = "on SupportBean_ST0 as st0" + NEWLINE +
                "merge Win as win" + NEWLINE +
                "where st0.key0=win.theString" + NEWLINE +
                "when matched" + NEWLINE +
                "then delete where intPrimitive<0" + NEWLINE +
                "then update set intPrimitive=st0.p00 where intPrimitive=3000 or p00=3000" + NEWLINE +
                "then update set intPrimitive=999 where intPrimitive=1000" + NEWLINE +
                "then delete where intPrimitive=1000" + NEWLINE +
                "then update set intPrimitive=1999 where intPrimitive=2000" + NEWLINE +
                "then delete where intPrimitive=2000";
        epService.getEPAdministrator().createEPL(epl);
        String[] fields = "theString,intPrimitive".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E1", 0));
        EPAssertionUtil.assertPropsPerRow(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", -1));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E2", 0));
        EPAssertionUtil.assertPropsPerRow(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3000));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E3", 3));
        EPAssertionUtil.assertPropsPerRow(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E4", 3000));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1000));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E5", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}, {"E5", 999}});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 2000));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "E6", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(nmStmt.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}, {"E5", 999}, {"E6", 1999}});

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl.trim(), model.toEPL().trim());
        assertEquals(eplFormatted.trim(), model.toEPL(new EPStatementFormatter(true)));
        EPStatement merged = epService.getEPAdministrator().create(model);
        assertEquals(merged.getText().trim(), model.toEPL().trim());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("Win", false);
    }

    private void runAssertionSubqueryNotMatched(boolean namedWindow) {
        String eplCreateOne = namedWindow ?
                "create window InfraOne#unique(string) (string string, intPrimitive int)" :
                "create table InfraOne (string string primary key, intPrimitive int)";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(eplCreateOne);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        String eplCreateTwo = namedWindow ?
                "create window InfraTwo#unique(val0) (val0 string, val1 int)" :
                "create table InfraTwo (val0 string primary key, val1 int primary key)";
        epService.getEPAdministrator().createEPL(eplCreateTwo);
        epService.getEPAdministrator().createEPL("insert into InfraTwo select 'W2' as val0, id as val1 from SupportBean_S0");

        String epl = "on SupportBean sb merge InfraOne w1 " +
                "where sb.theString = w1.string " +
                "when not matched then insert select 'Y' as string, (select val1 from InfraTwo as w2 where w2.val0 = sb.theString) as intPrimitive";
        epService.getEPAdministrator().createEPL(epl);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(50));  // InfraTwo now has a row {W2, 1}
        epService.getEPRuntime().sendEvent(new SupportBean("W2", 1));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "string,intPrimitive".split(","), new Object[][]{{"Y", 50}});

        if (namedWindow) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(51));  // InfraTwo now has a row {W2, 1}
            epService.getEPRuntime().sendEvent(new SupportBean("W2", 2));
            EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "string,intPrimitive".split(","), new Object[][]{{"Y", 51}});
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("InfraOne", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("InfraTwo", false);
    }

    private void runAssertionUpdateOrderOfFields(boolean namedWindow) throws Exception {
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra(theString string primary key, intPrimitive int, intBoxed int, doublePrimitive double)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive, intBoxed, doublePrimitive from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as sb " +
                "merge MyInfra as mywin where mywin.theString = sb.p00 when matched then " +
                "update set intPrimitive=id, intBoxed=mywin.intPrimitive, doublePrimitive=initial.intPrimitive");
        stmt.addListener(mergeListener);
        String[] fields = "intPrimitive,intBoxed,doublePrimitive".split(",");

        epService.getEPRuntime().sendEvent(makeSupportBean("E1", 1, 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "E1"));
        EPAssertionUtil.assertProps(mergeListener.getAndResetLastNewData()[0], fields, new Object[]{5, 5, 1.0});

        epService.getEPRuntime().sendEvent(makeSupportBean("E2", 10, 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "E2"));
        EPAssertionUtil.assertProps(mergeListener.getAndResetLastNewData()[0], fields, new Object[]{6, 6, 10.0});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(7, "E1"));
        EPAssertionUtil.assertProps(mergeListener.getAndResetLastNewData()[0], fields, new Object[]{7, 7, 5.0});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionInsertOtherStream(boolean namedWindow, EventRepresentationChoice eventRepresentationEnum) throws Exception {
        String epl = eventRepresentationEnum.getAnnotationText() + " create schema MyEvent as (name string, value double);\n" +
                (namedWindow ? 
                    eventRepresentationEnum.getAnnotationText() + " create window MyInfra#unique(name) as MyEvent;\n":
                    "create table MyInfra (name string primary key, value double primary key);\n"
                ) +
                "insert into MyInfra select * from MyEvent;\n" +
                eventRepresentationEnum.getAnnotationText() + " create schema InputEvent as (col1 string, col2 double);\n" +
                "\n" +
                "on MyEvent as eme\n" +
                "  merge MyInfra as MyInfra where MyInfra.name = eme.name\n" +
                "   when matched then\n" +
                "      insert into OtherStreamOne select eme.name as event_name, MyInfra.value as status\n" +
                "   when not matched then\n" +
                "      insert into OtherStreamOne select eme.name as event_name, 0d as status\n" +
                ";";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl, null, null, null);
        epService.getEPAdministrator().createEPL("select * from OtherStreamOne").addListener(mergeListener);

        makeSendNameValueEvent(epService, eventRepresentationEnum, "MyEvent", "name1", 10d);
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", namedWindow ? 0d : 10d});

        // for named windows we can send same-value keys now
        if (namedWindow) {
            makeSendNameValueEvent(epService, eventRepresentationEnum, "MyEvent", "name1", 11d);
            EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", 10d});

            makeSendNameValueEvent(epService, eventRepresentationEnum, "MyEvent", "name1", 12d);
            EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", 11d});
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyEvent", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", true);
    }

    private void makeSendNameValueEvent(EPServiceProvider engine, EventRepresentationChoice eventRepresentationEnum, String typeName, String name, double value) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            engine.getEPRuntime().sendEvent(new Object[] {name, value}, typeName);
        }
        else if (eventRepresentationEnum.isMapEvent()){
            Map<String, Object> theEvent = new HashMap<String, Object>();
            theEvent.put("name", name);
            theEvent.put("value", value);
            engine.getEPRuntime().sendEvent(theEvent, typeName);
        }
        else if (eventRepresentationEnum.isAvroEvent()){
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType(typeName)).getSchemaAvro();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("name", name);
            record.put("value", value);
            engine.getEPRuntime().sendEventAvro(record, typeName);
        }
        else {
            fail();
        }
    }

    private void runAssertionUpdateNestedEvent(boolean namedWindow) throws Exception {
        runUpdateNestedEvent(namedWindow, "map");
        runUpdateNestedEvent(namedWindow, "objectarray");
    }

    private void runUpdateNestedEvent(boolean namedWindow, String metaType) throws Exception {
        String eplTypes =
                "create " + metaType + " schema Composite as (c0 int);\n" +
                "create " + metaType + " schema AInfraType as (k string, cflat Composite, carr Composite[]);\n" +
                (namedWindow ?
                        "create window AInfra#lastevent as AInfraType;\n":
                        "create table AInfra (k string, cflat Composite, carr Composite[]);\n") +
                "insert into AInfra select theString as k, null as cflat, null as carr from SupportBean;\n" +
                "create " + metaType + " schema MyEvent as (cf Composite, ca Composite[]);\n" +
                "on MyEvent e merge AInfra when matched then update set cflat = e.cf, carr = e.ca";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplTypes);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        if (metaType.equals("map")) {
            epService.getEPRuntime().sendEvent(makeNestedMapEvent(), "MyEvent");
        }
        else {
            epService.getEPRuntime().sendEvent(makeNestedOAEvent(), "MyEvent");
        }

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select cflat.c0 as cf0, carr[0].c0 as ca0, carr[1].c0 as ca1 from AInfra");
        EPAssertionUtil.assertProps(result.getArray()[0], "cf0,ca0,ca1".split(","), new Object[]{1, 1, 2});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private static Map<String, Object> makeNestedMapEvent() {
        Map<String, Object> cf1 = Collections.<String, Object>singletonMap("c0", 1);
        Map<String, Object> cf2 = Collections.<String, Object>singletonMap("c0", 2);
        Map<String, Object> myEvent = new HashMap<String, Object>();
        myEvent.put("cf", cf1);
        myEvent.put("ca", new Map[] {cf1, cf2});
        return myEvent;
    }

    private static Object[] makeNestedOAEvent() {
        Object[] cf1 = new Object[] {1};
        Object[] cf2 = new Object[] {2};
        return new Object[] {cf1, new Object[] {cf1, cf2}};
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private void sendMyInnerSchemaEvent(EventRepresentationChoice eventRepresentationEnum, String col1, String col2in1, int col2in2) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {col1, new Object[] {col2in1, col2in2}}, "MyEventSchema");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> inner = new HashMap<String, Object>();
            inner.put("in1", col2in1);
            inner.put("in2", col2in2);
            Map<String, Object> theEvent = new HashMap<String, Object>();
            theEvent.put("col1", col1);
            theEvent.put("col2", inner);
            epService.getEPRuntime().sendEvent(theEvent, "MyEventSchema");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType("MyEventSchema")).getSchemaAvro();
            Schema innerSchema = schema.getField("col2").schema();
            GenericData.Record innerRecord = new GenericData.Record(innerSchema);
            innerRecord.put("in1", col2in1);
            innerRecord.put("in2", col2in2);
            GenericData.Record record = new GenericData.Record(schema);
            record.put("col1", col1);
            record.put("col2", innerRecord);
            epService.getEPRuntime().sendEventAvro(record, "MyEventSchema");
        }
        else {
            fail();
        }
    }

    private void sendMyEvent(EventRepresentationChoice eventRepresentationEnum, String in1, int in2) {
        Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("in1", in1);
        theEvent.put("in2", in2);
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent.values().toArray(), "MyEvent");
        }
        else {
            epService.getEPRuntime().sendEvent(theEvent, "MyEvent");
        }
    }
}
