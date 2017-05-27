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
package com.espertech.esper.regression.expr.filter;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanNumeric;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ExecFilterInAndBetween implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInDynamic(epService);
        runAssertionSimpleIntAndEnumWrite(epService);
        runAssertionInvalid(epService);
        runAssertionInExpr(epService);
        runAssertionNotInExpr(epService);
        runAssertionReuse(epService);
        runAssertionReuseNot(epService);
    }

    private void runAssertionInDynamic(EPServiceProvider epService) {
        String expr = "select * from pattern [a=" + SupportBeanNumeric.class.getName() + " -> every b=" + SupportBean.class.getName()
                + "(intPrimitive in (a.intOne, a.intTwo))]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanNumeric(epService, 10, 20);
        sendBeanInt(epService, 10);
        assertTrue(listener.getAndClearIsInvoked());
        sendBeanInt(epService, 11);
        assertFalse(listener.getAndClearIsInvoked());
        sendBeanInt(epService, 20);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.stop();

        expr = "select * from pattern [a=" + SupportBean_S0.class.getName() + " -> every b=" + SupportBean.class.getName()
                + "(theString in (a.p00, a.p01, a.p02))]";
        stmt = epService.getEPAdministrator().createEPL(expr);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "b", "c", "d"));
        sendBeanString(epService, "a");
        assertTrue(listener.getAndClearIsInvoked());
        sendBeanString(epService, "x");
        assertFalse(listener.getAndClearIsInvoked());
        sendBeanString(epService, "b");
        assertTrue(listener.getAndClearIsInvoked());
        sendBeanString(epService, "c");
        assertTrue(listener.getAndClearIsInvoked());
        sendBeanString(epService, "d");
        assertFalse(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void runAssertionSimpleIntAndEnumWrite(EPServiceProvider epService) {
        String expr = "select * from " + SupportBean.class.getName() + "(intPrimitive in (1, 10))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanInt(epService, 10);
        assertTrue(listener.getAndClearIsInvoked());
        sendBeanInt(epService, 11);
        assertFalse(listener.getAndClearIsInvoked());
        sendBeanInt(epService, 1);
        assertTrue(listener.getAndClearIsInvoked());
        stmt.destroy();

        // try enum - ESPER-459
        Set<SupportEnum> types = new HashSet<SupportEnum>();
        types.add(SupportEnum.ENUM_VALUE_2);
        EPPreparedStatement inPstmt = epService.getEPAdministrator().prepareEPL("select * from " + SupportBean.class.getName() + " ev " + "where ev.enumValue in (?)");
        inPstmt.setObject(1, types);

        EPStatement inStmt = epService.getEPAdministrator().create(inPstmt);
        inStmt.addListener(listener);

        SupportBean theEvent = new SupportBean();
        theEvent.setEnumValue(SupportEnum.ENUM_VALUE_2);
        epService.getEPRuntime().sendEvent(theEvent);

        assertTrue(listener.isInvoked());

        inStmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        // we do not coerce
        tryInvalid(epService, "select * from " + SupportBean.class.getName() + "(intPrimitive in (1L, 10L))");
        tryInvalid(epService, "select * from " + SupportBean.class.getName() + "(intPrimitive in (1, 10L))");
        tryInvalid(epService, "select * from " + SupportBean.class.getName() + "(intPrimitive in (1, 'x'))");

        String expr = "select * from pattern [a=" + SupportBean.class.getName() + " -> b=" + SupportBean.class.getName()
                + "(intPrimitive in (a.longPrimitive, a.longBoxed))]";
        tryInvalid(epService, expr);
    }

    private void runAssertionInExpr(EPServiceProvider epService) {
        tryExpr(epService, "(theString > 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{false, false, true, true});
        tryExpr(epService, "(theString < 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{true, false, false, false});
        tryExpr(epService, "(theString >= 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{false, true, true, true});
        tryExpr(epService, "(theString <= 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{true, true, false, false});
        tryExpr(epService, "(theString in ['b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, true, true, true, false});
        tryExpr(epService, "(theString in ('b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, false, true, true, false});
        tryExpr(epService, "(theString in ['b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, true, true, false, false});
        tryExpr(epService, "(theString in ('b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, false, true, false, false});
        tryExpr(epService, "(boolPrimitive in (false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, true});
        tryExpr(epService, "(boolPrimitive in (false, false, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, true});
        tryExpr(epService, "(boolPrimitive in (false, true, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, true});
        tryExpr(epService, "(intBoxed in (4, 6, 1))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, true, false, false, true, false, true});
        tryExpr(epService, "(intBoxed in (3))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, false, false, false});
        tryExpr(epService, "(longBoxed in (3))", "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}, new boolean[]{false, false, false, true, false, false, false});
        tryExpr(epService, "(intBoxed between 4 and 6)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, false, true, true, true});
        tryExpr(epService, "(intBoxed between 2 and 1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, true, true, false, false, false, false});
        tryExpr(epService, "(intBoxed between 4 and -1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, true, true, false, false});
        tryExpr(epService, "(intBoxed in [2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, true, true, true, false, false});
        tryExpr(epService, "(intBoxed in (2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, true, false, false});
        tryExpr(epService, "(intBoxed in [2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, true, true, false, false, false});
        tryExpr(epService, "(intBoxed in (2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, false, false, false});
    }

    private void runAssertionNotInExpr(EPServiceProvider epService) {
        tryExpr(epService, "(intBoxed not between 4 and 6)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, true, false, false, false});
        tryExpr(epService, "(intBoxed not between 2 and 1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, false, false, true, true, true, true});
        tryExpr(epService, "(intBoxed not between 4 and -1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, false, false, true, true});
        tryExpr(epService, "(intBoxed not in [2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, false, false, false, true, true});
        tryExpr(epService, "(intBoxed not in (2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, false, true, true});
        tryExpr(epService, "(intBoxed not in [2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, false, false, true, true, true});
        tryExpr(epService, "(intBoxed not in (2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, true, true, true});
        tryExpr(epService, "(theString not in ['b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, false, false, false, true});
        tryExpr(epService, "(theString not in ('b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, true, false, false, true});
        tryExpr(epService, "(theString not in ['b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, false, false, true, true});
        tryExpr(epService, "(theString not in ('b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, true, false, true, true});
        tryExpr(epService, "(theString not in ('a', 'b'))", "theString", new String[]{"a", "x", "b", "y"}, new boolean[]{false, true, false, true});
        tryExpr(epService, "(boolPrimitive not in (false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, false});
        tryExpr(epService, "(boolPrimitive not in (false, false, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, false});
        tryExpr(epService, "(boolPrimitive not in (false, true, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, false});
        tryExpr(epService, "(intBoxed not in (4, 6, 1))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, false, true, true, false, true, false});
        tryExpr(epService, "(intBoxed not in (3))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, true, true, true});
        tryExpr(epService, "(longBoxed not in (3))", "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}, new boolean[]{true, true, true, false, true, true, true});
    }

    private void runAssertionReuse(EPServiceProvider epService) {
        String expr = "select * from " + SupportBean.class.getName() + "(intBoxed in [2:4])";
        tryReuse(epService, new String[]{expr, expr});

        expr = "select * from " + SupportBean.class.getName() + "(intBoxed in (1, 2, 3))";
        tryReuse(epService, new String[]{expr, expr});

        String exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (2:3])";
        String exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed in (1:3])";
        tryReuse(epService, new String[]{exprOne, exprTwo});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (2, 3, 4))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed in (1, 3))";
        tryReuse(epService, new String[]{exprOne, exprTwo});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (2, 3, 4))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed in (1, 3))";
        String exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed in (8, 3))";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (3, 1, 3))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed in (3, 3))";
        exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed in (1, 3))";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});

        exprOne = "select * from " + SupportBean.class.getName() + "(boolPrimitive=false, intBoxed in (1, 2, 3))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(boolPrimitive=false, intBoxed in (3, 4))";
        exprThree = "select * from " + SupportBean.class.getName() + "(boolPrimitive=false, intBoxed in (3))";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (1, 2, 3), longPrimitive >= 0)";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed in (3, 4), intPrimitive >= 0)";
        exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed in (3), bytePrimitive < 1)";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});
    }

    private void runAssertionReuseNot(EPServiceProvider epService) {
        String expr = "select * from " + SupportBean.class.getName() + "(intBoxed not in [1:2])";
        tryReuse(epService, new String[]{expr, expr});

        String exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed in (3, 1, 3))";
        String exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed not in (2, 1))";
        String exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed not between 0 and -3)";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed not in (1, 4, 5))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed not in (1, 4, 5))";
        exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed not in (4, 5, 1))";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});

        exprOne = "select * from " + SupportBean.class.getName() + "(intBoxed not in (3:4))";
        exprTwo = "select * from " + SupportBean.class.getName() + "(intBoxed not in [1:3))";
        exprThree = "select * from " + SupportBean.class.getName() + "(intBoxed not in (1,1,1,33))";
        tryReuse(epService, new String[]{exprOne, exprTwo, exprThree});
    }

    private void tryReuse(EPServiceProvider epService, String[] statements) {
        SupportUpdateListener[] testListener = new SupportUpdateListener[statements.length];
        EPStatement[] stmt = new EPStatement[statements.length];

        // create all statements
        for (int i = 0; i < statements.length; i++) {
            testListener[i] = new SupportUpdateListener();
            stmt[i] = epService.getEPAdministrator().createEPL(statements[i]);
            stmt[i].addListener(testListener[i]);
        }

        // send event, all should receive the event
        sendBean(epService, "intBoxed", 3);
        for (int i = 0; i < testListener.length; i++) {
            assertTrue(testListener[i].isInvoked());
            testListener[i].reset();
        }

        // stop first, then second, then third etc statement
        for (int toStop = 0; toStop < statements.length; toStop++) {
            stmt[toStop].stop();

            // send event, all remaining statement received it
            sendBean(epService, "intBoxed", 3);
            for (int i = 0; i <= toStop; i++) {
                assertFalse(testListener[i].isInvoked());
                testListener[i].reset();
            }
            for (int i = toStop + 1; i < testListener.length; i++) {
                assertTrue(testListener[i].isInvoked());
                testListener[i].reset();
            }
        }

        // now all statements are stopped, send event and verify no listener received
        sendBean(epService, "intBoxed", 3);
        for (int i = 0; i < testListener.length; i++) {
            assertFalse(testListener[i].isInvoked());
        }
    }

    private void tryExpr(EPServiceProvider epService, String filterExpr, String fieldName, Object[] values, boolean[] isInvoked) {
        String expr = "select * from " + SupportBean.class.getName() + filterExpr;
        EPStatement stmt = epService.getEPAdministrator().createEPL(expr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < values.length; i++) {
            sendBean(epService, fieldName, values[i]);
            assertEquals("Listener invocation unexpected for " + filterExpr + " field " + fieldName + "=" + values[i], isInvoked[i], listener.isInvoked());
            listener.reset();
        }

        stmt.stop();
    }

    private void sendBeanInt(EPServiceProvider epService, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendBeanString(EPServiceProvider epService, String value) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(value);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendBeanNumeric(EPServiceProvider epService, int intOne, int intTwo) {
        SupportBeanNumeric num = new SupportBeanNumeric(intOne, intTwo);
        epService.getEPRuntime().sendEvent(num);
    }

    private void sendBean(EPServiceProvider epService, String fieldName, Object value) {
        SupportBean theEvent = new SupportBean();
        if (fieldName.equals("theString")) {
            theEvent.setTheString((String) value);
        }
        if (fieldName.equals("boolPrimitive")) {
            theEvent.setBoolPrimitive((Boolean) value);
        }
        if (fieldName.equals("intBoxed")) {
            theEvent.setIntBoxed((Integer) value);
        }
        if (fieldName.equals("longBoxed")) {
            theEvent.setLongBoxed((Long) value);
        }
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void tryInvalid(EPServiceProvider epService, String expr) {
        try {
            epService.getEPAdministrator().createEPL(expr);
            fail();
        } catch (EPException ex) {
            // expected
        }
    }
}
