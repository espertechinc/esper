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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.Expressions;
import com.espertech.esper.client.soda.UpdateClause;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanCopyMethod;
import com.espertech.esper.supportregression.bean.SupportBeanErrorTestingOne;
import com.espertech.esper.supportregression.bean.SupportBeanReadOnly;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class TestUpdate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.getEngineDefaults().getExecution().setPrioritized(true);

        ConfigurationEventTypeLegacy legacy = new ConfigurationEventTypeLegacy();
        legacy.setCopyMethod("myCopyMethod");
        config.addEventType("SupportBeanCopyMethod", SupportBeanCopyMethod.class.getName(), legacy);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testFieldUpdateOrder() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addVariable("myvar", Integer.class, 10);

        epService.getEPAdministrator().createEPL("update istream SupportBean " +
                "set intPrimitive=myvar, intBoxed=intPrimitive");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmt.addListener(listener);
        String[] fields = "intPrimitive,intBoxed".split(",");

        epService.getEPRuntime().sendEvent(makeSupportBean("E1", 1, 2));
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fields, new Object[]{10, 1});
    }

    public void testInvalid()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", long.class);
        type.put("p1", long.class);
        type.put("p2", long.class);
        type.put("p3", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanReadOnly", SupportBeanReadOnly.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanErrorTestingOne", SupportBeanErrorTestingOne.class);

        ConfigurationEventTypeXMLDOM configXML = new ConfigurationEventTypeXMLDOM();
        configXML.setRootElementName("MyXMLEvent");
        epService.getEPAdministrator().getConfiguration().addEventType("MyXmlEvent", configXML);

        epService.getEPAdministrator().createEPL("insert into SupportBeanStream select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into SupportBeanStreamTwo select * from pattern[a=SupportBean -> b=SupportBean]");
        epService.getEPAdministrator().createEPL("insert into SupportBeanStreamRO select * from SupportBeanReadOnly");

        tryInvalid("update istream SupportBeanStream set intPrimitive=longPrimitive",
                   "Error starting statement: Invalid assignment of column 'longPrimitive' of type 'long' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [update istream SupportBeanStream set intPrimitive=longPrimitive]");
        tryInvalid("update istream SupportBeanStream set xxx='abc'",
                   "Error starting statement: Property 'xxx' is not available for write access [update istream SupportBeanStream set xxx='abc']");
        tryInvalid("update istream SupportBeanStream set intPrimitive=null",
                   "Error starting statement: Invalid assignment of column 'null' of null type to event property 'intPrimitive' typed as 'int', nullable type mismatch [update istream SupportBeanStream set intPrimitive=null]");
        tryInvalid("update istream SupportBeanStreamTwo set a.intPrimitive=10",
                   "Error starting statement: Property 'a.intPrimitive' is not available for write access [update istream SupportBeanStreamTwo set a.intPrimitive=10]");
        tryInvalid("update istream SupportBeanStreamRO set side='a'",
                   "Error starting statement: Property 'side' is not available for write access [update istream SupportBeanStreamRO set side='a']");
        tryInvalid("update istream SupportBean set longPrimitive=sum(intPrimitive)",
                   "Error starting statement: Aggregation functions may not be used within an update-clause [update istream SupportBean set longPrimitive=sum(intPrimitive)]");
        tryInvalid("update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1",
                   "Error starting statement: Aggregation functions may not be used within an update-clause [update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1]");
        tryInvalid("update istream SupportBean set longPrimitive=prev(1, longPrimitive)",
                   "Error starting statement: Previous function cannot be used in this context [update istream SupportBean set longPrimitive=prev(1, longPrimitive)]");
        tryInvalid("update istream MyXmlEvent set abc=1",
                   "Error starting statement: Property 'abc' is not available for write access [update istream MyXmlEvent set abc=1]");
        tryInvalid("update istream SupportBeanErrorTestingOne set value='1'",
                   "Error starting statement: The update-clause requires the underlying event representation to support copy (via Serializable by default) [update istream SupportBeanErrorTestingOne set value='1']");
        tryInvalid("update istream SupportBean set longPrimitive=(select p0 from MyMapType#lastevent where theString=p3)",
                   "Error starting statement: Failed to plan subquery number 1 querying MyMapType: Failed to validate filter expression 'theString=p3': Property named 'theString' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [update istream SupportBean set longPrimitive=(select p0 from MyMapType#lastevent where theString=p3)]");
        tryInvalid("update istream XYZ.GYH set a=1",
                   "Failed to resolve event type: Event type or class named 'XYZ.GYH' was not found [update istream XYZ.GYH set a=1]");
        tryInvalid("update istream SupportBean set 1",
                    "Error starting statement: Missing property assignment expression in assignment number 0 [update istream SupportBean set 1]");
    }

    public void testInsertIntoWBeanWhere() throws Exception
    {
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyStream select * from SupportBean");
        stmtInsert.addListener(listenerInsert);

        SupportUpdateListener listenerUpdate = new SupportUpdateListener();
        EPStatement stmtUpdOne = epService.getEPAdministrator().createEPL("update istream MyStream set intPrimitive=10, theString='O_' || theString where intPrimitive=1");
        stmtUpdOne.addListener(listenerUpdate);

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyStream");
        stmtSelect.addListener(listener);

        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
        assertFalse(listenerUpdate.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"O_E2", 10});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetOld(), fields, new Object[]{"E2", 1});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNew(), fields, new Object[]{"O_E2", 10});
        listenerUpdate.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});
        assertFalse(listenerUpdate.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"O_E4", 10});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E4", 1});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetOld(), fields, new Object[]{"E4", 1});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNew(), fields, new Object[]{"O_E4", 10});
        listenerUpdate.reset();

        EPStatement stmtUpdTwo = epService.getEPAdministrator().createEPL("update istream MyStream as xyz set intPrimitive=xyz.intPrimitive + 1000 where intPrimitive=2");
        stmtUpdTwo.addListener(listenerUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", 1002});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E5", 2});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetOld(), fields, new Object[]{"E5", 2});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNew(), fields, new Object[]{"E5", 1002});
        listenerUpdate.reset();

        stmtUpdOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", 1});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E6", 1});
        assertFalse(listenerUpdate.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E7", 1002});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E7", 2});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetOld(), fields, new Object[]{"E7", 2});
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNew(), fields, new Object[]{"E7", 1002});
        listenerUpdate.reset();
        assertFalse(stmtUpdTwo.iterator().hasNext());

        stmtUpdTwo.removeAllListeners();

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E8", 1002});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E8", 2});
        assertFalse(listenerUpdate.isInvoked());

        SupportSubscriber subscriber = new SupportSubscriber();
        stmtUpdTwo.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E9", 1002});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E9", 2});
        EPAssertionUtil.assertPropsPOJO(subscriber.getOldDataListFlattened()[0], fields, new Object[]{"E9", 2});
        EPAssertionUtil.assertPropsPOJO(subscriber.getNewDataListFlattened()[0], fields, new Object[]{"E9", 1002});
        subscriber.reset();

        stmtUpdTwo.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E10", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E10", 2});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E10", 2});
        assertFalse(listenerUpdate.isInvoked());

        EPStatement stmtUpdThree = epService.getEPAdministrator().createEPL("update istream MyStream set intPrimitive=intBoxed");
        stmtUpdThree.addListener(listenerUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean("E11", 2));
        EPAssertionUtil.assertProps(listenerUpdate.assertOneGetNew(), fields, new Object[]{"E11", 2});
        listenerUpdate.reset();
    }

    public void testInsertIntoWMapNoWhere() throws Exception
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", long.class);
        type.put("p1", long.class);
        type.put("p2", long.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);

        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyStream select * from MyMapType");
        stmtInsert.addListener(listenerInsert);

        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream MyStream set p0=p1, p1=p0");

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyStream");
        stmtSelect.addListener(listener);

        String[] fields = "p0,p1,p2".split(",");
        epService.getEPRuntime().sendEvent(makeMap("p0", 10, "p1", 1, "p2", 100), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 10, 100});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{10, 1, 100});

        stmtUpd.stop();
        stmtUpd.start();
        
        epService.getEPRuntime().sendEvent(makeMap("p0", 5, "p1", 4, "p2", 101), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, 5, 101});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{5, 4, 101});

        stmtUpd.destroy();

        epService.getEPRuntime().sendEvent(makeMap("p0", 20, "p1", 0, "p2", 102), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 0, 102});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{20, 0, 102});
    }

    public void testFieldsWithPriority() throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionFieldsWithPriority(rep);
        }
    }

    private void runAssertionFieldsWithPriority(EventRepresentationChoice eventRepresentationEnum) throws Exception
    {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " insert into MyStream select theString, intPrimitive from SupportBean(theString not like 'Z%')");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " insert into MyStream select 'AX'||theString as theString, intPrimitive from SupportBean(theString like 'Z%')");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " @Name('a') @Priority(12) update istream MyStream set intPrimitive=-2 where intPrimitive=-1");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " @Name('b') @Priority(11) update istream MyStream set intPrimitive=-1 where theString like 'D%'");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " @Name('c') @Priority(9) update istream MyStream set intPrimitive=9 where theString like 'A%'");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " @Name('d') @Priority(8) update istream MyStream set intPrimitive=8 where theString like 'A%' or theString like 'C%'");
        epService.getEPAdministrator().createEPL(" @Name('e') @Priority(10) update istream MyStream set intPrimitive=10 where theString like 'A%'");
        epService.getEPAdministrator().createEPL(" @Name('f') @Priority(7) update istream MyStream set intPrimitive=7 where theString like 'A%' or theString like 'C%'");
        epService.getEPAdministrator().createEPL(" @Name('g') @Priority(6) update istream MyStream set intPrimitive=6 where theString like 'A%'");
        epService.getEPAdministrator().createEPL(" @Name('h') @Drop update istream MyStream set intPrimitive=6 where theString like 'B%'");

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyStream where intPrimitive > 0");
        stmtSelect.addListener(listener);

        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("C1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"C1", 8});

        epService.getEPRuntime().sendEvent(new SupportBean("D1", 100));
        assertFalse(listener.isInvoked());

        stmtSelect.stop();
        stmtSelect = epService.getEPAdministrator().createEPL("select * from MyStream");
        stmtSelect.addListener(listener);
        assertTrue(eventRepresentationEnum.matchesClass(stmtSelect.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("D1", -2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"D1", -2});

        epService.getEPRuntime().sendEvent(new SupportBean("Z1", -3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"AXZ1", 10});

        epService.getEPAdministrator().getStatement("e").stop();
        epService.getEPRuntime().sendEvent(new SupportBean("Z2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"AXZ2", 9});

        epService.getEPAdministrator().getStatement("c").stop();
        epService.getEPAdministrator().getStatement("d").stop();
        epService.getEPAdministrator().getStatement("f").stop();
        epService.getEPAdministrator().getStatement("g").stop();
        epService.getEPRuntime().sendEvent(new SupportBean("Z3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"AXZ3", 0});

        epService.initialize();
    }

    public void testInsertDirectBeanTypeInheritance() throws Exception
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", String.class);
        type.put("p1", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);
        epService.getEPAdministrator().getConfiguration().addEventType("BaseInterface", BaseInterface.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BaseOne", BaseOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BaseOneA", BaseOneA.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BaseOneB", BaseOneB.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BaseTwo", BaseTwo.class);

        // test update applies to child types via interface
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into BaseOne select p0 as i, p1 as p from MyMapType");
        epService.getEPAdministrator().createEPL("@Name('a') update istream BaseInterface set i='XYZ' where i like 'E%'");
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from BaseOne");
        stmtSelect.addListener(listener);

        String[] fields = "i,p".split(",");
        epService.getEPRuntime().sendEvent(makeMap("p0", "E1", "p1", "E1"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"XYZ", "E1"});

        epService.getEPRuntime().sendEvent(makeMap("p0", "F1", "p1", "E2"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"F1", "E2"});

        epService.getEPAdministrator().createEPL("@Priority(2) @Name('b') update istream BaseOne set i='BLANK'");

        epService.getEPRuntime().sendEvent(makeMap("p0", "somevalue", "p1", "E3"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E3"});

        epService.getEPAdministrator().createEPL("@Priority(3) @Name('c') update istream BaseOneA set i='FINAL'");

        epService.getEPRuntime().sendEvent(makeMap("p0", "somevalue", "p1", "E4"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E4"});

        stmtInsert.stop();
        stmtInsert = epService.getEPAdministrator().createEPL("insert into BaseOneA select p0 as i, p1 as p, 'a' as pa from MyMapType");

        epService.getEPRuntime().sendEvent(makeMap("p0", "somevalue", "p1", "E5"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"FINAL", "E5"});

        stmtInsert.stop();
        stmtInsert = epService.getEPAdministrator().createEPL("insert into BaseOneB select p0 as i, p1 as p, 'b' as pb from MyMapType");

        epService.getEPRuntime().sendEvent(makeMap("p0", "somevalue", "p1", "E6"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E6"});

        stmtInsert.stop();
        stmtInsert = epService.getEPAdministrator().createEPL("insert into BaseTwo select p0 as i, p1 as p from MyMapType");

        stmtSelect.stop();
        stmtSelect = epService.getEPAdministrator().createEPL("select * from BaseInterface");
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(makeMap("p0", "E2", "p1", "E7"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), new String[]{"i"}, new Object[]{"XYZ"});
    }

    public void testNamedWindow()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", String.class);
        type.put("p1", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String[] fields = "p0,p1".split(",");
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        SupportUpdateListener listenerOnSelect = new SupportUpdateListener();
        SupportUpdateListener listenerInsertOnSelect = new SupportUpdateListener();
        SupportUpdateListener listenerWindowSelect = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL("create window AWindow#keepall select * from MyMapType").addListener(listenerWindow);
        epService.getEPAdministrator().createEPL("insert into AWindow select * from MyMapType").addListener(listenerInsert);
        epService.getEPAdministrator().createEPL("select * from AWindow").addListener(listenerWindowSelect);
        epService.getEPAdministrator().createEPL("update istream AWindow set p1='newvalue'");

        epService.getEPRuntime().sendEvent(makeMap("p0", "E1", "p1", "oldvalue"), "MyMapType");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E1", "oldvalue"});
        EPAssertionUtil.assertProps(listenerWindowSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

        epService.getEPAdministrator().createEPL("on SupportBean(theString='A') select win.* from AWindow as win").addListener(listenerOnSelect);
        epService.getEPRuntime().sendEvent(new SupportBean("A", 0));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

        epService.getEPAdministrator().createEPL("on SupportBean(theString='B') insert into MyOtherStream select win.* from AWindow as win").addListener(listenerOnSelect);
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

        epService.getEPAdministrator().createEPL("update istream MyOtherStream set p0='a', p1='b'");
        epService.getEPAdministrator().createEPL("select * from MyOtherStream").addListener(listenerInsertOnSelect);
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});
        EPAssertionUtil.assertProps(listenerInsertOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"a", "b"});
    }

    public void testTypeWidener()
    {
        String[] fields = "theString,longBoxed,intBoxed".split(",");
        epService.getEPAdministrator().createEPL("insert into AStream select * from SupportBean");
        epService.getEPAdministrator().createEPL("update istream AStream set longBoxed=intBoxed, intBoxed=null");
        epService.getEPAdministrator().createEPL("select * from AStream").addListener(listener);

        SupportBean bean = new SupportBean("E1", 0);
        bean.setLongBoxed(888L);
        bean.setIntBoxed(999);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 999L, null});
    }

    public void testSendRouteSenderPreprocess()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", String.class);
        type.put("p1", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // test map
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyMapType");
        stmtSelect.addListener(listener);
        epService.getEPAdministrator().createEPL("update istream MyMapType set p0='a'");

        String[] fields = "p0,p1".split(",");
        epService.getEPRuntime().sendEvent(makeMap("p0", "E1", "p1", "E1"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a", "E1"});

        EventSender sender = epService.getEPRuntime().getEventSender("MyMapType");
        sender.sendEvent(makeMap("p0", "E2", "p1", "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a", "E2"});

        EPStatement stmtTrigger = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmtTrigger.addListener(new UpdateListener()
        {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                epService.getEPRuntime().route(makeMap("p0", "E3", "p1", "E3"), "MyMapType");
            }
        });
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a", "E3"});
        
        EPStatement stmtDrop = epService.getEPAdministrator().createEPL("@Drop update istream MyMapType set p0='a'");
        sender.sendEvent(makeMap("p0", "E4", "p1", "E4"));
        epService.getEPRuntime().sendEvent(makeMap("p0", "E5", "p1", "E5"), "MyMapType");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        stmtDrop.destroy();
        stmtSelect.destroy();
        stmtTrigger.destroy();

        // test bean
        stmtSelect = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmtSelect.addListener(listener);
        epService.getEPAdministrator().createEPL("update istream SupportBean set intPrimitive=999");

        fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 999});

        sender = epService.getEPRuntime().getEventSender("SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 999});

        stmtTrigger = epService.getEPAdministrator().createEPL("select * from MyMapType");
        stmtTrigger.addListener(new UpdateListener()
        {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                epService.getEPRuntime().route(new SupportBean("E3", 0));
            }
        });
        epService.getEPRuntime().sendEvent(makeMap("p0", "", "p1", ""), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 999});

        stmtDrop = epService.getEPAdministrator().createEPL("@Drop update istream SupportBean set intPrimitive=1");
        sender.sendEvent(new SupportBean("E4", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        epService.getEPRuntime().sendEvent(makeMap("p0", "", "p1", ""), "MyMapType");
        assertFalse(listener.isInvoked());
    }

    public void testSODA()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("p0", String.class);
        type.put("p1", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setUpdateClause(UpdateClause.create("MyMapType", Expressions.eq(Expressions.property("p1"), Expressions.constant("newvalue"))));
        model.getUpdateClause().setOptionalAsClauseStreamName("mytype");
        model.getUpdateClause().setOptionalWhereClause(Expressions.eq("p0", "E1"));
        assertEquals("update istream MyMapType as mytype set p1=\"newvalue\" where p0=\"E1\"", model.toEPL());
        
        // test map
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyMapType");
        stmtSelect.addListener(listener);
        epService.getEPAdministrator().create(model);

        String[] fields = "p0,p1".split(",");
        epService.getEPRuntime().sendEvent(makeMap("p0", "E1", "p1", "E1"), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

        // test unmap
        String text = "update istream MyMapType as mytype set p1=\"newvalue\" where p0=\"E1\"";
        model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
    }

    public void testXMLEvent() throws Exception
    {
        String xml = "<simpleEvent><prop1>SAMPLE_V1</prop1></simpleEvent>";

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        ConfigurationEventTypeXMLDOM config = new ConfigurationEventTypeXMLDOM();
        config.setRootElementName("simpleEvent");
        epService.getEPAdministrator().getConfiguration().addEventType("MyXMLEvent", config);

        epService.getEPAdministrator().createEPL("insert into ABCStream select 1 as valOne, 2 as valTwo, * from MyXMLEvent");
        epService.getEPAdministrator().createEPL("update istream ABCStream set valOne = 987, valTwo=123 where prop1='SAMPLE_V1'");
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(simpleDoc);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "valOne,valTwo,prop1".split(","), new Object[]{987, 123, "SAMPLE_V1"});
    }

    public void testWrappedObject() throws Exception
    {
        epService.getEPAdministrator().createEPL("insert into ABCStream select 1 as valOne, 2 as valTwo, * from SupportBean");
        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream ABCStream set valOne = 987, valTwo=123");
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{987, 123, "E1"});

        stmtUpd.destroy();
        stmtUpd = epService.getEPAdministrator().createEPL("update istream ABCStream set theString = 'A'");

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{1, 2, "A"});

        stmtUpd.destroy();
        stmtUpd = epService.getEPAdministrator().createEPL("update istream ABCStream set theString = 'B', valOne = 555");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{555, 2, "B"});
    }

    public void testCopyMethod()
    {
        epService.getEPAdministrator().createEPL("insert into ABCStream select * from SupportBeanCopyMethod");
        epService.getEPAdministrator().createEPL("update istream ABCStream set valOne = 'x', valTwo='y'");
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanCopyMethod("1", "2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "valOne,valTwo".split(","), new Object[]{"x", "y"});
    }

    public void testSubquery()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("s0", String.class);
        type.put("s1", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapTypeSelect", type);

        type = new HashMap<String, Object>();
        type.put("w0", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapTypeWhere", type);

        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPAdministrator().createEPL("insert into ABCStream select * from SupportBean");
        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream ABCStream set theString = (select s0 from MyMapTypeSelect#lastevent) where intPrimitive in (select w0 from MyMapTypeWhere#keepall)");
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 0});

        epService.getEPRuntime().sendEvent(makeMap("w0", 1), "MyMapTypeWhere");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});

        epService.getEPRuntime().sendEvent(makeMap("s0", "newvalue"), "MyMapTypeSelect");
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"newvalue", 1});

        epService.getEPRuntime().sendEvent(makeMap("s0", "othervalue"), "MyMapTypeSelect");
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"othervalue", 1});

        // test correlated subquery
        stmtUpd.destroy();
        stmtUpd = epService.getEPAdministrator().createEPL("update istream ABCStream set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = ABCStream.theString)");

        // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 8));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", 8});

        epService.getEPRuntime().sendEvent(makeMap("s0", "E7", "s1", 91), "MyMapTypeSelect");
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E7", 91});

        // test correlated with as-clause
        stmtUpd.destroy();
        epService.getEPAdministrator().createEPL("update istream ABCStream as mystream set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = mystream.theString)");

        // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 111));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E8", 111});

        epService.getEPRuntime().sendEvent(makeMap("s0", "E9", "s1", -1), "MyMapTypeSelect");
        epService.getEPRuntime().sendEvent(new SupportBean("E9", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E9", -1});
    }

    public void testUnprioritizedOrder()
    {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("s0", String.class);
        type.put("s1", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapType", type);

        String[] fields = "s0,s1".split(",");
        epService.getEPAdministrator().createEPL("insert into ABCStream select * from MyMapType");
        epService.getEPAdministrator().createEPL("@Name('A') update istream ABCStream set s0='A'");
        epService.getEPAdministrator().createEPL("@Name('B') update istream ABCStream set s0='B'");
        epService.getEPAdministrator().createEPL("@Name('C') update istream ABCStream set s0='C'");
        epService.getEPAdministrator().createEPL("@Name('D') update istream ABCStream set s0='D'");
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(makeMap("s0", "", "s1", 1), "MyMapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"D", 1});
    }

    public void testListenerDeliveryMultiupdate()
    {
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        SupportUpdateListener listeners[] = new SupportUpdateListener[5];
        for (int i = 0; i < listeners.length; i++)
        {
            listeners[i] = new SupportUpdateListener();
        }
        
        String[] fields = "theString,intPrimitive,value1".split(",");
        epService.getEPAdministrator().createEPL("insert into ABCStream select *, 'orig' as value1 from SupportBean").addListener(listenerInsert);
        epService.getEPAdministrator().createEPL("@Name('A') update istream ABCStream set theString='A', value1='a' where intPrimitive in (1,2)").addListener(listeners[0]);
        epService.getEPAdministrator().createEPL("@Name('B') update istream ABCStream set theString='B', value1='b' where intPrimitive in (1,3)").addListener(listeners[1]);
        epService.getEPAdministrator().createEPL("@Name('C') update istream ABCStream set theString='C', value1='c' where intPrimitive in (2,3)").addListener(listeners[2]);
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E1", 1, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 1, "a"});
        EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 1, "a"});
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 1, "b"});
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 1, "b"});
        reset(listeners);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E2", 2, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 2, "a"});
        assertFalse(listeners[1].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"A", 2, "a"});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 2, "c"});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"C", 2, "c"});
        reset(listeners);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3, "orig"});
        assertFalse(listeners[0].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"E3", 3, "orig"});
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 3, "b"});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"B", 3, "b"});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 3, "c"});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"C", 3, "c"});
        reset(listeners);
    }

    public void testListenerDeliveryMultiupdateMixed()
    {
        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        SupportUpdateListener listeners[] = new SupportUpdateListener[5];
        for (int i = 0; i < listeners.length; i++)
        {
            listeners[i] = new SupportUpdateListener();
        }

        String[] fields = "theString,intPrimitive,value1".split(",");
        epService.getEPAdministrator().createEPL("insert into ABCStream select *, 'orig' as value1 from SupportBean").addListener(listenerInsert);
        epService.getEPAdministrator().createEPL("select * from ABCStream").addListener(listener);

        epService.getEPAdministrator().createEPL("@Name('A') update istream ABCStream set theString='A', value1='a'");
        epService.getEPAdministrator().createEPL("@Name('B') update istream ABCStream set theString='B', value1='b'").addListener(listeners[1]);
        epService.getEPAdministrator().createEPL("@Name('C') update istream ABCStream set theString='C', value1='c'");
        epService.getEPAdministrator().createEPL("@Name('D') update istream ABCStream set theString='D', value1='d'").addListener(listeners[3]);
        epService.getEPAdministrator().createEPL("@Name('E') update istream ABCStream set theString='E', value1='e'");

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4, "orig"});
        assertFalse(listeners[0].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 4, "a"});
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 4, "b"});
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listeners[3].assertOneGetOld(), fields, new Object[]{"C", 4, "c"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNew(), fields, new Object[]{"D", 4, "d"});
        assertFalse(listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E", 4, "e"});
        reset(listeners);

        epService.getEPAdministrator().getStatement("B").removeAllListeners();
        epService.getEPAdministrator().getStatement("D").removeAllListeners();
        epService.getEPAdministrator().getStatement("A").addListener(listeners[0]);
        epService.getEPAdministrator().getStatement("E").addListener(listeners[4]);

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E5", 5, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E5", 5, "orig"});
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 5, "a"});
        assertFalse(listeners[1].isInvoked());
        assertFalse(listeners[2].isInvoked());
        assertFalse(listeners[3].isInvoked());
        EPAssertionUtil.assertProps(listeners[4].assertOneGetOld(), fields, new Object[]{"D", 5, "d"});
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNew(), fields, new Object[]{"E", 5, "e"});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E", 5, "e"});
        reset(listeners);
    }

    private void reset(SupportUpdateListener[] listeners)
    {
        for (SupportUpdateListener listener : listeners)
        {
            listener.reset();
        }
    }

    private Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2, String prop3, Object val3)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        map.put(prop3, val3);
        return map;
    }

    private Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        return map;
    }

    private Map<String, Object> makeMap(String prop1, Object val1)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(prop1, val1);
        return map;
    }

    private void tryInvalid(String expression, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    public static interface BaseInterface extends Serializable
    {
        public String getI();
        public void setI(String i);
    }

    public static class BaseOne implements BaseInterface, Serializable
    {
        private String i;
        private String p;

        public BaseOne()
        {
        }

        public BaseOne(String i, String p)
        {
            this.i = i;
            this.p = p;
        }

        public String getP()
        {
            return p;
        }

        public void setP(String p)
        {
            this.p = p;
        }

        public String getI()
        {
            return i;
        }

        public void setI(String i)
        {
            this.i = i;
        }
    }

    public static class BaseTwo implements BaseInterface, Serializable
    {
        private String i;
        private String p;

        public BaseTwo()
        {
        }

        public BaseTwo(String p)
        {
            this.p = p;
        }

        public void setP(String p)
        {
            this.p = p;
        }

        public String getP()
        {
            return p;
        }

        public String getI()
        {
            return i;
        }

        public void setI(String i)
        {
            this.i = i;
        }
    }

    public static class BaseOneA extends BaseOne
    {
        private String pa;

        public BaseOneA()
        {
        }

        public BaseOneA(String i, String p, String pa)
        {
            super(i, p);
            this.pa = pa;
        }

        public String getPa()
        {
            return pa;
        }

        public void setPa(String pa)
        {
            this.pa = pa;
        }
    }    

    public static class BaseOneB extends BaseOne
    {
        private String pb;

        public BaseOneB()
        {
        }

        public BaseOneB(String i, String p, String pb)
        {
            super(i, p);
            this.pb = pb;
        }

        public String getPb()
        {
            return pb;
        }

        public void setPb(String pb)
        {
            this.pb = pb;
        }
    }

    public static void setIntBoxedValue(SupportBean sb, int value) {
        sb.setIntBoxed(value);
    }
}
