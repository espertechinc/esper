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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEPLInvalid implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalidFuncParams(epService);
        runAssertionInvalidSyntax(epService);
        runAssertionLongTypeConstant(epService);
        runAssertionDifferentJoins(epService);
    }

    private void runAssertionInvalidFuncParams(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        SupportMessageAssertUtil.tryInvalid(epService, "select count(theString, theString, theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'count(theString,theString,theString)': The 'count' function expects at least 1 and up to 2 parameters");

        SupportMessageAssertUtil.tryInvalid(epService, "select leaving(theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'leaving(theString)': The 'leaving' function expects no parameters");
    }

    private void runAssertionInvalidSyntax(EPServiceProvider epService) {
        String exceptionText = getSyntaxExceptionEPL(epService, "select * from *");
        assertEquals("Incorrect syntax near '*' at line 1 column 14, please check the from clause [select * from *]", exceptionText);

        exceptionText = getSyntaxExceptionEPL(epService, "select * from SupportBean a where a.intPrimitive between r.start and r.end");
        assertEquals("Incorrect syntax near 'start' (a reserved keyword) at line 1 column 59, please check the where clause [select * from SupportBean a where a.intPrimitive between r.start and r.end]", exceptionText);

        SupportMessageAssertUtil.tryInvalid(epService, "select * from java.lang.Object(1=2=3)",
                "Failed to validate filter expression '1=2': Invalid use of equals, expecting left-hand side and right-hand side but received 3 expressions");
    }

    private void runAssertionLongTypeConstant(EPServiceProvider epService) {
        String stmtText = "select 2512570244 as value from " + SupportBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(2512570244L, listener.assertOneGetNewAndReset().get("value"));
    }

    private void runAssertionDifferentJoins(EPServiceProvider epService) {
        try {
            epService.getEPAdministrator().createEPL("select *");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: The from-clause is required but has not been specified [select *]", ex.getMessage());
        }

        String streamDef = "select * from " +
                SupportBean.class.getName() + "#length(3) as sa," +
                SupportBean.class.getName() + "#length(3) as sb" +
                " where ";

        String streamDefTwo = "select * from " +
                SupportBean.class.getName() + "#length(3)," +
                SupportMarketDataBean.class.getName() + "#length(3)" +
                " where ";

        tryInvalid(epService, streamDef + "sa.intPrimitive = sb.theString");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.intBoxed");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.longBoxed");

        tryInvalid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.boolPrimitive");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.boolBoxed = sa.boolPrimitive");

        tryInvalid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sX.theString");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sb.theString");

        tryInvalid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive or sa.theString=sX.theString");
        tryValid(epService, streamDef + "sa.intPrimitive = sb.intPrimitive or sb.intBoxed = sa.intPrimitive");

        // try constants
        tryValid(epService, streamDef + "sa.intPrimitive=5");
        tryValid(epService, streamDef + "sa.theString='4'");
        tryValid(epService, streamDef + "sa.theString=\"4\"");
        tryValid(epService, streamDef + "sa.boolPrimitive=false");
        tryValid(epService, streamDef + "sa.longPrimitive=-5L");
        tryValid(epService, streamDef + "sa.doubleBoxed=5.6d");
        tryValid(epService, streamDef + "sa.floatPrimitive=-5.6f");

        tryInvalid(epService, streamDef + "sa.intPrimitive='5'");
        tryInvalid(epService, streamDef + "sa.theString=5");
        tryInvalid(epService, streamDef + "sa.boolBoxed=f");
        tryInvalid(epService, streamDef + "sa.intPrimitive=x");
        tryValid(epService, streamDef + "sa.intPrimitive=5.5");

        // try addition and subtraction
        tryValid(epService, streamDef + "sa.intPrimitive=sa.intBoxed + 5");
        tryValid(epService, streamDef + "sa.intPrimitive=2*sa.intBoxed - sa.intPrimitive/10 + 1");
        tryValid(epService, streamDef + "sa.intPrimitive=2*(sa.intBoxed - sa.intPrimitive)/(10 + 1)");
        tryInvalid(epService, streamDef + "sa.intPrimitive=2*(sa.intBoxed");

        // try comparison
        tryValid(epService, streamDef + "sa.intPrimitive > sa.intBoxed and sb.doublePrimitive < sb.doubleBoxed");
        tryValid(epService, streamDef + "sa.intPrimitive >= sa.intBoxed and sa.doublePrimitive <= sa.doubleBoxed");
        tryValid(epService, streamDef + "sa.intPrimitive > (sa.intBoxed + sb.doublePrimitive)");
        tryInvalid(epService, streamDef + "sa.intPrimitive >= sa.theString");
        tryInvalid(epService, streamDef + "sa.boolBoxed >= sa.boolPrimitive");

        // Try some nested
        tryValid(epService, streamDef + "(sa.intPrimitive=3) or (sa.intBoxed=3 and sa.intPrimitive=1)");
        tryValid(epService, streamDef + "((sa.intPrimitive>3) or (sa.intBoxed<3)) and sa.boolBoxed=false");
        tryValid(epService, streamDef + "(sa.intPrimitive<=3 and sa.intPrimitive>=1) or (sa.boolBoxed=false and sa.boolPrimitive=true)");
        tryInvalid(epService, streamDef + "sa.intPrimitive=3 or (sa.intBoxed=2");
        tryInvalid(epService, streamDef + "sa.intPrimitive=3 or sa.intBoxed=2)");
        tryInvalid(epService, streamDef + "sa.intPrimitive=3 or ((sa.intBoxed=2)");

        // Try some without stream name
        tryInvalid(epService, streamDef + "intPrimitive=3");
        tryValid(epService, streamDefTwo + "intPrimitive=3");

        // Try invalid outer join criteria
        String outerJoinDef = "select * from " +
                SupportBean.class.getName() + "#length(3) as sa " +
                "left outer join " +
                SupportBean.class.getName() + "#length(3) as sb ";
        tryValid(epService, outerJoinDef + "on sa.intPrimitive = sb.intBoxed");
        tryInvalid(epService, outerJoinDef + "on sa.intPrimitive = sb.XX");
        tryInvalid(epService, outerJoinDef + "on sa.XX = sb.XX");
        tryInvalid(epService, outerJoinDef + "on sa.XX = sb.intBoxed");
        tryInvalid(epService, outerJoinDef + "on sa.boolBoxed = sb.intBoxed");
        tryValid(epService, outerJoinDef + "on sa.boolPrimitive = sb.boolBoxed");
        tryInvalid(epService, outerJoinDef + "on sa.boolPrimitive = sb.theString");
        tryInvalid(epService, outerJoinDef + "on sa.intPrimitive <= sb.intBoxed");
        tryInvalid(epService, outerJoinDef + "on sa.intPrimitive = sa.intBoxed");
        tryInvalid(epService, outerJoinDef + "on sb.intPrimitive = sb.intBoxed");
        tryValid(epService, outerJoinDef + "on sb.intPrimitive = sa.intBoxed");
    }

    private void tryInvalid(EPServiceProvider epService, String eplInvalidEPL) {
        try {
            epService.getEPAdministrator().createEPL(eplInvalidEPL);
            fail();
        } catch (EPException ex) {
            // Expected exception
        }
    }

    private void tryValid(EPServiceProvider epService, String invalidEPL) {
        epService.getEPAdministrator().createEPL(invalidEPL);
    }

    private String getSyntaxExceptionEPL(EPServiceProvider epService, String expression) {
        String exceptionText = null;
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        } catch (EPStatementSyntaxException ex) {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionEPL epl=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private final static Logger log = LoggerFactory.getLogger(ExecEPLInvalid.class);
}
