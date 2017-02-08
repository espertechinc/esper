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
package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestAggregateWithRollupGroupingFuncs extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testFAFCarEventAndGroupingFunc() {
        epService.getEPAdministrator().getConfiguration().addEventType(CarEvent.class);
        epService.getEPAdministrator().createEPL("create window CarWindow#keepall as CarEvent");
        epService.getEPAdministrator().createEPL("insert into CarWindow select * from CarEvent");

        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "france", 10000));
        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "germany", 5000));
        epService.getEPRuntime().sendEvent(new CarEvent("bmw", "france", 100));
        epService.getEPRuntime().sendEvent(new CarEvent("bmw", "germany", 1000));
        epService.getEPRuntime().sendEvent(new CarEvent("opel", "france", 7000));
        epService.getEPRuntime().sendEvent(new CarEvent("opel", "germany", 7000));

        String epl = "select name, place, sum(count), grouping(name), grouping(place), grouping_id(name, place) as gid " +
            "from CarWindow group by grouping sets((name, place),name, place,())";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);

        assertEquals(Integer.class, result.getEventType().getPropertyType("grouping(name)"));
        assertEquals(Integer.class, result.getEventType().getPropertyType("gid"));

        String[] fields = new String[] {"name", "place", "sum(count)", "grouping(name)", "grouping(place)", "gid"};
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][] {
                {"skoda",   "france",   10000, 0, 0, 0},
                {"skoda",   "germany",      5000, 0, 0, 0},
                {"bmw",     "france",   100, 0, 0, 0},
                {"bmw",     "germany",      1000, 0, 0, 0},
                {"opel",    "france",   7000, 0, 0, 0},
                {"opel",    "germany",      7000, 0, 0, 0},
                {"skoda",   null,           15000, 0, 1, 1},
                {"bmw",     null,           1100, 0, 1, 1},
                {"opel",    null,           14000, 0, 1, 1},
                {null,      "france",   17100, 1, 0, 2},
                {null,      "germany",      13000, 1, 0, 2},
                {null,      null,           30100, 1, 1, 3}});
    }

    public void testDocSampleCarEventAndGroupingFunc() {
        epService.getEPAdministrator().getConfiguration().addEventType(CarEvent.class);

        // try simple
        String epl = "select name, place, sum(count), grouping(name), grouping(place), grouping_id(name,place) as gid " +
                "from CarEvent group by grouping sets((name, place), name, place, ())";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        runAssertionDocSampleCarEvent();
        epService.getEPAdministrator().destroyAllStatements();

        // try audit
        epService.getEPAdministrator().createEPL("@Audit " + epl).addListener(listener);
        runAssertionDocSampleCarEvent();
        epService.getEPAdministrator().destroyAllStatements();

        // try model
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmt.getText());
        stmt.addListener(listener);
        runAssertionDocSampleCarEvent();
    }

    private void runAssertionDocSampleCarEvent() {
        String[] fields = new String[] {"name", "place", "sum(count)", "grouping(name)", "grouping(place)", "gid"};
        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "france", 100));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][] {
                {"skoda",   "france",   100, 0, 0, 0},
                {"skoda",   null,       100, 0, 1, 1},
                {null,      "france",   100, 1, 0, 2},
                {null,      null,       100, 1, 1, 3}});

        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "germany", 75));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][] {
                {"skoda",   "germany",   75, 0, 0, 0},
                {"skoda",   null,       175, 0, 1, 1},
                {null,      "germany",   75, 1, 0, 2},
                {null,      null,       175, 1, 1, 3}});
    }

    public void testGroupingFuncExpressionUse() {
        GroupingSupportFunc.getParameters().clear();
        epService.getEPAdministrator().getConfiguration().addEventType(CarEvent.class);

        // test uncorrelated subquery and expression-declaration and single-row func
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("myfunc", GroupingSupportFunc.class.getName(), "myfunc");
        epService.getEPAdministrator().createEPL("create expression myExpr {x=> '|' || x.name || '|'}");
        epService.getEPAdministrator().getConfiguration().addEventType(CarInfoEvent.class);
        String epl = "select myfunc(" +
                "  name, place, sum(count), grouping(name), grouping(place), grouping_id(name, place)," +
                "  (select refId from CarInfoEvent#lastevent), " +
                "  myExpr(ce)" +
                "  )" +
                "from CarEvent ce group by grouping sets((name, place),name, place,())";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new CarInfoEvent("a", "b", "c01"));

        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "france", 10000));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][] {
                {"skoda", "france", 10000, 0, 0, 0, "c01", "|skoda|"},
                {"skoda", null, 10000, 0, 1, 1, "c01", "|skoda|"},
                {null, "france", 10000, 1, 0, 2, "c01", "|skoda|"},
                {null, null, 10000, 1, 1, 3, "c01", "|skoda|"}}, GroupingSupportFunc.assertGetAndClear(4));
        epService.getEPAdministrator().destroyAllStatements();

        // test "prev" and "prior"
        String[] fields = "c0,c1,c2,c3".split(",");
        String eplTwo = "select prev(1, name) as c0, prior(1, name) as c1, name as c2, sum(count) as c3 from CarEvent#keepall ce group by rollup(name)";
        epService.getEPAdministrator().createEPL(eplTwo).addListener(listener);

        epService.getEPRuntime().sendEvent(new CarEvent("skoda", "france", 10));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][] {
                {null, null, "skoda", 10}, {null, null, null, 10}
        });

        epService.getEPRuntime().sendEvent(new CarEvent("vw", "france", 15));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][] {
                {"skoda", "skoda", "vw", 15}, {"skoda", "skoda", null, 25}
        });
    }

    public void testInvalid() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // invalid use of function
        String expected = "Failed to validate select-clause expression 'grouping(theString)': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select grouping(theString) from SupportBean]";
        tryInvalid("select grouping(theString) from SupportBean", "Error starting statement: " + expected);
        tryInvalid("select theString, sum(intPrimitive) from SupportBean(grouping(theString) = 1) group by rollup(theString)",
                "Failed to validate filter expression 'grouping(theString)=1': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean(grouping(theString) = 1) group by rollup(theString)]");
        tryInvalid("select theString, sum(intPrimitive) from SupportBean where grouping(theString) = 1 group by rollup(theString)",
                "Failed to validate filter expression 'grouping(theString)=1': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean where grouping(theString) = 1 group by rollup(theString)]");
        tryInvalid("select theString, sum(intPrimitive) from SupportBean group by rollup(grouping(theString))",
                "Error starting statement: The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean group by rollup(grouping(theString))]");

        // invalid parameters
        tryInvalid("select theString, sum(intPrimitive), grouping(longPrimitive) from SupportBean group by rollup(theString)",
                "Error starting statement: Group-by with rollup requires a fully-aggregated query, the query is not full-aggregated because of property 'longPrimitive' [select theString, sum(intPrimitive), grouping(longPrimitive) from SupportBean group by rollup(theString)]");
        tryInvalid("select theString, sum(intPrimitive), grouping(theString||'x') from SupportBean group by rollup(theString)",
                "Error starting statement: Failed to find expression 'theString||\"x\"' among group-by expressions [select theString, sum(intPrimitive), grouping(theString||'x') from SupportBean group by rollup(theString)]");

        tryInvalid("select theString, sum(intPrimitive), grouping_id(theString, theString) from SupportBean group by rollup(theString)",
                "Error starting statement: Duplicate expression 'theString' among grouping function parameters [select theString, sum(intPrimitive), grouping_id(theString, theString) from SupportBean group by rollup(theString)]");
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            if (!ex.getMessage().startsWith(message) || message.trim().isEmpty()) {
                fail("\nExpected: " + message + "\nReceived: " + ex.getMessage());
            }
        }
    }

    public static class GroupingSupportFunc {
        private static List<Object[]> parameters = new ArrayList<Object[]>();

        public static void myfunc(String name,
                                  String place,
                                  Integer cnt,
                                  Integer grpName,
                                  Integer grpPlace,
                                  Integer grpId,
                                  String refId,
                                  String namePlusDelim) {
            parameters.add(new Object[] {name, place, cnt, grpName, grpPlace, grpId, refId, namePlusDelim});
        }

        public static List<Object[]> getParameters() {
            return parameters;
        }

        public static Object[][] assertGetAndClear(int numRows) {
            assertEquals(numRows, parameters.size());
            Object[][] result = parameters.toArray(new Object[numRows][]);
            parameters.clear();
            return result;
        }
    }

    private static class CarInfoEvent {
        private final String name;
        private final String place;
        private final String refId;

        private CarInfoEvent(String name, String place, String refId) {
            this.name = name;
            this.place = place;
            this.refId = refId;
        }

        public String getName() {
            return name;
        }

        public String getPlace() {
            return place;
        }

        public String getRefId() {
            return refId;
        }
    }

    private static class CarEvent {
        private final String name;
        private final String place;
        private final int count;

        private CarEvent(String name, String place, int count) {
            this.name = name;
            this.place = place;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public String getPlace() {
            return place;
        }

        public int getCount() {
            return count;
        }
    }
}
