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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.EPStatementSyntaxException;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestInvalidEPL extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInvalidFuncParams() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        SupportMessageAssertUtil.tryInvalid(epService, "select count(theString, theString, theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'count(theString,theString,theString)': The 'count' function expects at least 1 and up to 2 parameters");

        SupportMessageAssertUtil.tryInvalid(epService, "select leaving(theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'leaving(theString)': The 'leaving' function expects no parameters");
    }

    public void testInvalidSyntax()
    {
        String exceptionText = getSyntaxExceptionEPL("select * from *");
        assertEquals("Incorrect syntax near '*' at line 1 column 14, please check the from clause [select * from *]", exceptionText);

        exceptionText = getSyntaxExceptionEPL("select * from SupportBean a where a.intPrimitive between r.start and r.end");
        assertEquals("Incorrect syntax near 'start' (a reserved keyword) at line 1 column 59, please check the where clause [select * from SupportBean a where a.intPrimitive between r.start and r.end]", exceptionText);

        SupportMessageAssertUtil.tryInvalid(epService, "select * from java.lang.Object(1=2=3)",
                "Failed to validate filter expression '1=2': Invalid use of equals, expecting left-hand side and right-hand side but received 3 expressions");
    }

    public void testLongTypeConstant()
    {
        String stmtText = "select 2512570244 as value from " + SupportBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(2512570244L, listener.assertOneGetNewAndReset().get("value"));
    }

    public void testDifferentJoins()
    {
        try {
            epService.getEPAdministrator().createEPL("select *");
            fail();
        }
        catch (EPStatementException ex) {
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

        tryInvalid(streamDef + "sa.intPrimitive = sb.theString");
        tryValid(streamDef + "sa.intPrimitive = sb.intBoxed");
        tryValid(streamDef + "sa.intPrimitive = sb.intPrimitive");
        tryValid(streamDef + "sa.intPrimitive = sb.longBoxed");

        tryInvalid(streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.boolPrimitive");
        tryValid(streamDef + "sa.intPrimitive = sb.intPrimitive and sb.boolBoxed = sa.boolPrimitive");

        tryInvalid(streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sX.theString");
        tryValid(streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sb.theString");

        tryInvalid(streamDef + "sa.intPrimitive = sb.intPrimitive or sa.theString=sX.theString");
        tryValid(streamDef + "sa.intPrimitive = sb.intPrimitive or sb.intBoxed = sa.intPrimitive");

        // try constants
        tryValid(streamDef + "sa.intPrimitive=5");
        tryValid(streamDef + "sa.theString='4'");
        tryValid(streamDef + "sa.theString=\"4\"");
        tryValid(streamDef + "sa.boolPrimitive=false");
        tryValid(streamDef + "sa.longPrimitive=-5L");
        tryValid(streamDef + "sa.doubleBoxed=5.6d");
        tryValid(streamDef + "sa.floatPrimitive=-5.6f");

        tryInvalid(streamDef + "sa.intPrimitive='5'");
        tryInvalid(streamDef + "sa.theString=5");
        tryInvalid(streamDef + "sa.boolBoxed=f");
        tryInvalid(streamDef + "sa.intPrimitive=x");
        tryValid(streamDef + "sa.intPrimitive=5.5");

        // try addition and subtraction
        tryValid(streamDef + "sa.intPrimitive=sa.intBoxed + 5");
        tryValid(streamDef + "sa.intPrimitive=2*sa.intBoxed - sa.intPrimitive/10 + 1");
        tryValid(streamDef + "sa.intPrimitive=2*(sa.intBoxed - sa.intPrimitive)/(10 + 1)");
        tryInvalid(streamDef + "sa.intPrimitive=2*(sa.intBoxed");

        // try comparison
        tryValid(streamDef + "sa.intPrimitive > sa.intBoxed and sb.doublePrimitive < sb.doubleBoxed");
        tryValid(streamDef + "sa.intPrimitive >= sa.intBoxed and sa.doublePrimitive <= sa.doubleBoxed");
        tryValid(streamDef + "sa.intPrimitive > (sa.intBoxed + sb.doublePrimitive)");
        tryInvalid(streamDef + "sa.intPrimitive >= sa.theString");
        tryInvalid(streamDef + "sa.boolBoxed >= sa.boolPrimitive");

        // Try some nested
        tryValid(streamDef + "(sa.intPrimitive=3) or (sa.intBoxed=3 and sa.intPrimitive=1)");
        tryValid(streamDef + "((sa.intPrimitive>3) or (sa.intBoxed<3)) and sa.boolBoxed=false");
        tryValid(streamDef + "(sa.intPrimitive<=3 and sa.intPrimitive>=1) or (sa.boolBoxed=false and sa.boolPrimitive=true)");
        tryInvalid(streamDef + "sa.intPrimitive=3 or (sa.intBoxed=2");
        tryInvalid(streamDef + "sa.intPrimitive=3 or sa.intBoxed=2)");
        tryInvalid(streamDef + "sa.intPrimitive=3 or ((sa.intBoxed=2)");

        // Try some without stream name
        tryInvalid(streamDef + "intPrimitive=3");
        tryValid(streamDefTwo + "intPrimitive=3");

        // Try invalid outer join criteria
        String outerJoinDef = "select * from " +
                SupportBean.class.getName() + "#length(3) as sa " +
                "left outer join " +
                SupportBean.class.getName() + "#length(3) as sb ";
        tryValid(outerJoinDef + "on sa.intPrimitive = sb.intBoxed");
        tryInvalid(outerJoinDef + "on sa.intPrimitive = sb.XX");
        tryInvalid(outerJoinDef + "on sa.XX = sb.XX");
        tryInvalid(outerJoinDef + "on sa.XX = sb.intBoxed");
        tryInvalid(outerJoinDef + "on sa.boolBoxed = sb.intBoxed");
        tryValid(outerJoinDef + "on sa.boolPrimitive = sb.boolBoxed");
        tryInvalid(outerJoinDef + "on sa.boolPrimitive = sb.theString");
        tryInvalid(outerJoinDef + "on sa.intPrimitive <= sb.intBoxed");
        tryInvalid(outerJoinDef + "on sa.intPrimitive = sa.intBoxed");
        tryInvalid(outerJoinDef + "on sb.intPrimitive = sb.intBoxed");
        tryValid(outerJoinDef + "on sb.intPrimitive = sa.intBoxed");
    }

    private void tryInvalid(String eplInvalidEPL)
    {
        try
        {
            epService.getEPAdministrator().createEPL(eplInvalidEPL);
            fail();
        }
        catch (EPException ex)
        {
            // Expected exception
        }
    }

    private void tryValid(String invalidEPL)
    {
        epService.getEPAdministrator().createEPL(invalidEPL);
    }

    private String getSyntaxExceptionEPL(String expression)
    {
        String exceptionText = null;
        try
        {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPStatementSyntaxException ex)
        {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionEPL epl=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private final static Logger log = LoggerFactory.getLogger(TestInvalidEPL.class);
}
