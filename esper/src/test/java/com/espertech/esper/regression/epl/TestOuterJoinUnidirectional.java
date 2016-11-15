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
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestOuterJoinUnidirectional extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testUnidirectionalOuterJoin() {
        for (Class clazz : new Class[] {SupportBean_A.class, SupportBean_B.class, SupportBean_C.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        // all: unidirectional and full-outer-join
        runAssertion2Stream();
        runAssertion3Stream();
        runAssertion3StreamMixed();
        runAssertion4StreamWhereClause();

        // no-view-declared
        SupportMessageAssertUtil.tryInvalid(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B#keepall() unidirectional",
                "Error starting statement: The unidirectional keyword requires that no views are declared onto the stream (applies to stream 1)");

        // not-all-unidirectional
        SupportMessageAssertUtil.tryInvalid(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional full outer join SupportBean_C#keepall()",
                "Error starting statement: The unidirectional keyword must either apply to a single stream or all streams in a full outer join");

        // no iterate
        SupportMessageAssertUtil.tryInvalidIterate(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional",
                "Iteration over a unidirectional join is not supported");
    }

    private void runAssertion2Stream() {
        for (Class clazz : new Class[] {SupportBean_A.class, SupportBean_B.class, SupportBean_C.class, SupportBean_D.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        epService.getEPAdministrator().createEPL("select a.id as aid, b.id as bid from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertReceived2Stream("A1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertReceived2Stream(null, "B1");

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        assertReceived2Stream(null, "B2");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        assertReceived2Stream("A2", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3Stream() {
        runAssertion3StreamAllUnidirectional(false);
        runAssertion3StreamAllUnidirectional(true);
    }

    private void runAssertion3StreamAllUnidirectional(boolean soda) {

        String epl = "select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional";
        SupportModelHelper.createByCompileOrParse(epService, soda, epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertReceived3Stream("A1", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));
        assertReceived3Stream(null, null, "C1");

        epService.getEPRuntime().sendEvent(new SupportBean_C("C2"));
        assertReceived3Stream(null, null, "C2");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        assertReceived3Stream("A2", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertReceived3Stream(null, "B1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        assertReceived3Stream(null, "B2", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3StreamMixed() {
        epService.getEPAdministrator().createEPL("create window MyCWindow#keepall() as SupportBean_C");
        epService.getEPAdministrator().createEPL("insert into MyCWindow select * from SupportBean_C");
        String epl = "select a.id as aid, b.id as bid, MyCWindow.id as cid, SupportBean_D.id as did " +
                "from pattern[every a=SupportBean_A -> b=SupportBean_B] t1 unidirectional " +
                "full outer join " +
                "MyCWindow unidirectional " +
                "full outer join " +
                "SupportBean_D unidirectional";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_C("c1"));
        assertReceived3StreamMixed(null, null, "c1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_A("a1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("b1"));
        assertReceived3StreamMixed("a1", "b1", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_A("a2"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("b2"));
        assertReceived3StreamMixed("a2", "b2", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_D("d1"));
        assertReceived3StreamMixed(null, null, null, "d1");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion4StreamWhereClause() {
        String epl = "select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional " +
                "full outer join SupportBean_D as d unidirectional " +
                "where coalesce(a.id,b.id,c.id,d.id) in ('YES')";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        sendAssert(new SupportBean_A("A1"), false);
        sendAssert(new SupportBean_A("YES"), true);
        sendAssert(new SupportBean_C("YES"), true);
        sendAssert(new SupportBean_C("C1"), false);
        sendAssert(new SupportBean_D("YES"), true);
        sendAssert(new SupportBean_B("YES"), true);
        sendAssert(new SupportBean_B("B1"), false);
    }

    private void sendAssert(SupportBeanBase event, boolean b) {
        epService.getEPRuntime().sendEvent(event);
        assertEquals(b, listener.getAndClearIsInvoked());
    }

    private void assertReceived2Stream(String a, String b) {
        String[] fields = "aid,bid".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {a, b});
    }

    private void assertReceived3Stream(String a, String b, String c) {
        String[] fields = "a.id,b.id,c.id".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {a, b, c});
    }

    private void assertReceived3StreamMixed(String a, String b, String c, String d) {
        String[] fields = "aid,bid,cid,did".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {a, b, c, d});
    }
}
