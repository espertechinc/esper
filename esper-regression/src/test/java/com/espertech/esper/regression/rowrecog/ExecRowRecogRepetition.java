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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.rowregex.RegexPatternExpandUtil;
import com.espertech.esper.rowregex.RowRegexExprNode;
import com.espertech.esper.rowregex.RowRegexExprNodePrecedenceEnum;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.io.StringWriter;
import java.util.*;

import static org.junit.Assert.*;

public class ExecRowRecogRepetition implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionRepeat(epService, false);
        runAssertionRepeat(epService, true);

        runAssertionPrev(epService);

        runAssertionInvalid(epService);

        runAssertionDocSamples(epService);

        runAssertionEquivalent(epService);
    }

    private void runAssertionDocSamples(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema TemperatureSensorEvent(id string, device string, temp int)");

        runDocSampleExactlyN(epService);
        runDocSampleNOrMore_and_BetweenNandM(epService, "A{2,} B");
        runDocSampleNOrMore_and_BetweenNandM(epService, "A{2,3} B");
        runDocSampleUpToN(epService);
    }

    private void runDocSampleUpToN(EPServiceProvider epService) {
        String[] fields = "a0_id,a1_id,b_id".split(",");
        String epl = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A[0].id as a0_id, A[1].id as a1_id, B.id as b_id\n" +
                "  pattern (A{,2} B)\n" +
                "  define \n" +
                "\tA as A.temp >= 100,\n" +
                "\tB as B.temp >= 102)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "1", 99}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E2", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E3", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E4", "1", 101}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E5", "1", 102}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4", "E5"});

        stmt.destroy();
    }

    private void runDocSampleNOrMore_and_BetweenNandM(EPServiceProvider epService, String pattern) {
        String[] fields = "a0_id,a1_id,a2_id,b_id".split(",");
        String epl = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A[0].id as a0_id, A[1].id as a1_id, A[2].id as a2_id, B.id as b_id\n" +
                "  pattern (" + pattern + ")\n" +
                "  define \n" +
                "\tA as A.temp >= 100,\n" +
                "\tB as B.temp >= 102)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "1", 99}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E2", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E3", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E4", "1", 101}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E5", "1", 102}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3", "E4", "E5"});

        stmt.destroy();
    }

    private void runDocSampleExactlyN(EPServiceProvider epService) {
        String[] fields = "a0_id,a1_id".split(",");
        String epl = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A[0].id as a0_id, A[1].id as a1_id\n" +
                "  pattern (A{2})\n" +
                "  define \n" +
                "\tA as A.temp >= 100)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "1", 99}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E2", "1", 100}, "TemperatureSensorEvent");

        epService.getEPRuntime().sendEvent(new Object[]{"E3", "1", 100}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3"});

        epService.getEPRuntime().sendEvent(new Object[]{"E4", "1", 101}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E5", "1", 102}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "E5"});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String template = "select * from SupportBean " +
                "match_recognize (" +
                "  measures A as a" +
                "  pattern (REPLACE) " +
                ")";
        epService.getEPAdministrator().createEPL("create variable int myvariable = 0");

        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{}"),
                "Invalid match-recognize quantifier '{}', expecting an expression");
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{null}"),
                "Error starting statement: pattern quantifier 'null' must return an integer-type value");
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{myvariable}"),
                "Error starting statement: pattern quantifier 'myvariable' must return a constant value");
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{prev(A)}"),
                "Error starting statement: Invalid match-recognize pattern expression 'prev(A)': Aggregation, sub-select, previous or prior functions are not supported in this context");

        String expected = "Error starting statement: Invalid pattern quantifier value -1, expecting a minimum of 1";
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{-1}"), expected);
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{,-1}"), expected);
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{-1,10}"), expected);
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{-1,}"), expected);
        SupportMessageAssertUtil.tryInvalid(epService, template.replaceAll("REPLACE", "A{5,3}"),
                "Error starting statement: Invalid pattern quantifier value 5, expecting a minimum of 1 and maximum of 3");
    }

    private void runAssertionPrev(EPServiceProvider epService) {
        String text = "select * from SupportBean " +
                "match_recognize (" +
                "  measures A as a" +
                "  pattern (A{3}) " +
                "  define " +
                "    A as A.intPrimitive > prev(A.intPrimitive)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent("A1", 1, epService);
        sendEvent("A2", 4, epService);
        sendEvent("A3", 2, epService);
        sendEvent("A4", 6, epService);
        sendEvent("A5", 5, epService);
        SupportBean b6 = sendEvent("A6", 6, epService);
        SupportBean b7 = sendEvent("A7", 7, epService);
        SupportBean b8 = sendEvent("A9", 8, epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{new Object[]{b6, b7, b8}});
    }

    private void runAssertionRepeat(EPServiceProvider epService, boolean soda) {
        // Atom Assertions
        //
        //

        // single-bound assertions
        runAssertionRepeatSingleBound(epService, soda);

        // defined-range assertions
        runAssertionsRepeatRange(epService, soda);

        // lower-bounds assertions
        runAssertionsUpTo(epService, soda);

        // upper-bounds assertions
        runAssertionsAtLeast(epService, soda);

        // Nested Assertions
        //
        //

        // single-bound nested assertions
        runAssertionNestedRepeatSingle(epService, soda);

        // defined-range nested assertions
        runAssertionNestedRepeatRange(epService, soda);

        // lower-bounds nested assertions
        runAssertionsNestedUpTo(epService, soda);

        // upper-bounds nested assertions
        runAssertionsNestedAtLeast(epService, soda);
    }

    private void runAssertionEquivalent(EPServiceProvider epService) throws Exception {
        //
        // Single-bounds Repeat.
        //
        runEquivalent(epService, "A{1}", "A");
        runEquivalent(epService, "A{2}", "A A");
        runEquivalent(epService, "A{3}", "A A A");
        runEquivalent(epService, "A{1} B{2}", "A B B");
        runEquivalent(epService, "A{1} B{2} C{3}", "A B B C C C");
        runEquivalent(epService, "(A{2})", "(A A)");
        runEquivalent(epService, "A?{2}", "A? A?");
        runEquivalent(epService, "A*{2}", "A* A*");
        runEquivalent(epService, "A+{2}", "A+ A+");
        runEquivalent(epService, "A??{2}", "A?? A??");
        runEquivalent(epService, "A*?{2}", "A*? A*?");
        runEquivalent(epService, "A+?{2}", "A+? A+?");
        runEquivalent(epService, "(A B){1}", "(A B)");
        runEquivalent(epService, "(A B){2}", "(A B) (A B)");
        runEquivalent(epService, "(A B)?{2}", "(A B)? (A B)?");
        runEquivalent(epService, "(A B)*{2}", "(A B)* (A B)*");
        runEquivalent(epService, "(A B)+{2}", "(A B)+ (A B)+");

        runEquivalent(epService, "A B{2} C", "A B B C");
        runEquivalent(epService, "A (B{2}) C", "A (B B) C");
        runEquivalent(epService, "(A{2}) C", "(A A) C");
        runEquivalent(epService, "A (B{2}|C{2})", "A (B B|C C)");
        runEquivalent(epService, "A{2} B{2} C{2}", "A A B B C C");
        runEquivalent(epService, "A{2} B C{2}", "A A B C C");
        runEquivalent(epService, "A B{2} C{2}", "A B B C C");

        // range bounds
        runEquivalent(epService, "A{1, 3}", "A A? A?");
        runEquivalent(epService, "A{2, 4}", "A A A? A?");
        runEquivalent(epService, "A?{1, 3}", "A? A? A?");
        runEquivalent(epService, "A*{1, 3}", "A* A* A*");
        runEquivalent(epService, "A+{1, 3}", "A+ A* A*");
        runEquivalent(epService, "A??{1, 3}", "A?? A?? A??");
        runEquivalent(epService, "A*?{1, 3}", "A*? A*? A*?");
        runEquivalent(epService, "A+?{1, 3}", "A+? A*? A*?");
        runEquivalent(epService, "(A B)?{1, 3}", "(A B)? (A B)? (A B)?");
        runEquivalent(epService, "(A B)*{1, 3}", "(A B)* (A B)* (A B)*");
        runEquivalent(epService, "(A B)+{1, 3}", "(A B)+ (A B)* (A B)*");

        // lower-only bounds
        runEquivalent(epService, "A{2,}", "A A A*");
        runEquivalent(epService, "A?{2,}", "A? A? A*");
        runEquivalent(epService, "A*{2,}", "A* A* A*");
        runEquivalent(epService, "A+{2,}", "A+ A+ A*");
        runEquivalent(epService, "A??{2,}", "A?? A?? A*?");
        runEquivalent(epService, "A*?{2,}", "A*? A*? A*?");
        runEquivalent(epService, "A+?{2,}", "A+? A+? A*?");
        runEquivalent(epService, "(A B)?{2,}", "(A B)? (A B)? (A B)*");
        runEquivalent(epService, "(A B)*{2,}", "(A B)* (A B)* (A B)*");
        runEquivalent(epService, "(A B)+{2,}", "(A B)+ (A B)+ (A B)*");

        // upper-only bounds
        runEquivalent(epService, "A{,2}", "A? A?");
        runEquivalent(epService, "A?{,2}", "A? A?");
        runEquivalent(epService, "A*{,2}", "A* A*");
        runEquivalent(epService, "A+{,2}", "A* A*");
        runEquivalent(epService, "A??{,2}", "A?? A??");
        runEquivalent(epService, "A*?{,2}", "A*? A*?");
        runEquivalent(epService, "A+?{,2}", "A*? A*?");
        runEquivalent(epService, "(A B){,2}", "(A B)? (A B)?");
        runEquivalent(epService, "(A B)?{,2}", "(A B)? (A B)?");
        runEquivalent(epService, "(A B)*{,2}", "(A B)* (A B)*");
        runEquivalent(epService, "(A B)+{,2}", "(A B)* (A B)*");

        //
        // Nested Repeat.
        //
        runEquivalent(epService, "(A B){2}", "(A B) (A B)");
        runEquivalent(epService, "(A){2}", "A A");
        runEquivalent(epService, "(A B C){3}", "(A B C) (A B C) (A B C)");
        runEquivalent(epService, "(A B){2} (C D){2}", "(A B) (A B) (C D) (C D)");
        runEquivalent(epService, "((A B){2} C){2}", "((A B) (A B) C) ((A B) (A B) C)");
        runEquivalent(epService, "((A|B){2} (C|D){2}){2}", "((A|B) (A|B) (C|D) (C|D)) ((A|B) (A|B) (C|D) (C|D))");
    }

    private void runAssertionNestedRepeatSingle(EPServiceProvider epService, boolean soda) {
        runTwiceAB(epService, soda, "(A B) (A B)");
        runTwiceAB(epService, soda, "(A B){2}");

        runAThenTwiceBC(epService, soda, "A (B C) (B C)");
        runAThenTwiceBC(epService, soda, "A (B C){2}");
    }

    private void runAssertionNestedRepeatRange(EPServiceProvider epService, boolean soda) {
        runOnceOrTwiceABThenC(epService, soda, "(A B) (A B)? C");
        runOnceOrTwiceABThenC(epService, soda, "(A B){1,2} C");
    }

    private void runAssertionsAtLeast(EPServiceProvider epService, boolean soda) {
        runAtLeast2AThenB(epService, soda, "A A A* B");
        runAtLeast2AThenB(epService, soda, "A{2,} B");
        runAtLeast2AThenB(epService, soda, "A{2,4} B");
    }

    private void runAssertionsUpTo(EPServiceProvider epService, boolean soda) {
        runUpTo2AThenB(epService, soda, "A? A? B");
        runUpTo2AThenB(epService, soda, "A{,2} B");
    }

    private void runAssertionsRepeatRange(EPServiceProvider epService, boolean soda) {
        run2To3AThenB(epService, soda, "A A A? B");
        run2To3AThenB(epService, soda, "A{2,3} B");
    }

    private void runAssertionsNestedUpTo(EPServiceProvider epService, boolean soda) {
        runUpTo2ABThenC(epService, soda, "(A B)? (A B)? C");
        runUpTo2ABThenC(epService, soda, "(A B){,2} C");
    }

    private void runAssertionsNestedAtLeast(EPServiceProvider epService, boolean soda) {
        runAtLeast2ABThenC(epService, soda, "(A B) (A B) (A B)* C");
        runAtLeast2ABThenC(epService, soda, "(A B){2,} C");
    }

    private void runAssertionRepeatSingleBound(EPServiceProvider epService, boolean soda) {
        runExactly2A(epService, soda, "A A");
        runExactly2A(epService, soda, "A{2}");
        runExactly2A(epService, soda, "(A{2})");

        // concatenation
        runAThen2BThenC(epService, soda, "A B B C");
        runAThen2BThenC(epService, soda, "A B{2} C");

        // nested
        runAThen2BThenC(epService, false, "A (B B) C");
        runAThen2BThenC(epService, false, "A (B{2}) C");

        // alteration
        runAThen2BOr2C(epService, soda, "A (B B|C C)");
        runAThen2BOr2C(epService, soda, "A (B{2}|C{2})");

        // multiple
        run2AThen2B(epService, soda, "A A B B");
        run2AThen2B(epService, soda, "A{2} B{2}");
    }

    private void runAtLeast2ABThenC(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "A1,B1,A2,B2,C1",
            "A1,B1,A2,B2,A3,B3,C1"
        }, new String[]{"A1,B1,C1", "A1,B1,A2,C1", "B1,A1,B2,C1"});
    }

    private void runOnceOrTwiceABThenC(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "A1,B1,C1",
            "A1,B1,A2,B2,C1"
        }, new String[]{"C1", "A1,A2,C2", "B1,A1,C1"});
    }

    private void runAtLeast2AThenB(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "A1,A2,B1",
            "A1,A2,A3,B1"
        }, new String[]{"A1,B1", "B1"});
    }

    private void runUpTo2AThenB(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "B1",
            "A1,B1",
            "A1,A2,B1"
        }, new String[]{"A1"});
    }

    private void run2AThen2B(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b", new boolean[]{true, true}, new String[]{
            "A1,A2,B1,B2",
        }, new String[]{"A1,A2,B1", "B1,B2,A1,A2", "A1,B1,A2,B2"});
    }

    private void runUpTo2ABThenC(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "C1",
            "A1,B1,C1",
            "A1,B1,A2,B2,C1",
        }, new String[]{"A1,B1,A2,B2", "A1,A2"});
    }

    private void run2To3AThenB(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "A1,A2,A3,B1",
            "A1,A2,B1",
        }, new String[]{"A1,B1", "A1,A2", "B1"});
    }

    private void runAThen2BOr2C(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{false, true, true}, new String[]{
            "A1,C1,C2",
            "A2,B1,B2",
        }, new String[]{"B1,B2", "C1,C2", "A1,B1,C1", "A1,C1,B1"});
    }

    private void runTwiceAB(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b", new boolean[]{true, true}, new String[]{
            "A1,B1,A2,B2",
        }, new String[]{"A1,A2,B1", "A1,A2,B1,B2", "A1,B1,B2,A2"});
    }

    private void runAThenTwiceBC(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{false, true, true}, new String[]{
            "A1,B1,C1,B2,C2",
        }, new String[]{"A1,B1,C1,B2", "A1,B1,C1,C2", "A1,B1,B2,C1,C2"});
    }

    private void runAThen2BThenC(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a,b,c", new boolean[]{false, true, false}, new String[]{
            "A1,B1,B2,C1",
        }, new String[]{"B1,B2,C1", "A1,B1,C1", "A1,B1,B2"});
    }

    private void runExactly2A(EPServiceProvider epService, boolean soda, String pattern) {
        runAssertion(epService, soda, pattern, "a", new boolean[]{true}, new String[]{
            "A1,A2",
            "A3,A4",
        }, new String[]{"A5"});
    }

    private void runAssertion(EPServiceProvider epService, boolean soda, String pattern, String propertyNames, boolean[] arrayProp,
                              String[] sequencesWithMatch,
                              String[] sequencesNoMatch) {
        String[] props = propertyNames.split(",");
        String measures = makeMeasures(props);
        String defines = makeDefines(props);

        String text = "select * from SupportBean " +
                "match_recognize (" +
                " partition by intPrimitive" +
                " measures " + measures +
                " pattern (" + pattern + ")" +
                " define " + defines +
                ")";
        SupportUpdateListener listener = new SupportUpdateListener();
        SupportModelHelper.createByCompileOrParse(epService, soda, text).addListener(listener);

        int sequenceNum = 0;
        for (String aSequencesWithMatch : sequencesWithMatch) {
            runAssertionSequence(epService, listener, true, props, arrayProp, sequenceNum, aSequencesWithMatch);
            sequenceNum++;
        }

        for (String aSequencesNoMatch : sequencesNoMatch) {
            runAssertionSequence(epService, listener, false, props, arrayProp, sequenceNum, aSequencesNoMatch);
            sequenceNum++;
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSequence(EPServiceProvider epService, SupportUpdateListener listener, boolean match, String[] propertyNames, boolean[] arrayProp, int sequenceNum, String sequence) {

        // send events
        String[] events = sequence.split(",");
        Map<String, List<SupportBean>> sent = new HashMap<>();
        for (String anEvent : events) {
            String type = new String(new char[]{anEvent.charAt(0)});
            SupportBean bean = sendEvent(anEvent, sequenceNum, epService);
            String propName = type.toLowerCase(Locale.ENGLISH);
            if (!sent.containsKey(propName)) {
                sent.put(propName, new ArrayList<>());
            }
            sent.get(propName).add(bean);
        }

        // prepare expected
        Object[] expected = new Object[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            List<SupportBean> sentForType = sent.get(propertyNames[i]);
            if (arrayProp[i]) {
                expected[i] = sentForType == null ? null : sentForType.toArray(new SupportBean[0]);
            } else {
                if (match) {
                    assertTrue(sentForType.size() == 1);
                    expected[i] = sentForType.get(0);
                }
            }
        }

        if (match) {
            EventBean event = listener.assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(event, propertyNames, expected);
        } else {
            assertFalse("Failed at " + sequence, listener.isInvoked());
        }
    }

    private String makeDefines(String[] props) {
        String delimiter = "";
        StringWriter buf = new StringWriter();
        for (String prop : props) {
            buf.append(delimiter);
            delimiter = ", ";
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(" as ");
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(".theString like \"");
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append("%\"");
        }
        return buf.toString();
    }

    private String makeMeasures(String[] props) {
        String delimiter = "";
        StringWriter buf = new StringWriter();
        for (String prop : props) {
            buf.append(delimiter);
            delimiter = ", ";
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(" as ");
            buf.append(prop);
        }
        return buf.toString();
    }

    private SupportBean sendEvent(String theString, int intPrimitive, EPServiceProvider epService) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(sb);
        return sb;
    }

    protected static void runEquivalent(EPServiceProvider epService, String before, String after) throws Exception {
        String epl = "select * from SupportBean#keepall " +
                "match_recognize (" +
                " measures A as a" +
                " pattern (" + before + ")" +
                " define" +
                " A as A.theString like \"A%\"" +
                ")";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        EPStatementSPI spi = (EPStatementSPI) epService.getEPAdministrator().create(model);
        StatementSpecCompiled spec = ((EPServiceProviderSPI) epService).getStatementLifecycleSvc().getStatementSpec(spi.getStatementId());
        RowRegexExprNode expanded = RegexPatternExpandUtil.expand(spec.getMatchRecognizeSpec().getPattern());
        StringWriter writer = new StringWriter();
        expanded.toEPL(writer, RowRegexExprNodePrecedenceEnum.MINIMUM);
        assertEquals(after, writer.toString());
        epService.getEPAdministrator().destroyAllStatements();
    }
}