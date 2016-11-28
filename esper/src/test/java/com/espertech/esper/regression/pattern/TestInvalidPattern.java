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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.bean.SupportBean_N;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestInvalidPattern extends TestCase
{
    private EPServiceProvider epService;
    private final String EVENT_NUM = SupportBean_N.class.getName();
    private final String EVENT_COMPLEX = SupportBeanComplexProps.class.getName();
    private final String EVENT_ALLTYPES = SupportBean.class.getName();

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testInvalid()
    {
        String exceptionText = getSyntaxExceptionPattern(EVENT_NUM + "(doublePrimitive='ss'");
        assertEquals("Incorrect syntax near end-of-input expecting a closing parenthesis ')' but found end-of-input at line 1 column 67, please check the filter specification within the pattern expression [com.espertech.esper.support.bean.SupportBean_N(doublePrimitive='ss']", exceptionText);

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("select * from pattern[(not a=SupportBean) -> SupportBean(theString=a.theString)]");

        // test invalid subselect
        epService.getEPAdministrator().createEPL("create window WaitWindow#keepall as (waitTime int)");
        epService.getEPAdministrator().createEPL("insert into WaitWindow select intPrimitive as waitTime from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));

        try {
            epService.getEPAdministrator().createPattern("timer:interval((select waitTime from WaitWindow))");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Subselects are not allowed within pattern observer parameters, please consider using a variable instead [timer:interval((select waitTime from WaitWindow))]",
                    ex.getMessage());
        }
    }

    public void testStatementException() throws Exception
    {
        String exceptionText;

        exceptionText = getStatementExceptionPattern("timer:at(2,3,4,4,4)");
        assertEquals("Invalid parameter for pattern observer 'timer:at(2,3,4,4,4)': Error computing crontab schedule specification: Invalid combination between days of week and days of month fields for timer:at [timer:at(2,3,4,4,4)]", exceptionText);

        exceptionText = getStatementExceptionPattern("timer:at(*,*,*,*,*,0,-1)");
        assertEquals("Invalid parameter for pattern observer 'timer:at(*,*,*,*,*,0,-1)': Error computing crontab schedule specification: Invalid timezone parameter '-1' for timer:at, expected a string-type value [timer:at(*,*,*,*,*,0,-1)]", exceptionText);

        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + " -> timer:within()");
        assertEquals("Failed to resolve pattern observer 'timer:within()': Pattern guard function 'within' cannot be used as a pattern observer [com.espertech.esper.support.bean.SupportBean -> timer:within()]", exceptionText);

        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + " where timer:interval(100)");
        assertEquals("Failed to resolve pattern guard 'com.espertech.esper.support.bean.SupportBean where timer:interval(100)': Pattern observer function 'interval' cannot be used as a pattern guard [com.espertech.esper.support.bean.SupportBean where timer:interval(100)]", exceptionText);

        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + " -> timer:interval()");
        assertEquals("Invalid parameter for pattern observer 'timer:interval()': Timer-interval observer requires a single numeric or time period parameter [com.espertech.esper.support.bean.SupportBean -> timer:interval()]", exceptionText);

        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + " where timer:within()");
        assertEquals("Invalid parameter for pattern guard 'com.espertech.esper.support.bean.SupportBean where timer:within()': Timer-within guard requires a single numeric or time period parameter [com.espertech.esper.support.bean.SupportBean where timer:within()]", exceptionText);

        // class not found
        exceptionText = getStatementExceptionPattern("dummypkg.dummy()");
        assertEquals("Failed to resolve event type: Event type or class named 'dummypkg.dummy' was not found [dummypkg.dummy()]", exceptionText);

        // simple property not found
        exceptionText = getStatementExceptionPattern(EVENT_NUM + "(dummy=1)");
        assertEquals("Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [com.espertech.esper.support.bean.SupportBean_N(dummy=1)]", exceptionText);

        // nested property not found
        exceptionText = getStatementExceptionPattern(EVENT_NUM + "(dummy.nested=1)");
        assertEquals("Failed to validate filter expression 'dummy.nested=1': Failed to resolve property 'dummy.nested' to a stream or nested property in a stream [com.espertech.esper.support.bean.SupportBean_N(dummy.nested=1)]", exceptionText);

        // property wrong type
        exceptionText = getStatementExceptionPattern(EVENT_NUM + "(intPrimitive='s')");
        assertEquals("Failed to validate filter expression 'intPrimitive=\"s\"': Implicit conversion from datatype 'String' to 'Integer' is not allowed [com.espertech.esper.support.bean.SupportBean_N(intPrimitive='s')]", exceptionText);

        // property not a primitive type
        exceptionText = getStatementExceptionPattern(EVENT_COMPLEX + "(nested=1)");
        assertEquals("Failed to validate filter expression 'nested=1': Implicit conversion from datatype 'Integer' to 'SupportBeanSpecialGetterNested' is not allowed [com.espertech.esper.support.bean.SupportBeanComplexProps(nested=1)]", exceptionText);

        // no tag matches prior use
        exceptionText = getStatementExceptionPattern(EVENT_NUM + "(doublePrimitive=x.abc)");
        assertEquals("Failed to validate filter expression 'doublePrimitive=x.abc': Failed to resolve property 'x.abc' to a stream or nested property in a stream [com.espertech.esper.support.bean.SupportBean_N(doublePrimitive=x.abc)]", exceptionText);

        // range not valid on string
        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + "(theString in [1:2])");
        assertEquals("Failed to validate filter expression 'theString between 1 and 2': Implicit conversion from datatype 'String' to numeric is not allowed [com.espertech.esper.support.bean.SupportBean(theString in [1:2])]", exceptionText);

        // range does not allow string params
        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + "(doubleBoxed in ['a':2])");
        assertEquals("Failed to validate filter expression 'doubleBoxed between \"a\" and 2': Implicit conversion from datatype 'String' to numeric is not allowed [com.espertech.esper.support.bean.SupportBean(doubleBoxed in ['a':2])]", exceptionText);

        // invalid observer arg
        exceptionText = getStatementExceptionPattern("timer:at(9l)");
        assertEquals("Invalid parameter for pattern observer 'timer:at(9)': Invalid number of parameters for timer:at [timer:at(9l)]", exceptionText);

        // invalid guard arg
        exceptionText = getStatementExceptionPattern(EVENT_ALLTYPES + " where timer:within('s')");
        assertEquals("Invalid parameter for pattern guard 'com.espertech.esper.support.bean.SupportBean where timer:within(\"s\")': Timer-within guard requires a single numeric or time period parameter [com.espertech.esper.support.bean.SupportBean where timer:within('s')]", exceptionText);

        // use-result property is wrong type
        exceptionText = getStatementExceptionPattern("x=" + EVENT_ALLTYPES + " -> " + EVENT_ALLTYPES + "(doublePrimitive=x.boolBoxed)");
        assertEquals("Failed to validate filter expression 'doublePrimitive=x.boolBoxed': Implicit conversion from datatype 'Boolean' to 'Double' is not allowed [x=com.espertech.esper.support.bean.SupportBean -> com.espertech.esper.support.bean.SupportBean(doublePrimitive=x.boolBoxed)]", exceptionText);

        // named-parameter for timer:at or timer:interval
        exceptionText = getStatementExceptionPattern("timer:interval(interval:10)");
        assertEquals("Invalid parameter for pattern observer 'timer:interval(interval:10)': Timer-interval observer does not allow named parameters [timer:interval(interval:10)]", exceptionText);
        exceptionText = getStatementExceptionPattern("timer:at(perhaps:10)");
        assertEquals("Invalid parameter for pattern observer 'timer:at(perhaps:10)': timer:at does not allow named parameters [timer:at(perhaps:10)]", exceptionText);
    }

    public void testUseResult()
    {
        final String EVENT = SupportBean_N.class.getName();

        tryValid("na=" + EVENT + " -> nb=" + EVENT + "(doublePrimitive = na.doublePrimitive)");
        tryInvalid("xx=" + EVENT + " -> nb=" + EVENT + "(doublePrimitive = na.doublePrimitive)");
        tryInvalid("na=" + EVENT + " -> nb=" + EVENT + "(doublePrimitive = xx.doublePrimitive)");
        tryInvalid("na=" + EVENT + " -> nb=" + EVENT + "(doublePrimitive = na.xx)");
        tryInvalid("xx=" + EVENT + " -> nb=" + EVENT + "(xx = na.doublePrimitive)");
        tryInvalid("na=" + EVENT + " -> nb=" + EVENT + "(xx = na.xx)");
        tryValid("na=" + EVENT + " -> nb=" + EVENT + "(doublePrimitive = na.doublePrimitive, intBoxed=na.intBoxed)");
        tryValid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in (na.doublePrimitive:na.doubleBoxed))");
        tryValid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.doublePrimitive:na.doubleBoxed])");
        tryValid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.intBoxed:na.intPrimitive])");
        tryInvalid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.intBoxed:na.xx])");
        tryInvalid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.intBoxed:na.boolBoxed])");
        tryInvalid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.xx:na.intPrimitive])");
        tryInvalid("na=" + EVENT + "() -> nb=" + EVENT + "(doublePrimitive in [na.boolBoxed:na.intPrimitive])");
    }

    private void tryInvalid(String eplInvalidPattern)
    {
        try
        {
            epService.getEPAdministrator().createPattern(eplInvalidPattern);
            fail();
        }
        catch (EPException ex)
        {
            // Expected exception
        }
    }

    private String getSyntaxExceptionPattern(String expression)
    {
        String exceptionText = null;
        try
        {
            epService.getEPAdministrator().createPattern(expression);
            fail();
        }
        catch (EPStatementSyntaxException ex)
        {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionPattern pattern=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private String getStatementExceptionPattern(String expression) throws Exception
    {
        return getStatementExceptionPattern(expression, false);
    }

    private String getStatementExceptionPattern(String expression, boolean isLogException) throws Exception
    {
        String exceptionText = null;
        try
        {
            epService.getEPAdministrator().createPattern(expression);
            fail();
        }
        catch (EPStatementSyntaxException es)
        {
            throw es;
        }
        catch (EPStatementException ex)
        {
            // Expected exception
            exceptionText = ex.getMessage();
            if (isLogException)
            {
                log.debug(".getSyntaxExceptionPattern pattern=" + expression, ex);
            }
        }

        return exceptionText;
    }

    private void tryValid(String eplInvalidPattern)
    {
        epService.getEPAdministrator().createPattern(eplInvalidPattern);
    }

    private final static Logger log = LoggerFactory.getLogger(TestInvalidPattern.class);
}
