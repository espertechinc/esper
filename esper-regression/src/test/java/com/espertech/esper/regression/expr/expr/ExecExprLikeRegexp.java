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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecExprLikeRegexp implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionLikeWConstants(epService);
        runAssertionLikeWExprs(epService);
        runAssertionRegexpWConstants(epService);
        runAssertionRegexpWExprs(epService);
        runAssertionLikeRegexStringAndNull(epService);
        runAssertionInvalidLikeRegEx(epService);
        runAssertionLikeRegexStringAndNull(epService);
        runAssertionLikeRegexEscapedChar(epService);
        runAssertionLikeRegexStringAndNull_OM(epService);
        runAssertionLikeRegexStringAndNull_Compile(epService);
        runAssertionLikeRegexNumericAndNull(epService);
    }

    private void runAssertionRegexpWExprs(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p00 regexp p01 as c0, id regexp p02 as c1 from SupportBean_S0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendS0Event(epService, 413, "XXAXX", ".*A.*", ".*1.*", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        sendS0Event(epService, 413, "XXaXX", ".*B.*", ".*2.*", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {false, false});

        sendS0Event(epService, 413, "XXCXX", ".*C.*", ".*3.*", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        stmt.destroy();
    }

    private void runAssertionRegexpWConstants(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString regexp '.*Jack.*' as c0, intPrimitive regexp '.*1.*' as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("Joe", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("TheJackWhite", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        stmt.destroy();
    }

    private void runAssertionLikeWConstants(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString like 'A%' as c0, intPrimitive like '1%' as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("Bxx", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("Ayyy", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        stmt.destroy();
    }

    private void runAssertionLikeWExprs(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p00 like p01 as c0, id like p02 as c1 from SupportBean_S0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendS0Event(epService, 413, "%XXaXX", "%a%", "%1%", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        sendS0Event(epService, 413, "%XXcXX", "%b%", "%2%", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {false, false});

        sendS0Event(epService, 413, "%XXcXX", "%c%", "%3%", null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {true, true});

        stmt.destroy();
    }

    private void runAssertionLikeRegexStringAndNull(EPServiceProvider epService) {
        String caseExpr = "select p00 like p01 as r1, " +
                " p00 like p01 escape \"!\" as r2," +
                " p02 regexp p03 as r3 " +
                " from SupportBean_S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runLikeRegexStringAndNull(epService, listener);

        stmt.destroy();
    }

    private void runAssertionLikeRegexEscapedChar(EPServiceProvider epService) {
        String caseExpr = "select p00 regexp '\\\\w*-ABC' as result from SupportBean_S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "TBT-ABC"));
        assertTrue((Boolean) listener.assertOneGetNewAndReset().get("result"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "TBT-BC"));
        assertFalse((Boolean) listener.assertOneGetNewAndReset().get("result"));

        stmt.destroy();
    }

    private void runAssertionLikeRegexStringAndNull_OM(EPServiceProvider epService) throws Exception {
        String stmtText = "select p00 like p01 as r1, " +
                "p00 like p01 escape \"!\" as r2, " +
                "p02 regexp p03 as r3 " +
                "from SupportBean_S0";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01")), "r1")
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01"), Expressions.constant("!")), "r2")
                .add(Expressions.regexp(Expressions.property("p02"), Expressions.property("p03")), "r3")
        );
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        runLikeRegexStringAndNull(epService, testListener);

        stmt.destroy();

        String epl = "select * from " + SupportBean.class.getName() + "(theString not like \"foo%\")";
        EPPreparedStatement eps = epService.getEPAdministrator().prepareEPL(epl);
        EPStatement statement = epService.getEPAdministrator().create(eps);
        assertEquals(epl, statement.getText());
        statement.destroy();

        epl = "select * from " + SupportBean.class.getName() + "(theString not regexp \"foo\")";
        eps = epService.getEPAdministrator().prepareEPL(epl);
        statement = epService.getEPAdministrator().create(eps);
        assertEquals(epl, statement.getText());
        statement.destroy();
    }

    private void runAssertionLikeRegexStringAndNull_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select p00 like p01 as r1, " +
                "p00 like p01 escape \"!\" as r2, " +
                "p02 regexp p03 as r3 " +
                "from SupportBean_S0";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        runLikeRegexStringAndNull(epService, testListener);

        stmt.destroy();
    }

    private void runLikeRegexStringAndNull(EPServiceProvider epService, SupportUpdateListener listener) {
        sendS0Event(epService, -1, "a", "b", "c", "d");
        assertReceived(listener, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, -1, null, "b", null, "d");
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, -1, "a", null, "c", null);
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, -1, null, null, null, null);
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, -1, "abcdef", "%de_", "a", "[a-c]");
        assertReceived(listener, new Object[][]{{"r1", true}, {"r2", true}, {"r3", true}});

        sendS0Event(epService, -1, "abcdef", "b%de_", "d", "[a-c]");
        assertReceived(listener, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, -1, "!adex", "!%de_", "", ".");
        assertReceived(listener, new Object[][]{{"r1", true}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, -1, "%dex", "!%de_", "a", ".");
        assertReceived(listener, new Object[][]{{"r1", false}, {"r2", true}, {"r3", true}});
    }

    private void runAssertionInvalidLikeRegEx(EPServiceProvider epService) {
        tryInvalid(epService, "intPrimitive like 'a' escape null");
        tryInvalid(epService, "intPrimitive like boolPrimitive");
        tryInvalid(epService, "boolPrimitive like string");
        tryInvalid(epService, "string like string escape intPrimitive");

        tryInvalid(epService, "intPrimitive regexp doublePrimitve");
        tryInvalid(epService, "intPrimitive regexp boolPrimitive");
        tryInvalid(epService, "boolPrimitive regexp string");
        tryInvalid(epService, "string regexp intPrimitive");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportBean where theString regexp \"*any*\"",
                "Failed to validate filter expression 'theString regexp \"*any*\"': Error compiling regex pattern '*any*': Dangling meta character '*' near index 0");
    }

    private void runAssertionLikeRegexNumericAndNull(EPServiceProvider epService) {
        String caseExpr = "select intBoxed like '%01%' as r1, " +
                " doubleBoxed regexp '[0-9][0-9].[0-9]' as r2 " +
                " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener testListener = new SupportUpdateListener();
        selectTestCase.addListener(testListener);

        sendSupportBeanEvent(epService, 101, 1.1);
        assertReceived(testListener, new Object[][]{{"r1", true}, {"r2", false}});

        sendSupportBeanEvent(epService, 102, 11d);
        assertReceived(testListener, new Object[][]{{"r1", false}, {"r2", true}});

        sendSupportBeanEvent(epService, null, null);
        assertReceived(testListener, new Object[][]{{"r1", null}, {"r2", null}});
    }

    private void tryInvalid(EPServiceProvider epService, String expr) {
        try {
            String statement = "select " + expr + " from " + SupportBean.class.getName();
            epService.getEPAdministrator().createEPL(statement);
            fail();
        } catch (EPException ex) {
            // expected
        }
    }

    private void assertReceived(SupportUpdateListener testListener, Object[][] objects) {
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        for (Object[] object : objects) {
            String key = (String) object[0];
            Object result = object[1];
            assertEquals("key=" + key + " result=" + result, result, theEvent.get(key));
        }
    }

    private void sendS0Event(EPServiceProvider epService, int id, String p00, String p01, String p02, String p03) {
        SupportBean_S0 bean = new SupportBean_S0(id, p00, p01, p02, p03);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, Integer intBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
