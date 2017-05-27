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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;

public class ExecOuterJoinUnidirectional implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean_A.class, SupportBean_B.class, SupportBean_C.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        // all: unidirectional and full-outer-join
        runAssertion2Stream(epService);
        runAssertion3Stream(epService);
        runAssertion3StreamMixed(epService);
        runAssertion4StreamWhereClause(epService);

        // no-view-declared
        tryInvalid(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B#keepall unidirectional",
                "Error starting statement: The unidirectional keyword requires that no views are declared onto the stream (applies to stream 1)");

        // not-all-unidirectional
        tryInvalid(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional full outer join SupportBean_C#keepall",
                "Error starting statement: The unidirectional keyword must either apply to a single stream or all streams in a full outer join");

        // no iterate
        SupportMessageAssertUtil.tryInvalidIterate(epService,
                "select * from SupportBean_A unidirectional full outer join SupportBean_B unidirectional",
                "Iteration over a unidirectional join is not supported");
    }

    private void runAssertion2Stream(EPServiceProvider epService) {
        for (Class clazz : new Class[]{SupportBean_A.class, SupportBean_B.class, SupportBean_C.class, SupportBean_D.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select a.id as aid, b.id as bid from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertReceived2Stream(listener, "A1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertReceived2Stream(listener, null, "B1");

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        assertReceived2Stream(listener, null, "B2");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        assertReceived2Stream(listener, "A2", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3Stream(EPServiceProvider epService) {
        runAssertion3StreamAllUnidirectional(epService, false);
        runAssertion3StreamAllUnidirectional(epService, true);
    }

    private void runAssertion3StreamAllUnidirectional(EPServiceProvider epService, boolean soda) {

        String epl = "select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional";
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportModelHelper.createByCompileOrParse(epService, soda, epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertReceived3Stream(listener, "A1", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));
        assertReceived3Stream(listener, null, null, "C1");

        epService.getEPRuntime().sendEvent(new SupportBean_C("C2"));
        assertReceived3Stream(listener, null, null, "C2");

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        assertReceived3Stream(listener, "A2", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertReceived3Stream(listener, null, "B1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        assertReceived3Stream(listener, null, "B2", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3StreamMixed(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyCWindow#keepall as SupportBean_C");
        epService.getEPAdministrator().createEPL("insert into MyCWindow select * from SupportBean_C");
        String epl = "select a.id as aid, b.id as bid, MyCWindow.id as cid, SupportBean_D.id as did " +
                "from pattern[every a=SupportBean_A -> b=SupportBean_B] t1 unidirectional " +
                "full outer join " +
                "MyCWindow unidirectional " +
                "full outer join " +
                "SupportBean_D unidirectional";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_C("c1"));
        assertReceived3StreamMixed(listener, null, null, "c1", null);

        epService.getEPRuntime().sendEvent(new SupportBean_A("a1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("b1"));
        assertReceived3StreamMixed(listener, "a1", "b1", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_A("a2"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("b2"));
        assertReceived3StreamMixed(listener, "a2", "b2", null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_D("d1"));
        assertReceived3StreamMixed(listener, null, null, null, "d1");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion4StreamWhereClause(EPServiceProvider epService) {
        String epl = "select * from SupportBean_A as a unidirectional " +
                "full outer join SupportBean_B as b unidirectional " +
                "full outer join SupportBean_C as c unidirectional " +
                "full outer join SupportBean_D as d unidirectional " +
                "where coalesce(a.id,b.id,c.id,d.id) in ('YES')";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        sendAssert(epService, listener, new SupportBean_A("A1"), false);
        sendAssert(epService, listener, new SupportBean_A("YES"), true);
        sendAssert(epService, listener, new SupportBean_C("YES"), true);
        sendAssert(epService, listener, new SupportBean_C("C1"), false);
        sendAssert(epService, listener, new SupportBean_D("YES"), true);
        sendAssert(epService, listener, new SupportBean_B("YES"), true);
        sendAssert(epService, listener, new SupportBean_B("B1"), false);
    }

    private void sendAssert(EPServiceProvider epService, SupportUpdateListener listener, SupportBeanBase event, boolean b) {
        epService.getEPRuntime().sendEvent(event);
        assertEquals(b, listener.getAndClearIsInvoked());
    }

    private void assertReceived2Stream(SupportUpdateListener listener, String a, String b) {
        String[] fields = "aid,bid".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{a, b});
    }

    private void assertReceived3Stream(SupportUpdateListener listener, String a, String b, String c) {
        String[] fields = "a.id,b.id,c.id".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{a, b, c});
    }

    private void assertReceived3StreamMixed(SupportUpdateListener listener, String a, String b, String c, String d) {
        String[] fields = "aid,bid,cid,did".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{a, b, c, d});
    }
}
