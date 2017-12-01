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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecNamedWindowInsertFrom implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        SupportUpdateListener[] listeners = new SupportUpdateListener[10];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportUpdateListener();
        }

        runAssertionCreateNamedAfterNamed(epService, listeners);
        runAssertionInsertWhereTypeAndFilter(epService, listeners);
        runAssertionInsertWhereOMStaggered(epService);
        runAssertionVariantStream(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionCreateNamedAfterNamed(EPServiceProvider epService, SupportUpdateListener[] listeners) {
        // create window
        String stmtTextCreateOne = "create window MyWindow#keepall as SupportBean";
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        stmtCreateOne.addListener(listeners[0]);

        // create window
        String stmtTextCreateTwo = "create window MyWindowTwo#keepall as MyWindow";
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        stmtCreateTwo.addListener(listeners[1]);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select theString from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listeners[2]);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        String[] fields = new String[]{"theString"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertWhereTypeAndFilter(EPServiceProvider epService, SupportUpdateListener[] listeners) throws Exception {
        String[] fields = new String[]{"theString"};

        // create window
        String stmtTextCreateOne = "create window MyWindowIWT#keepall as SupportBean";
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne, "name1");
        stmtCreateOne.addListener(listeners[0]);
        EventType eventTypeOne = stmtCreateOne.getEventType();

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowIWT select * from SupportBean(intPrimitive > 0)";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // populate some data
        assertEquals(0, getCount(epService, "MyWindowIWT"));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        assertEquals(1, getCount(epService, "MyWindowIWT"));
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("C3", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("C5", 4));
        assertEquals(5, getCount(epService, "MyWindowIWT"));
        assertEquals("name1", getStatementName(epService, "MyWindowIWT"));
        assertEquals(stmtTextCreateOne, getEPL(epService, "MyWindowIWT"));
        listeners[0].reset();

        // create window with keep-all
        String stmtTextCreateTwo = "create window MyWindowTwo#keepall as MyWindowIWT insert";
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        stmtCreateTwo.addListener(listeners[2]);
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fields, new Object[][]{{"A1"}, {"B2"}, {"C3"}, {"A4"}, {"C5"}});
        EventType eventTypeTwo = stmtCreateTwo.iterator().next().getEventType();
        assertFalse(listeners[2].isInvoked());
        assertEquals(5, getCount(epService, "MyWindowTwo"));
        assertEquals(StatementType.CREATE_WINDOW, ((EPStatementSPI) stmtCreateTwo).getStatementMetadata().getStatementType());

        // create window with keep-all and filter
        String stmtTextCreateThree = "create window MyWindowThree#keepall as MyWindowIWT insert where theString like 'A%'";
        EPStatement stmtCreateThree = epService.getEPAdministrator().createEPL(stmtTextCreateThree);
        stmtCreateThree.addListener(listeners[3]);
        EPAssertionUtil.assertPropsPerRow(stmtCreateThree.iterator(), fields, new Object[][]{{"A1"}, {"A4"}});
        EventType eventTypeThree = stmtCreateThree.iterator().next().getEventType();
        assertFalse(listeners[3].isInvoked());
        assertEquals(2, getCount(epService, "MyWindowThree"));

        // create window with last-per-id
        String stmtTextCreateFour = "create window MyWindowFour#unique(intPrimitive) as MyWindowIWT insert";
        EPStatement stmtCreateFour = epService.getEPAdministrator().createEPL(stmtTextCreateFour);
        stmtCreateFour.addListener(listeners[4]);
        EPAssertionUtil.assertPropsPerRow(stmtCreateFour.iterator(), fields, new Object[][]{{"C3"}, {"C5"}});
        EventType eventTypeFour = stmtCreateFour.iterator().next().getEventType();
        assertFalse(listeners[4].isInvoked());
        assertEquals(2, getCount(epService, "MyWindowFour"));

        epService.getEPAdministrator().createEPL("insert into MyWindowIWT select * from SupportBean(theString like 'A%')");
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo select * from SupportBean(theString like 'B%')");
        epService.getEPAdministrator().createEPL("insert into MyWindowThree select * from SupportBean(theString like 'C%')");
        epService.getEPAdministrator().createEPL("insert into MyWindowFour select * from SupportBean(theString like 'D%')");
        assertFalse(listeners[0].isInvoked() || listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B9", -9));
        EventBean received = listeners[2].assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"B9"});
        assertSame(eventTypeTwo, received.getEventType());
        assertFalse(listeners[0].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        assertEquals(6, getCount(epService, "MyWindowTwo"));

        epService.getEPRuntime().sendEvent(new SupportBean("A8", -8));
        received = listeners[0].assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"A8"});
        assertSame(eventTypeOne, received.getEventType());
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("C7", -7));
        received = listeners[3].assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"C7"});
        assertSame(eventTypeThree, received.getEventType());
        assertFalse(listeners[2].isInvoked() || listeners[0].isInvoked() || listeners[4].isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("D6", -6));
        received = listeners[4].assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"D6"});
        assertSame(eventTypeFour, received.getEventType());
        assertFalse(listeners[2].isInvoked() || listeners[0].isInvoked() || listeners[3].isInvoked());
    }

    private void runAssertionInsertWhereOMStaggered(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionInsertWhereOMStaggered(epService, rep);
        }
    }

    private void tryAssertionInsertWhereOMStaggered(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {

        Map<String, Object> dataType = makeMap(new Object[][]{{"a", String.class}, {"b", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMap", dataType);

        String stmtTextCreateOne = eventRepresentationEnum.getAnnotationText() + " create window MyWindowIWOM#keepall as select a, b from MyMap";
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreateOne.getEventType().getUnderlyingType()));
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCreateOne.addListener(listener);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowIWOM select a, b from MyMap";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // populate some data
        epService.getEPRuntime().sendEvent(makeMap(new Object[][]{{"a", "E1"}, {"b", 2}}), "MyMap");
        epService.getEPRuntime().sendEvent(makeMap(new Object[][]{{"a", "E2"}, {"b", 10}}), "MyMap");
        epService.getEPRuntime().sendEvent(makeMap(new Object[][]{{"a", "E3"}, {"b", 10}}), "MyMap");

        // create window with keep-all using OM
        EPStatementObjectModel model = new EPStatementObjectModel();
        eventRepresentationEnum.addAnnotationForNonMap(model);
        Expression where = Expressions.eq("b", 10);
        model.setCreateWindow(CreateWindowClause.create("MyWindowIWOMTwo", View.create("keepall")).insert(true).insertWhereClause(where));
        model.setSelectClause(SelectClause.createWildcard());
        model.setFromClause(FromClause.create(FilterStream.create("MyWindowIWOM")));
        String text = eventRepresentationEnum.getAnnotationTextForNonMap() + " create window MyWindowIWOMTwo#keepall as select * from MyWindowIWOM insert where b=10";
        assertEquals(text.trim(), model.toEPL());

        EPStatementObjectModel modelTwo = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text.trim(), modelTwo.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(modelTwo);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "a,b".split(","), new Object[][]{{"E2", 10}, {"E3", 10}});

        // test select individual fields and from an insert-from named window
        stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window MyWindowIWOMThree#keepall as select a from MyWindowIWOMTwo insert where a = 'E2'");
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "a".split(","), new Object[][]{{"E2"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowIWOM", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowIWOMTwo", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowIWOMThree", true);
    }

    private void runAssertionVariantStream(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_B", SupportBean_B.class);

        ConfigurationVariantStream config = new ConfigurationVariantStream();
        //config.setTypeVariance(ConfigurationVariantStream.TypeVariance.ANY);
        config.addEventTypeName("SupportBean_A");
        config.addEventTypeName("SupportBean_B");
        epService.getEPAdministrator().getConfiguration().addVariantStream("VarStream", config);
        epService.getEPAdministrator().createEPL("create window MyWindowVS#keepall as select * from VarStream");
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindowVSTwo#keepall as MyWindowVS");

        epService.getEPAdministrator().createEPL("insert into VarStream select * from SupportBean_A");
        epService.getEPAdministrator().createEPL("insert into VarStream select * from SupportBean_B");
        epService.getEPAdministrator().createEPL("insert into MyWindowVSTwo select * from VarStream");
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EventBean[] events = EPAssertionUtil.iteratorToArray(stmt.iterator());
        assertEquals("A1", events[0].get("id?"));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "id?".split(","), new Object[][]{{"A1"}, {"B1"}});
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String stmtTextCreateOne = "create window MyWindowINV#keepall as SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);

        tryInvalid(epService, "create window testWindow3#keepall as SupportBean insert",
                "A named window by name 'SupportBean' could not be located, use the insert-keyword with an existing named window [create window testWindow3#keepall as SupportBean insert]");
        tryInvalid(epService, "create window testWindow3#keepall as select * from " + SupportBean.class.getName() + " insert where (intPrimitive = 10)",
                "A named window by name '" + SupportBean.class.getName() + "' could not be located, use the insert-keyword with an existing named window [");
        tryInvalid(epService, "create window MyWindowTwo#keepall as MyWindowINV insert where (select intPrimitive from SupportBean#lastevent)",
                "Create window where-clause may not have a subselect [create window MyWindowTwo#keepall as MyWindowINV insert where (select intPrimitive from SupportBean#lastevent)]");
        tryInvalid(epService, "create window MyWindowTwo#keepall as MyWindowINV insert where sum(intPrimitive) > 2",
                "Create window where-clause may not have an aggregation function [create window MyWindowTwo#keepall as MyWindowINV insert where sum(intPrimitive) > 2]");
        tryInvalid(epService, "create window MyWindowTwo#keepall as MyWindowINV insert where prev(1, intPrimitive) = 1",
                "Create window where-clause may not have a function that requires view resources (prior, prev) [create window MyWindowTwo#keepall as MyWindowINV insert where prev(1, intPrimitive) = 1]");
    }

    private Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        if (entries == null) {
            return result;
        }
        for (int i = 0; i < entries.length; i++) {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }

    private long getCount(EPServiceProvider epService, String windowName) throws Exception {
        NamedWindowProcessor processor = ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getProcessor(windowName);
        return processor.getProcessorInstance(null).getCountDataWindow();
    }

    private String getStatementName(EPServiceProvider epService, String windowName) throws Exception {
        NamedWindowProcessor processor = ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getProcessor(windowName);
        return processor.getStatementName();
    }

    private String getEPL(EPServiceProvider epService, String windowName) throws Exception {
        NamedWindowProcessor processor = ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getProcessor(windowName);
        return processor.getEplExpression();
    }
}
