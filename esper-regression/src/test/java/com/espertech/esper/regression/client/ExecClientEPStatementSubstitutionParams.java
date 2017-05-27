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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.io.Serializable;

import static org.junit.Assert.*;

public class ExecClientEPStatementSubstitutionParams implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNamedParameter(epService);
        runAssertionMethodInvocation(epService);
        runAssertionPattern(epService);
        runAssertionSubselect(epService);
        runAssertionSimpleOneParameter(epService);
        runAssertionSimpleTwoParameterFilter(epService);
        runAssertionSimpleTwoParameterWhere(epService);
        runAssertionSimpleTwoParameterWhereNamed(epService);
        runAssertionSimpleNoParameter(epService);
        runAssertionInvalidParameterNotSet(epService);
        runAssertionInvalidParameterType(epService);
        runAssertionInvalidNoParameters(epService);
        runAssertionInvalidSetObject(epService);
        runAssertionInvalidCreateEPL(epService);
        runAssertionInvalidCreatePattern(epService);
        runAssertionInvalidCompile(epService);
        runAssertionInvalidViewParameter(epService);
    }

    private void runAssertionNamedParameter(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String epl = "select ?:my/value/int as c0 from SupportBean(theString = ?:somevalue, intPrimitive=?:my/value/int, longPrimitive=?:/my/value/long)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(epl);
        prepared.setObject("somevalue", "E1");
        prepared.setObject("my/value/int", 10);
        prepared.setObject("/my/value/long", 100L);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        epService.getEPAdministrator().create(prepared).addListener(listenerOne);

        SupportBean event = new SupportBean("E1", 10);
        event.setLongPrimitive(100);
        epService.getEPRuntime().sendEvent(event);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "c0".split(","), new Object[]{10});

        SupportMessageAssertUtil.tryInvalid(epService, "select ?,?:a from SupportBean",
                "Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");

        SupportMessageAssertUtil.tryInvalid(epService, "select ?:select from SupportBean",
                "Incorrect syntax near ':' ('select' is a reserved keyword) at line 1 column 8 near reserved keyword 'select' [");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMethodInvocation(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL("select * from SupportBean(theString = ?.getTheString())");
        prepared.setObject(1, new SupportBean("E1", 0));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        epService.getEPAdministrator().create(prepared).addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertTrue(listenerOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPattern(EPServiceProvider epService) {
        String stmt = SupportBean.class.getName() + "(theString=?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().preparePattern(stmt);

        prepared.setObject(1, "e1");
        EPStatement statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statement.addListener(listenerOne);
        assertEquals("select * from pattern [" + SupportBean.class.getName() + "(theString=\"e1\")]", statement.getText());

        prepared.setObject(1, "e2");
        statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statement.addListener(listenerTwo);
        assertEquals("select * from pattern [com.espertech.esper.supportregression.bean.SupportBean(theString=\"e2\")]", statement.getText());

        epService.getEPRuntime().sendEvent(new SupportBean("e2", 10));
        assertFalse(listenerOne.isInvoked());
        assertTrue(listenerTwo.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 10));
        assertFalse(listenerTwo.isInvoked());
        assertTrue(listenerOne.getAndClearIsInvoked());

        statement.destroy();
        prepared = epService.getEPAdministrator().prepareEPL("create window MyWindow#time(?) as " + SupportBean.class.getName());
        prepared.setObject(1, 300);
        statement = epService.getEPAdministrator().create(prepared);
        assertEquals("create window MyWindow#time(300) as select * from " + SupportBean.class.getName(), statement.getText());
    }

    private void runAssertionSubselect(EPServiceProvider epService) {
        String stmtText = "select (" +
                "select symbol from " + SupportMarketDataBean.class.getName() + "(symbol=?)#lastevent) as mysymbol from " +
                SupportBean.class.getName();

        EPPreparedStatement preparedStmt = epService.getEPAdministrator().prepareEPL(stmtText);

        preparedStmt.setObject(1, "S1");
        EPStatement stmtS1 = epService.getEPAdministrator().create(preparedStmt);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtS1.addListener(listenerOne);

        preparedStmt.setObject(1, "S2");
        EPStatement stmtS2 = epService.getEPAdministrator().create(preparedStmt);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtS2.addListener(listenerTwo);

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean("e1", -1));
        assertEquals(null, listenerOne.assertOneGetNewAndReset().get("mysymbol"));
        assertEquals(null, listenerTwo.assertOneGetNewAndReset().get("mysymbol"));

        // test one non-matching event
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("XX", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("e1", -1));
        assertEquals(null, listenerOne.assertOneGetNewAndReset().get("mysymbol"));
        assertEquals(null, listenerTwo.assertOneGetNewAndReset().get("mysymbol"));

        // test S2 matching event
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("S2", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("e1", -1));
        assertEquals(null, listenerOne.assertOneGetNewAndReset().get("mysymbol"));
        assertEquals("S2", listenerTwo.assertOneGetNewAndReset().get("mysymbol"));

        // test S1 matching event
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("S1", 0, 0L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("e1", -1));
        assertEquals("S1", listenerOne.assertOneGetNewAndReset().get("mysymbol"));
        assertEquals("S2", listenerTwo.assertOneGetNewAndReset().get("mysymbol"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSimpleOneParameter(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        prepared.setObject(1, "e1");
        EPStatement statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statement.addListener(listenerOne);
        assertEquals("select * from " + SupportBean.class.getName() + "(theString=\"e1\")", statement.getText());

        prepared.setObject(1, "e2");
        statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statement.addListener(listenerTwo);
        assertEquals("select * from com.espertech.esper.supportregression.bean.SupportBean(theString=\"e2\")", statement.getText());

        epService.getEPRuntime().sendEvent(new SupportBean("e2", 10));
        assertFalse(listenerOne.isInvoked());
        assertTrue(listenerTwo.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 10));
        assertFalse(listenerTwo.isInvoked());
        assertTrue(listenerOne.getAndClearIsInvoked());

        // Test substitution parameter and inheritance in key matching
        epService.getEPAdministrator().getConfiguration().addEventType("MyEventOne", MyEventOne.class);
        String epl = "select * from MyEventOne(key = ?)";
        EPPreparedStatement preparedStatement = epService.getEPAdministrator().prepareEPL(epl);
        MyObjectKeyInterface lKey = new MyObjectKeyInterface();
        preparedStatement.setObject(1, lKey);
        statement = epService.getEPAdministrator().create(preparedStatement);
        statement.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new MyEventOne(lKey));
        assertTrue(listenerOne.getAndClearIsInvoked());

        // Test substitution parameter and concrete subclass in key matching 
        epService.getEPAdministrator().getConfiguration().addEventType("MyEventTwo", MyEventTwo.class);
        epl = "select * from MyEventTwo where key = ?";
        preparedStatement = epService.getEPAdministrator().prepareEPL(epl);
        MyObjectKeyConcrete cKey = new MyObjectKeyConcrete();
        preparedStatement.setObject(1, cKey);
        statement = epService.getEPAdministrator().create(preparedStatement);
        statement.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new MyEventTwo(cKey));
        assertTrue(listenerOne.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSimpleTwoParameterFilter(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?,intPrimitive=?)";
        runSimpleTwoParameter(epService, stmt, null, true);
    }

    private void runAssertionSimpleTwoParameterWhere(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + " where theString=? and intPrimitive=?";
        runSimpleTwoParameter(epService, stmt, null, false);
    }

    private void runAssertionSimpleTwoParameterWhereNamed(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + " where theString=? and intPrimitive=?";
        runSimpleTwoParameter(epService, stmt, "s1", false);
    }

    private void runSimpleTwoParameter(EPServiceProvider epService, String stmtText, String statementName, boolean compareText) {
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmtText);

        prepared.setObject(1, "e1");
        prepared.setObject(2, 1);
        EPStatement statement;
        if (statementName != null) {
            statement = epService.getEPAdministrator().create(prepared, statementName);
        } else {
            statement = epService.getEPAdministrator().create(prepared);
        }
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statement.addListener(listenerOne);
        if (compareText) {
            assertEquals("select * from " + SupportBean.class.getName() + "(theString=\"e1\" and intPrimitive=1)", statement.getText());
        }

        prepared.setObject(1, "e2");
        prepared.setObject(2, 2);
        if (statementName != null) {
            statement = epService.getEPAdministrator().create(prepared, statementName + "_1");
        } else {
            statement = epService.getEPAdministrator().create(prepared);
        }
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statement.addListener(listenerTwo);
        if (compareText) {
            assertEquals("select * from " + SupportBean.class.getName() + "(theString=\"e2\" and intPrimitive=2)", statement.getText());
        }

        epService.getEPRuntime().sendEvent(new SupportBean("e2", 2));
        assertFalse(listenerOne.isInvoked());
        assertTrue(listenerTwo.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 1));
        assertFalse(listenerTwo.isInvoked());
        assertTrue(listenerOne.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 2));
        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        statement.destroy();
    }

    private void runAssertionSimpleNoParameter(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=\"e1\")";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        EPStatement statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statement.addListener(listenerOne);
        assertEquals("select * from " + SupportBean.class.getName() + "(theString=\"e1\")", statement.getText());

        statement = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statement.addListener(listenerTwo);
        assertEquals("select * from com.espertech.esper.supportregression.bean.SupportBean(theString=\"e1\")", statement.getText());

        epService.getEPRuntime().sendEvent(new SupportBean("e2", 10));
        assertFalse(listenerOne.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 10));
        assertTrue(listenerOne.getAndClearIsInvoked());
        assertTrue(listenerTwo.getAndClearIsInvoked());

        statement.destroy();
    }

    private void runAssertionInvalidParameterNotSet(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        try {
            epService.getEPAdministrator().create(prepared);
            fail();
        } catch (EPException ex) {
            assertEquals("Substitution parameter value for index 1 not set, please provide a value for this parameter", ex.getMessage());
        }

        stmt = "select * from " + SupportBean.class.getName() + "(theString in (?, ?))";
        prepared = epService.getEPAdministrator().prepareEPL(stmt);

        try {
            epService.getEPAdministrator().create(prepared);
            fail();
        } catch (EPException ex) {
            // expected
        }

        try {
            prepared.setObject(1, "");
            epService.getEPAdministrator().create(prepared);
            fail();
        } catch (EPException ex) {
            // expected
        }

        // success
        prepared.setObject(2, "");
        epService.getEPAdministrator().create(prepared).destroy();
    }

    private void runAssertionInvalidParameterType(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        try {
            prepared.setObject(1, -1);
            epService.getEPAdministrator().create(prepared);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Failed to validate filter expression 'theString=-1': Implicit conversion from datatype 'Integer' to 'String' is not allowed [");
        }
    }

    private void runAssertionInvalidNoParameters(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString='ABC')";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        try {
            prepared.setObject(1, -1);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Statement does not have substitution parameters indicated by the '?' character", ex.getMessage());
        }
    }

    private void runAssertionInvalidSetObject(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmt);

        try {
            prepared.setObject(0, "");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Substitution parameter index starts at 1", ex.getMessage());
        }

        try {
            prepared.setObject(2, "");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Invalid substitution parameter index of 2 supplied, the maximum for this statement is 1", ex.getMessage());
        }
    }

    private void runAssertionInvalidCreateEPL(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        try {
            epService.getEPAdministrator().createEPL(stmt);
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Failed to validate filter expression 'theString=?': Invalid use of substitution parameters marked by '?' in statement, use the prepare method to prepare statements with substitution parameters");
        }
    }

    private void runAssertionInvalidCreatePattern(EPServiceProvider epService) {
        String stmt = SupportBean.class.getName() + "(theString=?)";
        try {
            epService.getEPAdministrator().createPattern(stmt);
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex,
                    "Failed to validate filter expression 'theString=?': Invalid use of substitution parameters marked by '?' in statement, use the prepare method to prepare statements");
        }
    }

    private void runAssertionInvalidCompile(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "(theString=?)";
        try {
            epService.getEPAdministrator().compileEPL(stmt);
        } catch (EPException ex) {
            assertEquals("Invalid use of substitution parameters marked by '?' in statement, use the prepare method to prepare statements with substitution parameters", ex.getMessage());
        }
    }

    private void runAssertionInvalidViewParameter(EPServiceProvider epService) {
        String stmt = "select * from " + SupportBean.class.getName() + "#length(?)";
        try {
            epService.getEPAdministrator().prepareEPL(stmt);
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Incorrect syntax near '?' expecting a closing parenthesis ')' but found a questionmark '?' at line 1 column 70, please check the view specifications within the from clause [");
        }
    }

    public interface IKey extends Serializable {
    }

    public static class MyObjectKeyInterface implements IKey {
    }

    public static class MyEventOne {
        private IKey key;

        public MyEventOne(IKey key) {
            this.key = key;
        }

        public IKey getKey() {
            return key;
        }
    }

    public static class MyObjectKeyConcrete implements Serializable {
    }

    public static class MyEventTwo implements Serializable {
        private MyObjectKeyConcrete key;

        public MyEventTwo(MyObjectKeyConcrete key) {
            this.key = key;
        }

        public MyObjectKeyConcrete getKey() {
            return key;
        }
    }
}
