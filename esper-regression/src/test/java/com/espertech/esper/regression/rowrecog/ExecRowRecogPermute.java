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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.PermutationEnumeration;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static org.junit.Assert.assertFalse;

public class ExecRowRecogPermute implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runPermute(epService, false);
        runPermute(epService, true);

        runDocSamples(epService);

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

    private void runDocSamples(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema TemperatureSensorEvent(id string, device string, temp int)");

        runDocSampleUpToN(epService);
    }

    private void runDocSampleUpToN(EPServiceProvider epService) {
        String[] fields = "a_id,b_id".split(",");
        String epl = "select * from TemperatureSensorEvent\n" +
                "match_recognize (\n" +
                "  partition by device\n" +
                "  measures A.id as a_id, B.id as b_id\n" +
                "  pattern (match_recognize_permute(A, B))\n" +
                "  define \n" +
                "\tA as A.temp < 100, \n" +
                "\tB as B.temp >= 100)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"E1", "1", 99}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E2", "1", 100}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        epService.getEPRuntime().sendEvent(new Object[]{"E3", "1", 100}, "TemperatureSensorEvent");
        epService.getEPRuntime().sendEvent(new Object[]{"E4", "1", 99}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "E3"});

        epService.getEPRuntime().sendEvent(new Object[]{"E5", "1", 98}, "TemperatureSensorEvent");
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runPermute(EPServiceProvider epService, boolean soda) throws Exception {
        tryPermute(epService, soda, "(A B C)|(A C B)|(B A C)|(B C A)|(C A B)|(C B A)");
        tryPermute(epService, soda, "(match_recognize_permute(A,B,C))");
    }

    public void tryPermute(EPServiceProvider epService, boolean soda, String pattern) {
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
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] prefixes = "A,B,C".split(",");
        String[] fields = "a,b,c".split(",");
        PermutationEnumeration e = new PermutationEnumeration(3);
        int count = 0;

        while (e.hasMoreElements()) {
            int[] indexes = e.nextElement();
            Object[] expected = new Object[3];
            for (int i = 0; i < 3; i++) {
                expected[indexes[i]] = sendEvent(epService, prefixes[indexes[i]] + Integer.toString(count), count);
            }
            count++;

            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
        }

        stmt.destroy();
    }

    private static void runEquivalent(EPServiceProvider epService, String before, String after) throws Exception {
        ExecRowRecogRepetition.runEquivalent(epService, before, after);
    }

    private SupportBean sendEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(sb);
        return sb;
    }
}