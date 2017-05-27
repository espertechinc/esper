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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EPStatementSyntaxException;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBean_N;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecPatternInvalid implements RegressionExecution {
    private final static String EVENT_NUM = SupportBean_N.class.getName();
    private final static String EVENT_COMPLEX = SupportBeanComplexProps.class.getName();
    private final static String EVENT_ALLTYPES = SupportBean.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionStatementException(epService);
        runAssertionUseResult(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String exceptionText = getSyntaxExceptionPattern(epService, EVENT_NUM + "(doublePrimitive='ss'");
        assertEquals("Incorrect syntax near end-of-input expecting a closing parenthesis ')' but found end-of-input at line 1 column 77, please check the filter specification within the pattern expression [" + SupportBean_N.class.getName() + "(doublePrimitive='ss']", exceptionText);

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("select * from pattern[(not a=SupportBean) -> SupportBean(theString=a.theString)]");

        // test invalid subselect
        epService.getEPAdministrator().createEPL("create window WaitWindow#keepall as (waitTime int)");
        epService.getEPAdministrator().createEPL("insert into WaitWindow select intPrimitive as waitTime from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));

        try {
            epService.getEPAdministrator().createPattern("timer:interval((select waitTime from WaitWindow))");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Subselects are not allowed within pattern observer parameters, please consider using a variable instead [timer:interval((select waitTime from WaitWindow))]",
                    ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStatementException(EPServiceProvider epService) throws Exception {
        EPStatementException exception;

        exception = getStatementExceptionPattern(epService, "timer:at(2,3,4,4,4)");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(2,3,4,4,4)': Error computing crontab schedule specification: Invalid combination between days of week and days of month fields for timer:at [");

        exception = getStatementExceptionPattern(epService, "timer:at(*,*,*,*,*,0,-1)");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(*,*,*,*,*,0,-1)': Error computing crontab schedule specification: Invalid timezone parameter '-1' for timer:at, expected a string-type value [");

        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + " -> timer:within()");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve pattern observer 'timer:within()': Pattern guard function 'within' cannot be used as a pattern observer [");

        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + " where timer:interval(100)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve pattern guard '" + SupportBean.class.getName() + " where timer:interval(100)': Pattern observer function 'interval' cannot be used as a pattern guard [");

        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + " -> timer:interval()");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:interval()': Timer-interval observer requires a single numeric or time period parameter [");

        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + " where timer:within()");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern guard '" + SupportBean.class.getName() + " where timer:within()': Timer-within guard requires a single numeric or time period parameter [");

        // class not found
        exception = getStatementExceptionPattern(epService, "dummypkg.dummy()");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve event type: Event type or class named 'dummypkg.dummy' was not found [");

        // simple property not found
        exception = getStatementExceptionPattern(epService, EVENT_NUM + "(dummy=1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

        // nested property not found
        exception = getStatementExceptionPattern(epService, EVENT_NUM + "(dummy.nested=1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'dummy.nested=1': Failed to resolve property 'dummy.nested' to a stream or nested property in a stream [");

        // property wrong type
        exception = getStatementExceptionPattern(epService, EVENT_NUM + "(intPrimitive='s')");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'intPrimitive=\"s\"': Implicit conversion from datatype 'String' to 'Integer' is not allowed [");

        // property not a primitive type
        exception = getStatementExceptionPattern(epService, EVENT_COMPLEX + "(nested=1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'nested=1': Implicit conversion from datatype 'Integer' to 'SupportBeanSpecialGetterNested' is not allowed [");

        // no tag matches prior use
        exception = getStatementExceptionPattern(epService, EVENT_NUM + "(doublePrimitive=x.abc)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doublePrimitive=x.abc': Failed to resolve property 'x.abc' to a stream or nested property in a stream [");

        // range not valid on string
        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + "(theString in [1:2])");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'theString between 1 and 2': Implicit conversion from datatype 'String' to numeric is not allowed [");

        // range does not allow string params
        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + "(doubleBoxed in ['a':2])");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doubleBoxed between \"a\" and 2': Implicit conversion from datatype 'String' to numeric is not allowed [");

        // invalid observer arg
        exception = getStatementExceptionPattern(epService, "timer:at(9l)");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(9)': Invalid number of parameters for timer:at [timer:at(9l)]");

        // invalid guard arg
        exception = getStatementExceptionPattern(epService, EVENT_ALLTYPES + " where timer:within('s')");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern guard '" + SupportBean.class.getName() + " where timer:within(\"s\")': Timer-within guard requires a single numeric or time period parameter [");

        // use-result property is wrong type
        exception = getStatementExceptionPattern(epService, "x=" + EVENT_ALLTYPES + " -> " + EVENT_ALLTYPES + "(doublePrimitive=x.boolBoxed)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doublePrimitive=x.boolBoxed': Implicit conversion from datatype 'Boolean' to 'Double' is not allowed [");

        // named-parameter for timer:at or timer:interval
        exception = getStatementExceptionPattern(epService, "timer:interval(interval:10)");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:interval(interval:10)': Timer-interval observer does not allow named parameters [timer:interval(interval:10)]");
        exception = getStatementExceptionPattern(epService, "timer:at(perhaps:10)");
        SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(perhaps:10)': timer:at does not allow named parameters [timer:at(perhaps:10)]");
    }

    private void runAssertionUseResult(EPServiceProvider epService) {
        final String event = SupportBean_N.class.getName();

        tryValid(epService, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive)");
        tryInvalid(epService, "xx=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive)");
        tryInvalid(epService, "na=" + event + " -> nb=" + event + "(doublePrimitive = xx.doublePrimitive)");
        tryInvalid(epService, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.xx)");
        tryInvalid(epService, "xx=" + event + " -> nb=" + event + "(xx = na.doublePrimitive)");
        tryInvalid(epService, "na=" + event + " -> nb=" + event + "(xx = na.xx)");
        tryValid(epService, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive, intBoxed=na.intBoxed)");
        tryValid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in (na.doublePrimitive:na.doubleBoxed))");
        tryValid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.doublePrimitive:na.doubleBoxed])");
        tryValid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.intPrimitive])");
        tryInvalid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.xx])");
        tryInvalid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.boolBoxed])");
        tryInvalid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.xx:na.intPrimitive])");
        tryInvalid(epService, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.boolBoxed:na.intPrimitive])");
    }

    private void tryInvalid(EPServiceProvider epService, String eplInvalidPattern) {
        try {
            epService.getEPAdministrator().createPattern(eplInvalidPattern);
            fail();
        } catch (EPException ex) {
            // Expected exception
        }
    }

    private String getSyntaxExceptionPattern(EPServiceProvider epService, String expression) {
        String exceptionText = null;
        try {
            epService.getEPAdministrator().createPattern(expression);
            fail();
        } catch (EPStatementSyntaxException ex) {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionPattern pattern=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private EPStatementException getStatementExceptionPattern(EPServiceProvider epService, String expression) throws Exception {
        return getStatementExceptionPattern(epService, expression, false);
    }

    private EPStatementException getStatementExceptionPattern(EPServiceProvider epService, String expression, boolean isLogException) throws Exception {
        try {
            epService.getEPAdministrator().createPattern(expression);
            fail();
        } catch (EPStatementSyntaxException es) {
            throw es;
        } catch (EPStatementException ex) {
            // Expected exception
            if (isLogException) {
                log.debug(".getSyntaxExceptionPattern pattern=" + expression, ex);
            }
            return ex;
        }
        throw new IllegalStateException();
    }

    private void tryValid(EPServiceProvider epService, String eplInvalidPattern) {
        epService.getEPAdministrator().createPattern(eplInvalidPattern);
    }

    private final static Logger log = LoggerFactory.getLogger(ExecPatternInvalid.class);
}
