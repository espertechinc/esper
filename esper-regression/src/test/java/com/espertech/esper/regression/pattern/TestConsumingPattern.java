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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.io.Serializable;

public class TestConsumingPattern extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType("A", AEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", BEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("C", CEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("D", DEvent.class);
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInvalid() {
        tryInvalid("select * from pattern @XX [A]",
                "Error in expression: Unrecognized pattern-level annotation 'XX' [select * from pattern @XX [A]]");

        String expected = "Discard-partials and suppress-matches is not supported in a joins, context declaration and on-action ";
        tryInvalid("select * from pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[A]#keepall, A#keepall",
                expected + "[select * from pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [A]#keepall, A#keepall]");

        epService.getEPAdministrator().createEPL("create window AWindow#keepall as A");
        tryInvalid("on pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[A] select * from AWindow",
                expected + "[on pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [A] select * from AWindow]");
    }

    public void testCombination() {
        for (boolean testsoda : new boolean[] {false, true}) {
            for (TargetEnum target : TargetEnum.values()) {
                runAssertionTargetCurrentMatch(testsoda, target);
                runAssertionTargetNextMatch(testsoda, target);
            }
        }

        // test order-by
        epService.getEPAdministrator().createEPL("select * from pattern @DiscardPartialsOnMatch [every a=A -> B] order by a.id desc").addListener(listener);
        epService.getEPRuntime().sendEvent(new AEvent("A1", null, null));
        epService.getEPRuntime().sendEvent(new AEvent("A2", null, null));
        epService.getEPRuntime().sendEvent(new BEvent("B1", null));
        EventBean[] events = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRow(events, "a.id".split(","), new Object[][]{{"A2"}, {"A1"}});
    }

    public void testFollowedByOp() {
        runFollowedByOp("every a1=A -> a2=A", false);
        runFollowedByOp("every a1=A -> a2=A", true);
        runFollowedByOp("every a1=A -[10]> a2=A", false);
        runFollowedByOp("every a1=A -[10]> a2=A", true);
    }

    public void testMatchUntilOp() {
        runAssertionMatchUntilBoundOp(true);
        runAssertionMatchUntilBoundOp(false);
        runAssertionMatchUntilWChildMatcher(true);
        runAssertionMatchUntilWChildMatcher(false);
        runAssertionMatchUntilRangeOpWTime();    // with time
    }

    public void testObserverOp() {
        String[] fields = "a.id,b.id".split(",");
        sendTime(0);
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> b=B -> timer:interval(a.mysec)]").addListener(listener);
        sendAEvent("A1", 5);    // 5 seconds for this one
        sendAEvent("A2", 1);    // 1 seconds for this one
        sendBEvent("B1");
        sendTime(1000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1"});

        sendTime(5000);
        assertFalse(listener.isInvoked());
    }

    public void testAndOp() {
        runAndWAndState(true);
        runAndWAndState(false);
        runAndWChild(true);
        runAndWChild(false);
    }

    public void testNotOpNotImpacted() {
        String[] fields = "a.id".split(",");
        sendTime(0);
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> timer:interval(a.mysec) and not (B -> C)]").addListener(listener);
        sendAEvent("A1", 5); // 5 sec
        sendAEvent("A2", 1); // 1 sec
        sendBEvent("B1");
        sendTime(1000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2"});

        sendCEvent("C1", null);
        sendTime(5000);
        assertFalse(listener.isInvoked());
    }

    public void testGuardOp() {
        runGuardOpBeginState(true);
        runGuardOpBeginState(false);
        runGuardOpChildState(true);
        runGuardOpChildState(false);
    }

    public void testOrOp() {
        String[] fields = "a.id,b.id,c.id".split(",");
        sendTime(0);
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> (b=B -> c=C(pc=a.pa)) or timer:interval(1000)]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C1", "x");
        assertFalse(listener.isInvoked());
    }

    public void testEveryOp() {
        runAssertionEveryBeginState("");
        runAssertionEveryBeginState("-distinct(id)");
        runAssertionEveryBeginState("-distinct(id, 10 seconds)");

        runAssertionEveryChildState("", true);
        runAssertionEveryChildState("", false);
        runAssertionEveryChildState("-distinct(id)", true);
        runAssertionEveryChildState("-distinct(id)", false);
        runAssertionEveryChildState("-distinct(id, 10 seconds)", true);
        runAssertionEveryChildState("-distinct(id, 10 seconds)", false);
    }

    private void runAssertionEveryChildState(String everySuffix, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> every" + everySuffix + " (b=B -> c=C(pc=a.pa))]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEveryBeginState(String distinct) {
        String[] fields = "a.id,b.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
                "every a=A -> every" + distinct + " b=B]").addListener(listener);
        sendAEvent("A1");
        sendBEvent("B1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1"});

        sendBEvent("B2");
        assertFalse(listener.isInvoked());

        sendAEvent("A2");
        sendBEvent("B3");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B3"});

        sendBEvent("B4");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTime(long msec) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }

    private void sendAEvent(String id) {
        sendAEvent(id, null, null);
    }

    private void sendAEvent(String id, String pa) {
        sendAEvent(id, pa, null);
    }

    private void sendDEvent(String id) {
        epService.getEPRuntime().sendEvent(new DEvent(id));
    }

    private void sendAEvent(String id, int mysec) {
        sendAEvent(id, null, mysec);
    }

    private void sendAEvent(String id, String pa, Integer mysec) {
        epService.getEPRuntime().sendEvent(new AEvent(id, pa, mysec));
    }

    private void sendBEvent(String id) {
        sendBEvent(id, null);
    }

    private void sendBEvent(String id, String pb) {
        epService.getEPRuntime().sendEvent(new BEvent(id, pb));
    }

    private void sendCEvent(String id, String pc) {
        epService.getEPRuntime().sendEvent(new CEvent(id, pc));
    }

    private void runFollowedByOp(String pattern, boolean matchDiscard)
    {
        String[] fields = "a1.id,a2.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern "
                + (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" + pattern + "]").addListener(listener);

        sendAEvent("E1");
        sendAEvent("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", "E2"});

        sendAEvent("E3");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", "E3"});
        }
        sendAEvent("E4");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E3", "E4"});

        sendAEvent("E5");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E4", "E5"});
        }
        sendAEvent("E6");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E5", "E6"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTargetNextMatch(boolean testSoda, TargetEnum target) {

        String[] fields = "a.id,b.id,c.id".split(",");
        String epl = "select * from pattern " + target.getText() + "[every a=A -> b=B -> c=C(pc=a.pa)]";
        if (testSoda) {
            EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
            assertEquals(epl, model.toEPL());
            epService.getEPAdministrator().create(model).addListener(listener);
        }
        else {
            epService.getEPAdministrator().createEPL(epl).addListener(listener);
        }

        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.NONE) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        else {
            assertFalse(listener.isInvoked());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMatchUntilBoundOp(boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,b[1].id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
                "every a=A -> [2] b=B(pb in (a.pa, '-'))]").addListener(listener);

        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1", "-");  // applies to both matches
        sendBEvent("B2", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "B2"});

        sendBEvent("B3", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "B3"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMatchUntilWChildMatcher(boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,c[0].id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> [1] (b=B -> c=C(pc=a.pa))]").addListener(listener);

        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMatchUntilRangeOpWTime() {
        String[] fields = "a1.id,aarr[0].id".split(",");
        sendTime(0);
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
                "every a1=A -> ([:100] aarr=A until (timer:interval(10 sec) and not b=B))]").addListener(listener);

        sendAEvent("A1");
        sendTime(1000);
        sendAEvent("A2");
        sendTime(10000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2"});

        sendTime(11000);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTargetCurrentMatch(boolean testSoda, TargetEnum target) {

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "a1.id,aarr[0].id,b.id".split(",");
        String epl = "select * from pattern " + target.getText() + "[every a1=A -> [:10] aarr=A until b=B]";
        if (testSoda) {
            EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
            assertEquals(epl, model.toEPL());
            epService.getEPAdministrator().create(model).addListener(listener);
        }
        else {
            epService.getEPAdministrator().createEPL(epl).addListener(listener);
        }

        sendAEvent("A1");
        sendAEvent("A2");
        sendBEvent("B1");

        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.DISCARD_AND_SUPPRESS) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", "B1"});
        }
        else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields,
                    new Object[][]{{"A1", "A2", "B1"}, {"A2", null, "B1"}});
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAndWAndState(boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> b=B and c=C(pc=a.pa)]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAndWChild(boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> D and (b=B -> c=C(pc=a.pa))]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendDEvent("D1");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runGuardOpBeginState(boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
                "every a=A -> b=B -> c=C(pc=a.pa) where timer:within(1)]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runGuardOpChildState(boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> (b=B -> c=C(pc=a.pa)) where timer:within(1)]").addListener(listener);
        sendAEvent("A1", "x");
        sendAEvent("A2", "y");
        sendBEvent("B1");
        sendCEvent("C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent("C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch(EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
        try {
            epService.getEPAdministrator().create(epService.getEPAdministrator().compileEPL(epl));
            fail();
        }
        catch(EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private static class AEvent implements Serializable {
        private final String id;
        private final String pa;
        private final Integer mysec;

        private AEvent(String id, String pa, Integer mysec) {
            this.id = id;
            this.pa = pa;
            this.mysec = mysec;
        }

        public String getId() {
            return id;
        }

        public String getPa() {
            return pa;
        }

        public Integer getMysec() {
            return mysec;
        }
    }

    private static class BEvent implements Serializable {
        private final String id;
        private final String pb;

        private BEvent(String id, String pb) {
            this.id = id;
            this.pb = pb;
        }

        public String getId() {
            return id;
        }

        public String getPb() {
            return pb;
        }
    }

    private static class CEvent implements Serializable {
        private final String id;
        private final String pc;

        private CEvent(String id, String pc) {
            this.id = id;
            this.pc = pc;
        }

        public String getId() {
            return id;
        }

        public String getPc() {
            return pc;
        }
    }

    private static class DEvent implements Serializable {
        private final String id;

        private DEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private enum TargetEnum {
        DISCARD_ONLY("@DiscardPartialsOnMatch "),
        DISCARD_AND_SUPPRESS("@DiscardPartialsOnMatch @SuppressOverlappingMatches "),
        SUPPRESS_ONLY("@SuppressOverlappingMatches "),
        NONE("");

        private String text;

        private TargetEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}
