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

package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.PermutationEnumeration;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestRowPatternRecognitionPermute extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testPermute() throws Exception {
        runPermute(false);
        runPermute(true);

        runDocSamples();

        runEquivalent(epService, "mAtCh_Recognize_Permute(A)",
                "(A)");
        runEquivalent(epService, "match_recognize_permute(A,B)",
                "(A B|B A)");
        runEquivalent(epService, "match_recognize_permute(A,B,C)",
                "(A B C|A C B|B A C|B C A|C A B|C B A)");
        runEquivalent(epService, "match_recognize_permute(A,B,C,D)",
                "(A B C D|A B D C|A C B D|A C D B|A D B C|A D C B|B A C D|B A D C|B C A D|B C D A|B D A C|B D C A|C A B D|C A D B|C B A D|C B D A|C D A B|C D B A|D A B C|D A C B|D B A C|D B C A|D C A B|D C B A)");

        runEquivalent(epService, "match_recognize_permute((A B), C)",
                "((A B) C|C (A B))");
        runEquivalent(epService, "match_recognize_permute((A|B), (C D), E)",
                "((A|B) (C D) E|(A|B) E (C D)|(C D) (A|B) E|(C D) E (A|B)|E (A|B) (C D)|E (C D) (A|B))");

        runEquivalent(epService, "A match_recognize_permute(B,C) D",
                "A (B C|C B) D");

        runEquivalent(epService, "match_recognize_permute(A, match_recognize_permute(B, C))",
                "(A (B C|C B)|(B C|C B) A)");
    }

    private void runDocSamples() {
        epService.getEPAdministrator().createEPL("create objectarray schema TemperatureSensorEvent(id string, device string, temp int)");

        runDocSampleUpToN();
    }

    private void runDocSampleUpToN() {
        String[] fields = "a_id,b_id".split(",");
        String epl = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A.id as a_id, B.id as b_id\n" +
                "  pattern (match_recognize_permute(A, B))\n" +
                "  define \n" +
                "\tA as A.temp < 100, \n" +
                "\tB as B.temp >= 100)";
        System.out.println(epl);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {"E1", "1", 99}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[] {"E2", "1", 100}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        epService.getEPRuntime().sendEvent(new Object[] {"E3", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[] {"E4", "1", 99}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "E3"});

        epService.getEPRuntime().sendEvent(new Object[] {"E5", "1", 98}, "TemperatureSensorEvent");
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runPermute(boolean soda) throws Exception {
        tryPermute(soda, "(A B C)|(A C B)|(B A C)|(B C A)|(C A B)|(C B A)");
        tryPermute(soda, "(match_recognize_permute(A,B,C))");
    }

    public void tryPermute(boolean soda, String pattern)
    {
        String epl = "select * from SupportBean " +
                "match_recognize (" +
                " partition by intPrimitive" +
                " measures A as a, B as b, C as c" +
                " pattern (" + pattern + ")" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"," +
                " C as C.theString like \"C%\"" +
                ")";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        String[] prefixes = "A,B,C".split(",");
        String[] fields = "a,b,c".split(",");
        PermutationEnumeration e = new PermutationEnumeration(3);
        int count = 0;

        while(e.hasMoreElements()) {
            int[] indexes = e.nextElement();
            Object[] expected = new Object[3];
            for (int i = 0; i < 3; i++) {
                expected[indexes[i]] = sendEvent(prefixes[indexes[i]] + Integer.toString(count), count);
            }
            count++;

            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
        }

        stmt.destroy();
    }

    private static void runEquivalent(EPServiceProvider epService, String before, String after) throws Exception {
        TestRowPatternRecognitionRepetition.runEquivalent(epService, before, after);
    }

    private SupportBean sendEvent(String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(sb);
        return sb;
    }
}