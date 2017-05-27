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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecExprLikeRegexp implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionRegexpFilterWithDanglingMetaCharacter(epService);
        runAssertionLikeRegexStringAndNull(epService);
        runAssertionLikeRegexEscapedChar(epService);
        runAssertionLikeRegexStringAndNull_OM(epService);
        runAssertionLikeRegexStringAndNull_Compile(epService);
        runAssertionInvalidLikeRegEx(epService);
        runAssertionLikeRegexNumericAndNull(epService);
    }

    private void runAssertionRegexpFilterWithDanglingMetaCharacter(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean where theString regexp \"*any*\"");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionLikeRegexStringAndNull(EPServiceProvider epService) {
        String caseExpr = "select p00 like p01 as r1, " +
                " p00 like p01 escape \"!\" as r2," +
                " p02 regexp p03 as r3 " +
                " from " + SupportBean_S0.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runLikeRegexStringAndNull(epService, listener);

        stmt.destroy();
    }

    private void runAssertionLikeRegexEscapedChar(EPServiceProvider epService) {
        String caseExpr = "select p00 regexp '\\\\w*-ABC' as result from " + SupportBean_S0.class.getName();

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
                "from " + SupportBean_S0.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01")), "r1")
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01"), Expressions.constant("!")), "r2")
                .add(Expressions.regexp(Expressions.property("p02"), Expressions.property("p03")), "r3")
        );
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean_S0.class.getName())));
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
                "from " + SupportBean_S0.class.getName();

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
        sendS0Event(epService, "a", "b", "c", "d");
        assertReceived(listener, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, null, "b", null, "d");
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, "a", null, "c", null);
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, null, null, null, null);
        assertReceived(listener, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(epService, "abcdef", "%de_", "a", "[a-c]");
        assertReceived(listener, new Object[][]{{"r1", true}, {"r2", true}, {"r3", true}});

        sendS0Event(epService, "abcdef", "b%de_", "d", "[a-c]");
        assertReceived(listener, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, "!adex", "!%de_", "", ".");
        assertReceived(listener, new Object[][]{{"r1", true}, {"r2", false}, {"r3", false}});

        sendS0Event(epService, "%dex", "!%de_", "a", ".");
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

    private void sendS0Event(EPServiceProvider epService, String p00, String p01, String p02, String p03) {
        SupportBean_S0 bean = new SupportBean_S0(-1, p00, p01, p02, p03);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, Integer intBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
