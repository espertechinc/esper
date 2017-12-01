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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import java.util.Collections;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecInsertIntoPopulateEventTypeColumn implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);

        runAssertionTypableSubquery(epService);
        runAssertionEnumerationSubquery(epService);
        runAssertionTypableNewOperatorDocSample(epService);
        runAssertionTypableAndCaseNew(epService, EventRepresentationChoice.MAP);
        runAssertionTypableAndCaseNew(epService, EventRepresentationChoice.ARRAY);
        runAssertionInvalid(epService);
    }

    private void runAssertionTypableSubquery(EPServiceProvider epService) throws Exception {
        tryAssertionTypableSubqueryMulti(epService, "objectarray");
        tryAssertionTypableSubqueryMulti(epService, "map");

        tryAssertionTypableSubquerySingleMayFilter(epService, "objectarray", true);
        tryAssertionTypableSubquerySingleMayFilter(epService, "map", true);

        tryAssertionTypableSubquerySingleMayFilter(epService, "objectarray", false);
        tryAssertionTypableSubquerySingleMayFilter(epService, "map", false);

        tryAssertionTypableSubqueryMultiFilter(epService, "objectarray");
        tryAssertionTypableSubqueryMultiFilter(epService, "map");
    }

    private void runAssertionEnumerationSubquery(EPServiceProvider epService) {
        tryAssertionEnumerationSubqueryMultiMayFilter(epService, "objectarray", true);
        tryAssertionEnumerationSubqueryMultiMayFilter(epService, "map", true);

        tryAssertionEnumerationSubqueryMultiMayFilter(epService, "objectarray", false);
        tryAssertionEnumerationSubqueryMultiMayFilter(epService, "map", false);

        tryAssertionEnumerationSubquerySingleMayFilter(epService, "objectarray", true);
        tryAssertionEnumerationSubquerySingleMayFilter(epService, "map", true);

        tryAssertionEnumerationSubquerySingleMayFilter(epService, "objectarray", false);
        tryAssertionEnumerationSubquerySingleMayFilter(epService, "map", false);

        tryAssertionFragmentSingeColNamedWindow(epService);
    }

    private void runAssertionTypableNewOperatorDocSample(EPServiceProvider epService) {
        tryAssertionTypableNewOperatorDocSample(epService, "objectarray");
        tryAssertionTypableNewOperatorDocSample(epService, "map");
    }

    private void runAssertionTypableAndCaseNew(EPServiceProvider epService, EventRepresentationChoice representation) {
        epService.getEPAdministrator().createEPL("create " + representation.getOutputTypeCreateSchemaName() + " schema Nested(p0 string, p1 int)");
        epService.getEPAdministrator().createEPL("create " + representation.getOutputTypeCreateSchemaName() + " schema OuterType(n0 Nested)");

        String[] fields = "n0.p0,n0.p1".split(",");
        epService.getEPAdministrator().createEPL("@Name('out') " +
                "expression computeNested {\n" +
                "  sb => case\n" +
                "  when intPrimitive = 1 \n" +
                "    then new { p0 = 'a', p1 = 1}\n" +
                "  else new { p0 = 'b', p1 = 2 }\n" +
                "  end\n" +
                "}\n" +
                "insert into OuterType select computeNested(sb) as n0 from SupportBean as sb");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"b", 2});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a", 1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTypableNewOperatorDocSample(EPServiceProvider epService, String typeType) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema Item(name string, price double)");
        epService.getEPAdministrator().createEPL("create " + typeType + " schema PurchaseOrder(orderId string, items Item[])");
        epService.getEPAdministrator().createEPL("create schema TriggerEvent()");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into PurchaseOrder select '001' as orderId, new {name= 'i1', price=10} as items from TriggerEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(Collections.emptyMap(), "TriggerEvent");
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "orderId,items[0].name,items[0].price".split(","), new Object[]{"001", "i1", 10d});
        EventBean[] underlying = (EventBean[]) event.get("items");
        assertEquals(1, underlying.length);
        assertEquals("i1", underlying[0].get("name"));
        assertEquals(10d, underlying[0].get("price"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("Item", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("PurchaseOrder", true);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema N1_1(p0 int)");
        epService.getEPAdministrator().createEPL("create schema N1_2(p1 N1_1)");

        // enumeration type is incompatible
        epService.getEPAdministrator().createEPL("create schema TypeOne(sbs SupportBean[])");
        tryInvalid(epService, "insert into TypeOne select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Error starting statement: Incompatible type detected attempting to insert into column 'sbs' type 'SupportBean' compared to selected type 'SupportBean_S0' [insert into TypeOne select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1]");

        epService.getEPAdministrator().createEPL("create schema TypeTwo(sbs SupportBean)");
        tryInvalid(epService, "insert into TypeTwo select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Error starting statement: Incompatible type detected attempting to insert into column 'sbs' type 'SupportBean' compared to selected type 'SupportBean_S0' [insert into TypeTwo select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1]");

        // typable - selected column type is incompatible
        tryInvalid(epService, "insert into N1_2 select new {p0='a'} as p1 from SupportBean",
                "Error starting statement: Invalid assignment of column 'p0' of type 'java.lang.String' to event property 'p0' typed as 'java.lang.Integer', column and parameter types mismatch [insert into N1_2 select new {p0='a'} as p1 from SupportBean]");

        // typable - selected column type is not matching anything
        tryInvalid(epService, "insert into N1_2 select new {xxx='a'} as p1 from SupportBean",
                "Error starting statement: Failed to find property 'xxx' among properties for target event type 'N1_1' [insert into N1_2 select new {xxx='a'} as p1 from SupportBean]");
    }

    private void tryAssertionFragmentSingeColNamedWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema AEvent (symbol string)");
        epService.getEPAdministrator().createEPL("create window MyEventWindow#lastevent (e AEvent)");
        epService.getEPAdministrator().createEPL("insert into MyEventWindow select (select * from AEvent#lastevent) as e from SupportBean(theString = 'A')");
        epService.getEPAdministrator().createEPL("create schema BEvent (e AEvent)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into BEvent select (select e from MyEventWindow) as e from SupportBean(theString = 'B')");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(Collections.singletonMap("symbol", "GE"), "AEvent");
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 2));
        EventBean result = listener.assertOneGetNewAndReset();
        EventBean fragment = (EventBean) result.get("e");
        assertEquals("AEvent", fragment.getEventType().getName());
        assertEquals("GE", fragment.get("symbol"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTypableSubquerySingleMayFilter(EPServiceProvider epService, String typeType, boolean filter) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)");
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventOne(ez EventZero)");

        String[] fields = "ez.e0_0,ez.e0_1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into EventOne select " +
                "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#lastevent" +
                (filter ? " where id >= 100" : "") + ") as ez " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x1", "y1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        Object[] expected = filter ? new Object[]{null, null} : new Object[]{"x1", "y1"};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "x2", "y2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"x2", "y2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "x3", "y3"));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        expected = filter ? new Object[]{null, null} : new Object[]{"x3", "y3"};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("EventZero", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("EventOne", true);
    }

    private void tryAssertionTypableSubqueryMulti(EPServiceProvider epService, String typeType) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)");
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventOne(e1_0 string, ez EventZero[])");

        String[] fields = "e1_0,ez[0].e0_0,ez[0].e0_1,ez[1].e0_0,ez[1].e0_1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("" +
                "expression thequery {" +
                "  (select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall)" +
                "} " +
                "insert into EventOne select " +
                "theString as e1_0, " +
                "thequery() as ez " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x1", "y1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{"E1", "x1", "y1", null, null});
        SupportEventTypeAssertionUtil.assertConsistency(event);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "x2", "y2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "x1", "y1", "x2", "y2"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("EventZero", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("EventOne", true);
    }

    private void tryAssertionTypableSubqueryMultiFilter(EPServiceProvider epService, String typeType) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)");
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventOne(ez EventZero[])");

        String[] fields = "e0_0".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into EventOne select " +
                "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall where id between 10 and 20) as ez " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x1", "y1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow((EventBean[]) listener.assertOneGetNewAndReset().get("ez"), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "x2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "x3"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow((EventBean[]) listener.assertOneGetNewAndReset().get("ez"), fields, new Object[][]{{"x2"}, {"x3"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("EventZero", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("EventOne", true);
    }

    private void tryAssertionEnumerationSubqueryMultiMayFilter(EPServiceProvider epService, String typeType, boolean filter) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventOne(sbarr SupportBean_S0[])");

        String[] fields = "p00".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into EventOne select " +
                "(select * from SupportBean_S0#keepall " +
                (filter ? "where 1=1" : "") + ") as sbarr " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean[] inner = (EventBean[]) listener.assertOneGetNewAndReset().get("sbarr");
        EPAssertionUtil.assertPropsPerRow(inner, fields, new Object[][]{{"x1"}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "x2", "y2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        inner = (EventBean[]) listener.assertOneGetNewAndReset().get("sbarr");
        EPAssertionUtil.assertPropsPerRow(inner, fields, new Object[][]{{"x1"}, {"x2"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("EventOne", true);
    }

    private void tryAssertionEnumerationSubquerySingleMayFilter(EPServiceProvider epService, String typeType, boolean filter) {
        epService.getEPAdministrator().createEPL("create " + typeType + " schema EventOne(sb SupportBean_S0)");

        String[] fields = "sb.p00".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into EventOne select " +
                "(select * from SupportBean_S0#length(2) " +
                (filter ? "where id >= 100" : "") + ") as sb " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "x1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        Object[] expected = filter ? new Object[]{null} : new Object[]{"x1"};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "x2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        String received = (String) listener.assertOneGetNewAndReset().get(fields[0]);
        if (filter) {
            assertEquals("x2", received);
        } else {
            if (!received.equals("x1") && !received.equals("x2")) {
                fail();
            }
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("EventOne", true);
    }
}
