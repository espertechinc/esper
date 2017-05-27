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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecRowRecogDataSet implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecRowRecogDataSet.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionExampleFinancialWPattern(epService);
        runAssertionExampleWithPREV(epService);
    }

    private void runAssertionExampleFinancialWPattern(EPServiceProvider epService) {
        String text = "select * " +
                "from SupportBean " +
                "match_recognize (" +
                " measures A.theString as beginA, last(Z.theString) as lastZ" +
                " all matches" +
                " after match skip to current row" +
                " pattern (A W+ X+ Y+ Z+)" +
                " define" +
                " W as W.intPrimitive<prev(W.intPrimitive)," +
                " X as X.intPrimitive>prev(X.intPrimitive)," +
                " Y as Y.intPrimitive<prev(Y.intPrimitive)," +
                " Z as Z.intPrimitive>prev(Z.intPrimitive)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] data = new Object[][]{
                {"E1", 8},   // 0
                {"E2", 8},
                {"E3", 8},       // A
                {"E4", 6},       // W
                {"E5", 3},       // W
                {"E6", 7},
                {"E7", 6},
                {"E8", 2},
                {"E9", 6,        // Z
                    new String[]{"beginA=E3,lastZ=E9", "beginA=E4,lastZ=E9"}},
                {"E10", 2},
                {"E11", 9,  // 10
                    new String[]{"beginA=E6,lastZ=E11", "beginA=E7,lastZ=E11"}},
                {"E12", 9},
                {"E13", 8},
                {"E14", 5},
                {"E15", 0},
                {"E16", 9},
                {"E17", 2},
                {"E18", 0},
                {"E19", 2,
                    new String[]{"beginA=E12,lastZ=E19", "beginA=E13,lastZ=E19", "beginA=E14,lastZ=E19"}},
                {"E20", 3,
                    new String[]{"beginA=E12,lastZ=E20", "beginA=E13,lastZ=E20", "beginA=E14,lastZ=E20"}},
                {"E21", 8,
                    new String[]{"beginA=E12,lastZ=E21", "beginA=E13,lastZ=E21", "beginA=E14,lastZ=E21"}},
                {"E22", 5},
                {"E23", 9,
                    new String[]{"beginA=E16,lastZ=E23", "beginA=E17,lastZ=E23"}},
                {"E24", 9},
                {"E25", 4},
                {"E26", 7},
                {"E27", 2},
                {"E28", 8,
                    new String[]{"beginA=E24,lastZ=E28"}},
                {"E29", 0},
                {"E30", 4,
                    new String[]{"beginA=E26,lastZ=E30"}},
                {"E31", 4},
                {"E32", 7},
                {"E33", 8},
                {"E34", 6},
                {"E35", 4},
                {"E36", 5},
                {"E37", 1},
                {"E38", 7,
                    new String[]{"beginA=E33,lastZ=E38", "beginA=E34,lastZ=E38"}},
                {"E39", 5},
                {"E40", 8,
                    new String[]{"beginA=E36,lastZ=E40"}},
                {"E41", 6},
                {"E42", 6},
                {"E43", 0},
                {"E44", 6},
                {"E45", 8},
                {"E46", 4},
                {"E47", 3},
                {"E48", 8,
                    new String[]{"beginA=E42,lastZ=E48"}},
                {"E49", 2},
                {"E50", 5,
                    new String[]{"beginA=E45,lastZ=E50", "beginA=E46,lastZ=E50"}},
                {"E51", 3},
                {"E52", 3},
                {"E53", 9},
                {"E54", 8},
                {"E55", 5},
                {"E56", 5},
                {"E57", 9},
                {"E58", 7},
                {"E59", 3},
                {"E60", 3}
        };

        int rowCount = 0;
        for (Object[] row : data) {
            SupportBean theEvent = new SupportBean((String) row[0], (Integer) row[1]);
            epService.getEPRuntime().sendEvent(theEvent);

            compare(row, rowCount, theEvent, listener);
            rowCount++;
        }

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(text, stmt.getText());

        for (Object[] row : data) {
            SupportBean theEvent = new SupportBean((String) row[0], (Integer) row[1]);
            epService.getEPRuntime().sendEvent(theEvent);

            compare(row, rowCount, theEvent, listener);
            rowCount++;
        }
    }

    private void runAssertionExampleWithPREV(EPServiceProvider epService) {
        String query = "SELECT * " +
                "FROM MyEvent#keepall" +
                "   MATCH_RECOGNIZE (" +
                "       MEASURES A.theString AS a_string," +
                "         A.value AS a_value," +
                "         B.theString AS b_string," +
                "         B.value AS b_value," +
                "         C[0].theString AS c0_string," +
                "         C[0].value AS c0_value," +
                "         C[1].theString AS c1_string," +
                "         C[1].value AS c1_value," +
                "         C[2].theString AS c2_string," +
                "         C[2].value AS c2_value," +
                "         D.theString AS d_string," +
                "         D.value AS d_value," +
                "         E[0].theString AS e0_string," +
                "         E[0].value AS e0_value," +
                "         E[1].theString AS e1_string," +
                "         E[1].value AS e1_value," +
                "         F[0].theString AS f0_string," +
                "         F[0].value AS f0_value," +
                "         F[1].theString AS f1_string," +
                "         F[1].value AS f1_value" +
                "       ALL MATCHES" +
                "       after match skip to current row" +
                "       PATTERN ( A B C* D E* F+ )" +
                "       DEFINE /* A is unspecified, defaults to TRUE, matches any row */" +
                "            B AS (B.value < PREV (B.value))," +
                "            C AS (C.value <= PREV (C.value))," +
                "            D AS (D.value < PREV (D.value))," +
                "            E AS (E.value >= PREV (E.value))," +
                "            F AS (F.value >= PREV (F.value) and F.value > A.value)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(query);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] data = new Object[][]{
                {"E1", 100, null},
                {"E2", 98, null},
                {"E3", 75, null},
                {"E4", 61, null},
                {"E5", 50, null},
                {"E6", 49, null},
                {"E7", 64,
                    new String[]{"a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=null,e0_value=null,e1_string=null,e1_value=null,f0_string=E7,f0_value=64,f1_string=null,f1_value=null"}},
                {"E8", 78,
                    new String[]{"a_string=E3,a_value=75,b_string=E4,b_value=61,c0_string=E5,c0_value=50,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=null,e1_value=null,f0_string=E8,f0_value=78,f1_string=null,f1_value=null",
                            "a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=null,e1_value=null,f0_string=E8,f0_value=78,f1_string=null,f1_value=null",
                            "a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=null,e0_value=null,e1_string=null,e1_value=null,f0_string=E7,f0_value=64,f1_string=E8,f1_value=78"}},
                {"E9", 84,
                    new String[]{"a_string=E3,a_value=75,b_string=E4,b_value=61,c0_string=E5,c0_value=50,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=null,e1_value=null,f0_string=E8,f0_value=78,f1_string=E9,f1_value=84",
                            "a_string=E3,a_value=75,b_string=E4,b_value=61,c0_string=E5,c0_value=50,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=E8,e1_value=78,f0_string=E9,f0_value=84,f1_string=null,f1_value=null",
                            "a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=E8,e1_value=78,f0_string=E9,f0_value=84,f1_string=null,f1_value=null",
                            "a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=E7,e0_value=64,e1_string=null,e1_value=null,f0_string=E8,f0_value=78,f1_string=E9,f1_value=84",
                            "a_string=E4,a_value=61,b_string=E5,b_value=50,c0_string=null,c0_value=null,c1_string=null,c1_value=null,c2_string=null,c2_value=null,d_string=E6,d_value=49,e0_string=null,e0_value=null,e1_string=null,e1_value=null,f0_string=E7,f0_value=64,f1_string=E8,f1_value=78"
                    }},
        };

        int rowCount = 0;
        for (Object[] row : data) {
            rowCount++;
            SupportRecogBean theEvent = new SupportRecogBean((String) row[0], (Integer) row[1]);
            epService.getEPRuntime().sendEvent(theEvent);

            compare(row, rowCount, theEvent, listener);
            rowCount++;
        }

        stmt.destroy();
    }

    private static void compare(Object[] row, int rowCount, Object theEvent, SupportUpdateListener listener) {
        if (row.length < 3 || row[2] == null) {
            if (listener.isInvoked()) {
                EventBean[] matches = listener.getLastNewData();
                if (matches != null) {
                    for (int i = 0; i < matches.length; i++) {
                        log.info("Received matches: " + getProps(matches[i]));
                    }
                }
            }
            assertFalse("For event " + theEvent + " row " + rowCount, listener.isInvoked());
            return;
        }

        String[] expected = (String[]) row[2];

        EventBean[] matches = listener.getLastNewData();
        String[] matchesText = null;
        if (matches != null) {
            matchesText = new String[matches.length];
            for (int i = 0; i < matches.length; i++) {
                matchesText[i] = getProps(matches[i]);
                log.debug(getProps(matches[i]));
            }
        } else {
            if (expected != null) {
                log.info("Received no matches but expected: ");
                for (int i = 0; i < expected.length; i++) {
                    log.info(expected[i]);
                }
                Assert.fail();
            }
        }

        Arrays.sort(expected);
        Arrays.sort(matchesText);

        assertEquals("For event " + theEvent, matches.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(matchesText[i])) {
                log.info("expected:" + expected[i]);
                log.info("  actual:" + expected[i]);
                assertEquals("Sending event " + theEvent + " row " + rowCount, expected[i], matchesText[i]);
            }
        }

        listener.reset();
    }

    private static String getProps(EventBean theEvent) {
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (EventPropertyDescriptor prop : theEvent.getEventType().getPropertyDescriptors()) {
            buf.append(delimiter);
            buf.append(prop.getPropertyName());
            buf.append("=");
            buf.append(theEvent.get(prop.getPropertyName()));
            delimiter = ",";
        }
        return buf.toString();
    }
}
