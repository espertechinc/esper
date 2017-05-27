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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecSubselectWithinPattern implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionSubqueryAgainstNamedWindowInUDFInPattern(epService);
        runAssertionFilterPatternNamedWindowNoAlias(epService);
        runAssertionCorrelated(epService);
        runAssertionAggregation(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

        tryInvalid(epService, "select * from S0(exists (select * from S1))",
                "Failed to validate subquery number 1 querying S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window [select * from S0(exists (select * from S1))]");

        epService.getEPAdministrator().createEPL("create window MyWindowInvalid#lastevent as select * from S0");
        tryInvalid(epService, "select * from S0(exists (select * from MyWindowInvalid#lastevent))",
                "Failed to validate subquery number 1 querying MyWindowInvalid: Consuming statements to a named window cannot declare a data window view onto the named window [select * from S0(exists (select * from MyWindowInvalid#lastevent))]");

        tryInvalid(epService, "select * from S0(id in ((select p00 from MyWindowInvalid)))",
                "Failed to validate filter expression 'id in (subselect_1)': Implicit conversion not allowed: Cannot coerce types java.lang.Integer and java.lang.String [select * from S0(id in ((select p00 from MyWindowInvalid)))]");
    }

    private void runAssertionSubqueryAgainstNamedWindowInUDFInPattern(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("supportSingleRowFunction", ExecSubselectWithinPattern.class.getName(), "supportSingleRowFunction");
        epService.getEPAdministrator().createEPL("create window MyWindowSNW#unique(p00)#keepall as S0");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern[S1(supportSingleRowFunction((select * from MyWindowSNW)))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        listener.assertInvokedAndReset();
    }

    private void runAssertionFilterPatternNamedWindowNoAlias(EPServiceProvider epService) {
        // subselect in pattern
        String stmtTextOne = "select s.id as myid from pattern [every s=S0(p00 in (select p10 from S1#lastevent))]";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);
        tryAssertion(epService, listener);
        stmtOne.destroy();

        // subselect in filter
        String stmtTextTwo = "select id as myid from S0(p00 in (select p10 from S1#lastevent))";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        stmtTwo.addListener(listener);
        tryAssertion(epService, listener);
        stmtTwo.destroy();

        // subselect in filter with named window
        EPStatement stmtNamedThree = epService.getEPAdministrator().createEPL("create window MyS1Window#lastevent as select * from S1");
        EPStatement stmtInsertThree = epService.getEPAdministrator().createEPL("insert into MyS1Window select * from S1");
        String stmtTextThree = "select id as myid from S0(p00 in (select p10 from MyS1Window))";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtTextThree);
        stmtThree.addListener(listener);
        tryAssertion(epService, listener);
        stmtThree.destroy();
        stmtInsertThree.destroy();
        stmtNamedThree.destroy();

        // subselect in pattern with named window
        EPStatement stmtNamedFour = epService.getEPAdministrator().createEPL("create window MyS1Window#lastevent as select * from S1");
        EPStatement stmtInsertFour = epService.getEPAdministrator().createEPL("insert into MyS1Window select * from S1");
        String stmtTextFour = "select s.id as myid from pattern [every s=S0(p00 in (select p10 from MyS1Window))]";
        EPStatement stmtFour = epService.getEPAdministrator().createEPL(stmtTextFour);
        stmtFour.addListener(listener);
        tryAssertion(epService, listener);
        stmtFour.destroy();
        stmtInsertFour.destroy();
        stmtNamedFour.destroy();
    }

    private void runAssertionCorrelated(EPServiceProvider epService) {

        String stmtTextTwo = "select sp1.id as myid from pattern[every sp1=S0(exists (select * from S1#keepall as stream1 where stream1.p10 = sp1.p00))]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);
        tryAssertionCorrelated(epService, listener);
        stmtTwo.destroy();

        String stmtTextOne = "select id as myid from S0(exists (select stream1.id from S1#keepall as stream1 where stream1.p10 = stream0.p00)) as stream0";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);
        stmtOne.addListener(listener);
        tryAssertionCorrelated(epService, listener);
        stmtOne.destroy();

        // Correlated across two matches
        String stmtTextThree = "select sp0.p00||'+'||sp1.p10 as myid from pattern[" +
                "every sp0=S0 -> sp1=S1(p11 = (select stream2.p21 from S2#keepall as stream2 where stream2.p20 = sp0.p00))]";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtTextThree);
        stmtThree.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S2(21, "X", "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(22, "Y", "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(23, "Z", "C"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "Y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "C"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(4, "B", "B"));
        assertEquals("Y+B", listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(4, "B", "C"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(5, "C", "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(6, "X", "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(7, "A", "C"));
        assertFalse(listener.getAndClearIsInvoked());

        stmtThree.destroy();
    }

    private void runAssertionAggregation(EPServiceProvider epService) {

        String stmtText = "select * from S0(id = (select sum(id) from S1#length(2)))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));  // now at 4
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));  // now at 13 (length window 2)
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(13));
        assertTrue(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void tryAssertionCorrelated(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(4, "C"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "C"));
        assertEquals(5, listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "A"));
        assertEquals(6, listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(7, "D"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(8, "E"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(9, "C"));
        assertEquals(9, listener.assertOneGetNewAndReset().get("myid"));
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "B"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(4, "C"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "C"));
        assertEquals(5, listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(7, "D"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(8, "E"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(9, "C"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E"));
        assertEquals(10, listener.assertOneGetNewAndReset().get("myid"));
    }

    public static boolean supportSingleRowFunction(Object... v) {
        return true;
    }
}