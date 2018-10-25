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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.fail;

public class PatternInvalid {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternInvalidExpr());
        execs.add(new PatternStatementException());
        execs.add(new PatternUseResult());
        return execs;
    }

    private static class PatternInvalidExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String exceptionText = getSyntaxExceptionPattern(env, "SupportBean_N(doublePrimitive='ss'");
            SupportMessageAssertUtil.assertMessage(exceptionText, "Incorrect syntax near ']' expecting a closing parenthesis ')' but found a right angle bracket ']' at line 1 column 56, please check the filter specification within the pattern expression within the from clause");

            env.compileDeploy("select * from pattern[(not a=SupportBean) -> SupportBean(theString=a.theString)]");

            // test invalid subselect
            String epl = "create window WaitWindow#keepall as (waitTime int);\n" +
                "insert into WaitWindow select intPrimitive as waitTime from SupportBean;\n";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportBean("E1", 100));

            tryInvalidCompile(env, "select * from pattern[timer:interval((select waitTime from WaitWindow))]",
                "Subselects are not allowed within pattern observer parameters, please consider using a variable instead");

            env.undeployAll();
        }
    }

    private static class PatternStatementException implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompileException exception;

            exception = getStatementExceptionPattern(env, "timer:at(2,3,4,4,4)");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(2,3,4,4,4)': Error computing crontab schedule specification: Invalid combination between days of week and days of month fields for timer:at [");

            exception = getStatementExceptionPattern(env, "timer:at(*,*,*,*,*,0,-1)");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(*,*,*,*,*,0,-1)': Error computing crontab schedule specification: Invalid timezone parameter '-1' for timer:at, expected a string-type value [");

            exception = getStatementExceptionPattern(env, "SupportBean -> timer:within()");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve pattern observer 'timer:within()': Pattern guard function 'within' cannot be used as a pattern observer [");

            exception = getStatementExceptionPattern(env, "SupportBean where timer:interval(100)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve pattern guard '" + SupportBean.class.getSimpleName() + " where timer:interval(100)': Pattern observer function 'interval' cannot be used as a pattern guard [");

            exception = getStatementExceptionPattern(env, "SupportBean -> timer:interval()");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:interval()': Timer-interval observer requires a single numeric or time period parameter [");

            exception = getStatementExceptionPattern(env, "SupportBean where timer:within()");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern guard '" + SupportBean.class.getSimpleName() + " where timer:within()': Timer-within guard requires a single numeric or time period parameter [");

            // class not found
            exception = getStatementExceptionPattern(env, "dummypkg.dummy()");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve event type, named window or table by name 'dummypkg.dummy' [");

            // simple property not found
            exception = getStatementExceptionPattern(env, "SupportBean_N(dummy=1)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

            // nested property not found
            exception = getStatementExceptionPattern(env, "SupportBean_N(dummy.nested=1)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'dummy.nested=1': Failed to resolve property 'dummy.nested' to a stream or nested property in a stream [");

            // property wrong type
            exception = getStatementExceptionPattern(env, "SupportBean_N(intPrimitive='s')");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'intPrimitive=\"s\"': Implicit conversion from datatype 'String' to 'Integer' is not allowed [");

            // property not a primitive type
            exception = getStatementExceptionPattern(env, "SupportBeanComplexProps(nested=1)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'nested=1': Implicit conversion from datatype 'Integer' to 'SupportBeanSpecialGetterNested' is not allowed [");

            // no tag matches prior use
            exception = getStatementExceptionPattern(env, "SupportBean_N(doublePrimitive=x.abc)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doublePrimitive=x.abc': Failed to resolve property 'x.abc' to a stream or nested property in a stream [");

            // range not valid on string
            exception = getStatementExceptionPattern(env, "SupportBean(theString in [1:2])");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'theString between 1 and 2': Implicit conversion from datatype 'String' to numeric is not allowed [");

            // range does not allow string params
            exception = getStatementExceptionPattern(env, "SupportBean(doubleBoxed in ['a':2])");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doubleBoxed between \"a\" and 2': Implicit conversion from datatype 'String' to numeric is not allowed [");

            // invalid observer arg
            exception = getStatementExceptionPattern(env, "timer:at(9l)");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(9)': Invalid number of parameters for timer:at");

            // invalid guard arg
            exception = getStatementExceptionPattern(env, "SupportBean where timer:within('s')");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern guard '" + SupportBean.class.getSimpleName() + " where timer:within(\"s\")': Timer-within guard requires a single numeric or time period parameter [");

            // use-result property is wrong type
            exception = getStatementExceptionPattern(env, "x=SupportBean -> SupportBean(doublePrimitive=x.boolBoxed)");
            SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'doublePrimitive=x.boolBoxed': Implicit conversion from datatype 'Boolean' to 'Double' is not allowed [");

            // named-parameter for timer:at or timer:interval
            exception = getStatementExceptionPattern(env, "timer:interval(interval:10)");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:interval(interval:10)': Timer-interval observer does not allow named parameters ");
            exception = getStatementExceptionPattern(env, "timer:at(perhaps:10)");
            SupportMessageAssertUtil.assertMessage(exception, "Invalid parameter for pattern observer 'timer:at(perhaps:10)': timer:at does not allow named parameters");
        }
    }

    private static class PatternUseResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String event = SupportBean_N.class.getSimpleName();

            tryValid(env, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive)");
            tryInvalid(env, "xx=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive)");
            tryInvalid(env, "na=" + event + " -> nb=" + event + "(doublePrimitive = xx.doublePrimitive)");
            tryInvalid(env, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.xx)");
            tryInvalid(env, "xx=" + event + " -> nb=" + event + "(xx = na.doublePrimitive)");
            tryInvalid(env, "na=" + event + " -> nb=" + event + "(xx = na.xx)");
            tryValid(env, "na=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive, intBoxed=na.intBoxed)");
            tryValid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in (na.doublePrimitive:na.doubleBoxed))");
            tryValid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.doublePrimitive:na.doubleBoxed])");
            tryValid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.intPrimitive])");
            tryInvalid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.xx])");
            tryInvalid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.intBoxed:na.boolBoxed])");
            tryInvalid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.xx:na.intPrimitive])");
            tryInvalid(env, "na=" + event + "() -> nb=" + event + "(doublePrimitive in [na.boolBoxed:na.intPrimitive])");
        }
    }

    private static void tryInvalid(RegressionEnvironment env, String eplInvalidPattern) {
        try {
            env.compileWCheckedEx("select * from pattern[" + eplInvalidPattern + "]");
            fail();
        } catch (EPCompileException ex) {
            // Expected exception
        }
    }

    private static String getSyntaxExceptionPattern(RegressionEnvironment env, String expression) {
        String exceptionText = null;
        try {
            env.compileWCheckedEx("select * from pattern[" + expression + "]");
            fail();
        } catch (EPCompileException ex) {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionPattern pattern=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private static EPCompileException getStatementExceptionPattern(RegressionEnvironment env, String expression) {
        return getStatementExceptionPattern(env, expression, false);
    }

    private static EPCompileException getStatementExceptionPattern(RegressionEnvironment env, String expression, boolean isLogException) {
        try {
            env.compileWCheckedEx("select * from pattern[" + expression + "]");
            fail();
        } catch (EPCompileException ex) {
            // Expected exception
            if (isLogException) {
                log.debug(expression, ex);
            }
            return ex;
        }
        throw new IllegalStateException();
    }

    private static void tryValid(RegressionEnvironment env, String expression) {
        env.compileDeploy("select * from pattern[" + expression + "]").undeployAll();
    }

    private final static Logger log = LoggerFactory.getLogger(PatternInvalid.class);
}
