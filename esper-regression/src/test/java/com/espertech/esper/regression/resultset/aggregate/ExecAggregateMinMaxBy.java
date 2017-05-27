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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;

public class ExecAggregateMinMaxBy implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionGroupedSortedMinMax(epService);
        runAssertionMinByMaxByOverWindow(epService);
        runAssertionNoAlias(epService);
        runAssertionMultipleOverlappingCategories(epService);
        runAssertionMultipleCriteria(epService);
        runAssertionNoDataWindow(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionGroupedSortedMinMax(EPServiceProvider epService) {
        String epl = "select " +
                "window(*) as c0, " +
                "sorted(intPrimitive desc) as c1, " +
                "sorted(intPrimitive asc) as c2, " +
                "maxby(intPrimitive) as c3, " +
                "minby(intPrimitive) as c4, " +
                "maxbyever(intPrimitive) as c5, " +
                "minbyever(intPrimitive) as c6 " +
                "from SupportBean#groupwin(longPrimitive)#length(3) " +
                "group by longPrimitive";
        EPStatement stmtPlain = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtPlain.addListener(listener);

        tryAssertionGroupedSortedMinMax(epService, listener);
        stmtPlain.destroy();

        // test SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtSoda = epService.getEPAdministrator().create(model);
        stmtSoda.addListener(listener);
        assertEquals(epl, stmtSoda.getText());
        tryAssertionGroupedSortedMinMax(epService, listener);
        stmtSoda.destroy();

        // test join
        String eplJoin = "select " +
                "window(sb.*) as c0, " +
                "sorted(intPrimitive desc) as c1, " +
                "sorted(intPrimitive asc) as c2, " +
                "maxby(intPrimitive) as c3, " +
                "minby(intPrimitive) as c4, " +
                "maxbyever(intPrimitive) as c5, " +
                "minbyever(intPrimitive) as c6 " +
                "from S0#lastevent, SupportBean#groupwin(longPrimitive)#length(3) as sb " +
                "group by longPrimitive";
        EPStatement stmtJoin = epService.getEPAdministrator().createEPL(eplJoin);
        stmtJoin.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "p00"));
        tryAssertionGroupedSortedMinMax(epService, listener);
        stmtJoin.destroy();

        // test join multirow
        String[] fields = "c0".split(",");
        String joinMultirow = "select sorted(intPrimitive desc) as c0 from S0#keepall, SupportBean#length(2)";
        EPStatement stmtJoinMultirow = epService.getEPAdministrator().createEPL(joinMultirow);
        stmtJoinMultirow.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S3"));

        SupportBean eventOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventOne}});

        SupportBean eventTwo = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventTwo, eventOne}});

        SupportBean eventThree = new SupportBean("E3", 0);
        epService.getEPRuntime().sendEvent(eventThree);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventTwo, eventThree}});

        stmtJoinMultirow.destroy();
    }

    private void tryAssertionGroupedSortedMinMax(EPServiceProvider epService, SupportUpdateListener listener) {

        String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
        SupportBean eventOne = makeEvent("E1", 1, 1);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventOne},
                    new Object[]{eventOne},
                    new Object[]{eventOne},
                    eventOne, eventOne, eventOne, eventOne});

        SupportBean eventTwo = makeEvent("E2", 2, 1);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventOne, eventTwo},
                    new Object[]{eventTwo, eventOne},
                    new Object[]{eventOne, eventTwo},
                    eventTwo, eventOne, eventTwo, eventOne});

        SupportBean eventThree = makeEvent("E3", 0, 1);
        epService.getEPRuntime().sendEvent(eventThree);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventOne, eventTwo, eventThree},
                    new Object[]{eventTwo, eventOne, eventThree},
                    new Object[]{eventThree, eventOne, eventTwo},
                    eventTwo, eventThree, eventTwo, eventThree});

        SupportBean eventFour = makeEvent("E4", 3, 1);   // pushes out E1
        epService.getEPRuntime().sendEvent(eventFour);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventTwo, eventThree, eventFour},
                    new Object[]{eventFour, eventTwo, eventThree},
                    new Object[]{eventThree, eventTwo, eventFour},
                    eventFour, eventThree, eventFour, eventThree});

        SupportBean eventFive = makeEvent("E5", -1, 2);   // group 2
        epService.getEPRuntime().sendEvent(eventFive);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventFive},
                    new Object[]{eventFive},
                    new Object[]{eventFive},
                    eventFive, eventFive, eventFive, eventFive});

        SupportBean eventSix = makeEvent("E6", -1, 1);   // pushes out E2
        epService.getEPRuntime().sendEvent(eventSix);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventThree, eventFour, eventSix},
                    new Object[]{eventFour, eventThree, eventSix},
                    new Object[]{eventSix, eventThree, eventFour},
                    eventFour, eventSix, eventFour, eventSix});

        SupportBean eventSeven = makeEvent("E7", 2, 2);   // group 2
        epService.getEPRuntime().sendEvent(eventSeven);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{
                    new Object[]{eventFive, eventSeven},
                    new Object[]{eventSeven, eventFive},
                    new Object[]{eventFive, eventSeven},
                    eventSeven, eventFive, eventSeven, eventFive});

    }

    private void runAssertionMinByMaxByOverWindow(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");
        String epl = "select " +
                "maxbyever(longPrimitive) as c0, " +
                "minbyever(longPrimitive) as c1, " +
                "maxby(longPrimitive).longPrimitive as c2, " +
                "maxby(longPrimitive).theString as c3, " +
                "maxby(longPrimitive).intPrimitive as c4, " +
                "maxby(longPrimitive) as c5, " +
                "minby(longPrimitive).longPrimitive as c6, " +
                "minby(longPrimitive).theString as c7, " +
                "minby(longPrimitive).intPrimitive as c8, " +
                "minby(longPrimitive) as c9 " +
                "from SupportBean#length(5)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean eventOne = makeEvent("E1", 1, 10);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{eventOne, eventOne, 10L, "E1", 1, eventOne, 10L, "E1", 1, eventOne});

        SupportBean eventTwo = makeEvent("E2", 2, 20);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{eventTwo, eventOne, 20L, "E2", 2, eventTwo, 10L, "E1", 1, eventOne});

        SupportBean eventThree = makeEvent("E3", 3, 5);
        epService.getEPRuntime().sendEvent(eventThree);
        Object[] resultThree = new Object[]{eventTwo, eventThree, 20L, "E2", 2, eventTwo, 5L, "E3", 3, eventThree};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultThree);

        SupportBean eventFour = makeEvent("E4", 4, 5);
        epService.getEPRuntime().sendEvent(eventFour); // same as E3
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultThree);

        SupportBean eventFive = makeEvent("E5", 5, 20);
        epService.getEPRuntime().sendEvent(eventFive); // same as E2
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultThree);

        SupportBean eventSix = makeEvent("E6", 6, 10);
        epService.getEPRuntime().sendEvent(eventSix); // expires E1
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultThree);

        SupportBean eventSeven = makeEvent("E7", 7, 20);
        epService.getEPRuntime().sendEvent(eventSeven); // expires E2
        Object[] resultSeven = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 5L, "E3", 3, eventThree};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultSeven);

        epService.getEPRuntime().sendEvent(makeEvent("E8", 8, 20)); // expires E3
        Object[] resultEight = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 5L, "E4", 4, eventFour};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultEight);

        epService.getEPRuntime().sendEvent(makeEvent("E9", 9, 19)); // expires E4
        Object[] resultNine = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 10L, "E6", 6, eventSix};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultNine);

        epService.getEPRuntime().sendEvent(makeEvent("E10", 10, 12)); // expires E5
        Object[] resultTen = new Object[]{eventTwo, eventThree, 20L, "E7", 7, eventSeven, 10L, "E6", 6, eventSix};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, resultTen);

        stmt.destroy();
    }

    private void runAssertionNoAlias(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "maxby(intPrimitive).theString, " +
                "minby(intPrimitive)," +
                "maxbyever(intPrimitive).theString, " +
                "minbyever(intPrimitive)," +
                "sorted(intPrimitive asc, theString desc)" +
                " from SupportBean#time(10)");

        EventPropertyDescriptor[] props = stmt.getEventType().getPropertyDescriptors();
        assertEquals("maxby(intPrimitive).theString()", props[0].getPropertyName());
        assertEquals("minby(intPrimitive)", props[1].getPropertyName());
        assertEquals("maxbyever(intPrimitive).theString()", props[2].getPropertyName());
        assertEquals("minbyever(intPrimitive)", props[3].getPropertyName());
        assertEquals("sorted(intPrimitive,theString desc)", props[4].getPropertyName());

        stmt.destroy();
    }

    private void runAssertionMultipleOverlappingCategories(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "maxbyever(intPrimitive).longPrimitive as c0," +
                "maxbyever(theString).longPrimitive as c1," +
                "minbyever(intPrimitive).longPrimitive as c2," +
                "minbyever(theString).longPrimitive as c3," +
                "maxby(intPrimitive).longPrimitive as c4," +
                "maxby(theString).longPrimitive as c5," +
                "minby(intPrimitive).longPrimitive as c6," +
                "minby(theString).longPrimitive as c7 " +
                "from SupportBean#keepall");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("C", 10, 1L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L});

        epService.getEPRuntime().sendEvent(makeEvent("P", 5, 2L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

        epService.getEPRuntime().sendEvent(makeEvent("G", 7, 3L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

        epService.getEPRuntime().sendEvent(makeEvent("A", 7, 4L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 4L, 1L, 2L, 2L, 4L});

        epService.getEPRuntime().sendEvent(makeEvent("G", 1, 5L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 5L, 4L, 1L, 2L, 5L, 4L});

        epService.getEPRuntime().sendEvent(makeEvent("X", 7, 6L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1L, 6L, 5L, 4L, 1L, 6L, 5L, 4L});

        epService.getEPRuntime().sendEvent(makeEvent("G", 100, 7L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{7L, 6L, 5L, 4L, 7L, 6L, 5L, 4L});

        epService.getEPRuntime().sendEvent(makeEvent("Z", 1000, 8L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{8L, 8L, 5L, 4L, 8L, 8L, 5L, 4L});

        stmt.destroy();
    }

    private void runAssertionMultipleCriteria(EPServiceProvider epService) {
        // test sorted multiple criteria
        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "sorted(theString desc, intPrimitive desc) as c0," +
                "sorted(theString, intPrimitive) as c1," +
                "sorted(theString asc, intPrimitive asc) as c2," +
                "sorted(theString desc, intPrimitive asc) as c3 " +
                "from SupportBean#keepall");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean eventOne = new SupportBean("C", 10);
        epService.getEPRuntime().sendEvent(eventOne);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[][]{
            new Object[]{eventOne},
            new Object[]{eventOne},
            new Object[]{eventOne},
            new Object[]{eventOne}});

        SupportBean eventTwo = new SupportBean("D", 20);
        epService.getEPRuntime().sendEvent(eventTwo);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[][]{
            new Object[]{eventTwo, eventOne},
            new Object[]{eventOne, eventTwo},
            new Object[]{eventOne, eventTwo},
            new Object[]{eventTwo, eventOne}});

        SupportBean eventThree = new SupportBean("C", 15);
        epService.getEPRuntime().sendEvent(eventThree);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[][]{
            new Object[]{eventTwo, eventThree, eventOne},
            new Object[]{eventOne, eventThree, eventTwo},
            new Object[]{eventOne, eventThree, eventTwo},
            new Object[]{eventTwo, eventOne, eventThree}});

        SupportBean eventFour = new SupportBean("D", 19);
        epService.getEPRuntime().sendEvent(eventFour);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[][]{
            new Object[]{eventTwo, eventFour, eventThree, eventOne},
            new Object[]{eventOne, eventThree, eventFour, eventTwo},
            new Object[]{eventOne, eventThree, eventFour, eventTwo},
            new Object[]{eventFour, eventTwo, eventOne, eventThree}});

        stmt.destroy();

        // test min/max
        String[] fieldsTwo = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select " +
                "maxbyever(intPrimitive, theString).longPrimitive as c0," +
                "minbyever(intPrimitive, theString).longPrimitive as c1," +
                "maxbyever(theString, intPrimitive).longPrimitive as c2," +
                "minbyever(theString, intPrimitive).longPrimitive as c3," +
                "maxby(intPrimitive, theString).longPrimitive as c4," +
                "minby(intPrimitive, theString).longPrimitive as c5," +
                "maxby(theString, intPrimitive).longPrimitive as c6," +
                "minby(theString, intPrimitive).longPrimitive as c7 " +
                "from SupportBean#keepall");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("C", 10, 1L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L});

        epService.getEPRuntime().sendEvent(makeEvent("P", 5, 2L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

        epService.getEPRuntime().sendEvent(makeEvent("C", 9, 3L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 2L, 2L, 3L, 1L, 2L, 2L, 3L});

        epService.getEPRuntime().sendEvent(makeEvent("C", 11, 4L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{4L, 2L, 2L, 3L, 4L, 2L, 2L, 3L});

        epService.getEPRuntime().sendEvent(makeEvent("X", 11, 5L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{5L, 2L, 5L, 3L, 5L, 2L, 5L, 3L});

        epService.getEPRuntime().sendEvent(makeEvent("X", 0, 6L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{5L, 6L, 5L, 3L, 5L, 6L, 5L, 3L});

        stmtTwo.destroy();
    }

    private void runAssertionNoDataWindow(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "maxbyever(intPrimitive).theString as c0, " +
                "minbyever(intPrimitive).theString as c1, " +
                "maxby(intPrimitive).theString as c2, " +
                "minby(intPrimitive).theString as c3 " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", "E1", "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1", "E2", "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3", "E2", "E3"});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "E3", "E4", "E3"});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select maxBy(p00||p10) from S0#lastevent, S1#lastevent",
                "Error starting statement: Failed to validate select-clause expression 'maxby(p00||p10)': The 'maxby' aggregation function requires that any parameter expressions evaluate properties of the same stream [select maxBy(p00||p10) from S0#lastevent, S1#lastevent]");

        tryInvalid(epService, "select sorted(p00) from S0",
                "Error starting statement: Failed to validate select-clause expression 'sorted(p00)': The 'sorted' aggregation function requires that a data window is declared for the stream [select sorted(p00) from S0]");
    }

    private SupportBean makeEvent(String string, int intPrimitive, long longPrimitive) {
        SupportBean event = new SupportBean(string, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        return event;
    }
}
