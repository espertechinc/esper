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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.Serializable;

import static org.junit.Assert.*;

public class ExecPatternConsumingPattern implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("A", AEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", BEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("C", CEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("D", DEvent.class);

        runAssertionInvalid(epService);
        runAssertionCombination(epService);
        runAssertionFollowedByOp(epService);
        runAssertionMatchUntilOp(epService);
        runAssertionObserverOp(epService);
        runAssertionAndOp(epService);
        runAssertionNotOpNotImpacted(epService);
        runAssertionGuardOp(epService);
        runAssertionOrOp(epService);
        runAssertionEveryOp(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from pattern @XX [A]",
                "Error in expression: Unrecognized pattern-level annotation 'XX' [select * from pattern @XX [A]]");

        String expected = "Discard-partials and suppress-matches is not supported in a joins, context declaration and on-action ";
        tryInvalid(epService, "select * from pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[A]#keepall, A#keepall",
                expected + "[select * from pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [A]#keepall, A#keepall]");

        epService.getEPAdministrator().createEPL("create window AWindow#keepall as A");
        tryInvalid(epService, "on pattern " + TargetEnum.DISCARD_AND_SUPPRESS.getText() + "[A] select * from AWindow",
                expected + "[on pattern @DiscardPartialsOnMatch @SuppressOverlappingMatches [A] select * from AWindow]");
    }

    private void runAssertionCombination(EPServiceProvider epService) {
        for (boolean testsoda : new boolean[]{false, true}) {
            for (TargetEnum target : TargetEnum.values()) {
                tryAssertionTargetCurrentMatch(epService, testsoda, target);
                tryAssertionTargetNextMatch(epService, testsoda, target);
            }
        }

        // test order-by
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern @DiscardPartialsOnMatch [every a=A -> B] order by a.id desc").addListener(listener);
        epService.getEPRuntime().sendEvent(new AEvent("A1", null, null));
        epService.getEPRuntime().sendEvent(new AEvent("A2", null, null));
        epService.getEPRuntime().sendEvent(new BEvent("B1", null));
        EventBean[] events = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRow(events, "a.id".split(","), new Object[][]{{"A2"}, {"A1"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFollowedByOp(EPServiceProvider epService) {
        runFollowedByOp(epService, "every a1=A -> a2=A", false);
        runFollowedByOp(epService, "every a1=A -> a2=A", true);
        runFollowedByOp(epService, "every a1=A -[10]> a2=A", false);
        runFollowedByOp(epService, "every a1=A -[10]> a2=A", true);
    }

    private void runAssertionMatchUntilOp(EPServiceProvider epService) {
        tryAssertionMatchUntilBoundOp(epService, true);
        tryAssertionMatchUntilBoundOp(epService, false);
        tryAssertionMatchUntilWChildMatcher(epService, true);
        tryAssertionMatchUntilWChildMatcher(epService, false);
        tryAssertionMatchUntilRangeOpWTime(epService);    // with time
    }

    private void runAssertionObserverOp(EPServiceProvider epService) {
        String[] fields = "a.id,b.id".split(",");
        sendTime(epService, 0);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> b=B -> timer:interval(a.mysec)]").addListener(listener);
        sendAEvent(epService, "A1", 5);    // 5 seconds for this one
        sendAEvent(epService, "A2", 1);    // 1 seconds for this one
        sendBEvent(epService, "B1");
        sendTime(epService, 1000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1"});

        sendTime(epService, 5000);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAndOp(EPServiceProvider epService) {
        runAndWAndState(epService, true);
        runAndWAndState(epService, false);
        runAndWChild(epService, true);
        runAndWChild(epService, false);
    }

    private void runAssertionNotOpNotImpacted(EPServiceProvider epService) {
        String[] fields = "a.id".split(",");
        sendTime(epService, 0);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> timer:interval(a.mysec) and not (B -> C)]").addListener(listener);
        sendAEvent(epService, "A1", 5); // 5 sec
        sendAEvent(epService, "A2", 1); // 1 sec
        sendBEvent(epService, "B1");
        sendTime(epService, 1000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2"});

        sendCEvent(epService, "C1", null);
        sendTime(epService, 5000);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGuardOp(EPServiceProvider epService) {
        runGuardOpBeginState(epService, true);
        runGuardOpBeginState(epService, false);
        runGuardOpChildState(epService, true);
        runGuardOpChildState(epService, false);
    }

    private void runAssertionOrOp(EPServiceProvider epService) {
        String[] fields = "a.id,b.id,c.id".split(",");
        sendTime(epService, 0);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + " [" +
                "every a=A -> (b=B -> c=C(pc=a.pa)) or timer:interval(1000)]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C1", "x");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEveryOp(EPServiceProvider epService) {
        tryAssertionEveryBeginState(epService, "");
        tryAssertionEveryBeginState(epService, "-distinct(id)");
        tryAssertionEveryBeginState(epService, "-distinct(id, 10 seconds)");

        tryAssertionEveryChildState(epService, "", true);
        tryAssertionEveryChildState(epService, "", false);
        tryAssertionEveryChildState(epService, "-distinct(id)", true);
        tryAssertionEveryChildState(epService, "-distinct(id)", false);
        tryAssertionEveryChildState(epService, "-distinct(id, 10 seconds)", true);
        tryAssertionEveryChildState(epService, "-distinct(id, 10 seconds)", false);
    }

    private void tryAssertionEveryChildState(EPServiceProvider epService, String everySuffix, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> every" + everySuffix + " (b=B -> c=C(pc=a.pa))]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionEveryBeginState(EPServiceProvider epService, String distinct) {
        String[] fields = "a.id,b.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
                "every a=A -> every" + distinct + " b=B]").addListener(listener);
        sendAEvent(epService, "A1");
        sendBEvent(epService, "B1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1"});

        sendBEvent(epService, "B2");
        assertFalse(listener.isInvoked());

        sendAEvent(epService, "A2");
        sendBEvent(epService, "B3");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B3"});

        sendBEvent(epService, "B4");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runFollowedByOp(EPServiceProvider epService, String pattern, boolean matchDiscard) {
        String[] fields = "a1.id,a2.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern "
                + (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" + pattern + "]").addListener(listener);

        sendAEvent(epService, "E1");
        sendAEvent(epService, "E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        sendAEvent(epService, "E3");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3"});
        }
        sendAEvent(epService, "E4");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4"});

        sendAEvent(epService, "E5");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "E5"});
        }
        sendAEvent(epService, "E6");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", "E6"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTargetNextMatch(EPServiceProvider epService, boolean testSoda, TargetEnum target) {

        String[] fields = "a.id,b.id,c.id".split(",");
        String epl = "select * from pattern " + target.getText() + "[every a=A -> b=B -> c=C(pc=a.pa)]";
        SupportUpdateListener listener = new SupportUpdateListener();
        if (testSoda) {
            EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
            assertEquals(epl, model.toEPL());
            epService.getEPAdministrator().create(model).addListener(listener);
        } else {
            epService.getEPAdministrator().createEPL(epl).addListener(listener);
        }

        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.NONE) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        } else {
            assertFalse(listener.isInvoked());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionMatchUntilBoundOp(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,b[1].id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
                "every a=A -> [2] b=B(pb in (a.pa, '-'))]").addListener(listener);

        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1", "-");  // applies to both matches
        sendBEvent(epService, "B2", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "B2"});

        sendBEvent(epService, "B3", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "B3"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionMatchUntilWChildMatcher(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b[0].id,c[0].id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> [1] (b=B -> c=C(pc=a.pa))]").addListener(listener);

        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionMatchUntilRangeOpWTime(EPServiceProvider epService) {
        String[] fields = "a1.id,aarr[0].id".split(",");
        sendTime(epService, 0);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " + TargetEnum.DISCARD_ONLY.getText() + "[" +
                "every a1=A -> ([:100] aarr=A until (timer:interval(10 sec) and not b=B))]").addListener(listener);

        sendAEvent(epService, "A1");
        sendTime(epService, 1000);
        sendAEvent(epService, "A2");
        sendTime(epService, 10000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2"});

        sendTime(epService, 11000);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTargetCurrentMatch(EPServiceProvider epService, boolean testSoda, TargetEnum target) {

        SupportUpdateListener listener = new SupportUpdateListener();
        String[] fields = "a1.id,aarr[0].id,b.id".split(",");
        String epl = "select * from pattern " + target.getText() + "[every a1=A -> [:10] aarr=A until b=B]";
        if (testSoda) {
            EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
            assertEquals(epl, model.toEPL());
            epService.getEPAdministrator().create(model).addListener(listener);
        } else {
            epService.getEPAdministrator().createEPL(epl).addListener(listener);
        }

        sendAEvent(epService, "A1");
        sendAEvent(epService, "A2");
        sendBEvent(epService, "B1");

        if (target == TargetEnum.SUPPRESS_ONLY || target == TargetEnum.DISCARD_AND_SUPPRESS) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", "B1"});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields,
                    new Object[][]{{"A1", "A2", "B1"}, {"A2", null, "B1"}});
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAndWAndState(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> b=B and c=C(pc=a.pa)]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAndWChild(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> D and (b=B -> c=C(pc=a.pa))]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendDEvent(epService, "D1");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runGuardOpBeginState(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + "[" +
                "every a=A -> b=B -> c=C(pc=a.pa) where timer:within(1)]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runGuardOpChildState(EPServiceProvider epService, boolean matchDiscard) {
        String[] fields = "a.id,b.id,c.id".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from pattern " +
                (matchDiscard ? TargetEnum.DISCARD_ONLY.getText() : "") + " [" +
                "every a=A -> (b=B -> c=C(pc=a.pa)) where timer:within(1)]").addListener(listener);
        sendAEvent(epService, "A1", "x");
        sendAEvent(epService, "A2", "y");
        sendBEvent(epService, "B1");
        sendCEvent(epService, "C1", "y");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "C1"});

        sendCEvent(epService, "C2", "x");
        if (matchDiscard) {
            assertFalse(listener.isInvoked());
        } else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C2"});
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryInvalid(EPServiceProvider epService, String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
        try {
            epService.getEPAdministrator().create(epService.getEPAdministrator().compileEPL(epl));
            fail();
        } catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendTime(EPServiceProvider epService, long msec) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }

    private void sendAEvent(EPServiceProvider epService, String id) {
        sendAEvent(id, null, null, epService);
    }

    private void sendAEvent(EPServiceProvider epService, String id, String pa) {
        sendAEvent(id, pa, null, epService);
    }

    private void sendDEvent(EPServiceProvider epService, String id) {
        epService.getEPRuntime().sendEvent(new DEvent(id));
    }

    private void sendAEvent(EPServiceProvider epService, String id, int mysec) {
        sendAEvent(id, null, mysec, epService);
    }

    private void sendAEvent(String id, String pa, Integer mysec, EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new AEvent(id, pa, mysec));
    }

    private void sendBEvent(EPServiceProvider epService, String id) {
        sendBEvent(epService, id, null);
    }

    private void sendBEvent(EPServiceProvider epService, String id, String pb) {
        epService.getEPRuntime().sendEvent(new BEvent(id, pb));
    }

    private void sendCEvent(EPServiceProvider epService, String id, String pc) {
        epService.getEPRuntime().sendEvent(new CEvent(id, pc));
    }

    public static class AEvent implements Serializable {
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

    public static class BEvent implements Serializable {
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

    public static class CEvent implements Serializable {
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

    public static class DEvent implements Serializable {
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
