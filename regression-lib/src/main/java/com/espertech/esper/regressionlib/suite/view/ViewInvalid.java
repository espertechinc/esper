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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompileExceptionItem;
import com.espertech.esper.compiler.client.EPCompileExceptionSyntaxItem;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ViewInvalid implements RegressionExecution {
    private final static String EVENT_NUM = SupportBean_N.class.getSimpleName();
    private final static String EVENT_ALLTYPES = SupportBean.class.getSimpleName();

    public void run(RegressionEnvironment env) {
        runAssertionInvalidPropertyExpression(env);
        runAssertionInvalidSyntax(env);
        runAssertionStatementException(env);
        runAssertionInvalidView(env);
    }

    private void runAssertionInvalidPropertyExpression(RegressionEnvironment env) {
        String epl = "@name('s0') @IterableUnbound select * from SupportBean";
        env.compileDeploy(epl);
        env.sendEventBean(new SupportBean());
        EventBean theEvent = env.statement("s0").iterator().next();

        String exceptionText = getSyntaxExceptionProperty("", theEvent);
        assertTrue(exceptionText.startsWith("Property named '' is not a valid property name for this type"));

        exceptionText = getSyntaxExceptionProperty("-", theEvent);
        assertTrue(exceptionText.startsWith("Property named '-' is not a valid property name for this type"));

        exceptionText = getSyntaxExceptionProperty("a[]", theEvent);
        assertTrue(exceptionText.startsWith("Property named 'a[]' is not a valid property name for this type"));

        env.undeployAll();
    }

    private void runAssertionInvalidSyntax(RegressionEnvironment env) {
        // keyword in select clause
        EPCompileExceptionSyntaxItem exception = getSyntaxExceptionView(env, "select inner from MyStream");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'inner' (a reserved keyword) at line 1 column 7, please check the select clause");

        // keyword in from clause
        exception = getSyntaxExceptionView(env, "select something from Outer");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'Outer' (a reserved keyword) at line 1 column 22, please check the from clause");

        // keyword used in package
        exception = getSyntaxExceptionView(env, "select * from com.true.mycompany.MyEvent");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'true' (a reserved keyword) expecting an identifier but found 'true' at line 1 column 18, please check the view specifications within the from clause");

        // keyword as part of identifier
        exception = getSyntaxExceptionView(env, "select * from MyEvent, MyEvent2 where a.day=b.day");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'day' (a reserved keyword) at line 1 column 40, please check the where clause");

        exception = getSyntaxExceptionView(env, "select * * from " + EVENT_NUM);
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near '*' at line 1 column 9 near reserved keyword 'from'");

        // keyword in select clause
        exception = getSyntaxExceptionView(env, "select day from MyEvent, MyEvent2");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'day' (a reserved keyword) at line 1 column 7, please check the select clause");
    }

    private void runAssertionStatementException(RegressionEnvironment env) {
        EPCompileExceptionItem exception;

        // property near to spelling
        exception = getStatementExceptionView(env, "select s0.intPrimitv from SupportBean as s0");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 's0.intPrimitv': Property named 'intPrimitv' is not valid in stream 's0' (did you mean 'intPrimitive'?)");

        exception = getStatementExceptionView(env, "select INTPRIMITIVE from SupportBean");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'INTPRIMITIVE': Property named 'INTPRIMITIVE' is not valid in any stream (did you mean 'intPrimitive'?)");

        exception = getStatementExceptionView(env, "select theStrring from SupportBean");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'theStrring': Property named 'theStrring' is not valid in any stream (did you mean 'theString'?)");

        // aggregation in where clause known
        exception = getStatementExceptionView(env, "select * from SupportBean where sum(intPrimitive) > 10");
        SupportMessageAssertUtil.assertMessage(exception, "Aggregation functions not allowed within filters");

        // class not found
        exception = getStatementExceptionView(env, "select * from dummypkg.dummy()#length(10)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to resolve event type, named window or table by name 'dummypkg.dummy'");

        // invalid view
        exception = getStatementExceptionView(env, "select * from " + EVENT_NUM + ".dummy:dummy(10)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate data window declaration: View name 'dummy:dummy' is not a known view name");

        // keyword used
        exception = getSyntaxExceptionView(env, "select order from SupportBean");
        SupportMessageAssertUtil.assertMessage(exception, "Incorrect syntax near 'order' (a reserved keyword) at line 1 column 7, please check the select clause");

        // invalid view parameter
        exception = getStatementExceptionView(env, "select * from " + EVENT_NUM + "#length('s')");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate data window declaration: Error in view 'length', Length view requires a single integer-type parameter");

        // where-clause relational op has invalid type
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) where theString > 5");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating expression: Failed to validate filter expression 'theString>5': Implicit conversion from datatype 'String' to numeric is not allowed");

        // where-clause has aggregation function
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) where sum(intPrimitive) > 5");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating expression: An aggregate function may not appear in a WHERE clause (use the HAVING clause)");

        // invalid numerical expression
        exception = getStatementExceptionView(env, "select 2 * 's' from " + EVENT_ALLTYPES + "#length(1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression '2*\"s\"': Implicit conversion from datatype 'String' to numeric is not allowed");

        // invalid property in select
        exception = getStatementExceptionView(env, "select a[2].m('a') from " + EVENT_ALLTYPES + "#length(1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'a[2].m('a')': Failed to resolve enumeration method, date-time method or mapped property 'a[2].m('a')': Failed to resolve 'a[2].m' to a property, single-row function, aggregation function, script, stream or class name");

        // select clause uses same "as" name twice
        exception = getStatementExceptionView(env, "select 2 as m, 2 as m from " + EVENT_ALLTYPES + "#length(1)");
        SupportMessageAssertUtil.assertMessage(exception, "Column name 'm' appears more then once in select clause");

        // class in method invocation not found
        exception = getStatementExceptionView(env, "select unknownClass.method() from " + EVENT_NUM + "#length(10)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'unknownClass.method()': Failed to resolve 'unknownClass.method' to a property, single-row function, aggregation function, script, stream or class name");

        // method not found
        exception = getStatementExceptionView(env, "select Math.unknownMethod() from " + EVENT_NUM + "#length(10)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'Math.unknownMethod()': Failed to resolve 'Math.unknownMethod' to a property, single-row function, aggregation function, script, stream or class name");

        // invalid property in group-by
        exception = getStatementExceptionView(env, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by xxx");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate group-by-clause expression 'xxx': Property named 'xxx' is not valid in any stream");

        // group-by not specifying a property
        exception = getStatementExceptionView(env, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by 5");
        SupportMessageAssertUtil.assertMessage(exception, "Group-by expressions must refer to property names");

        // group-by specifying aggregates
        exception = getStatementExceptionView(env, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by sum(intPrimitive)");
        SupportMessageAssertUtil.assertMessage(exception, "Group-by expressions cannot contain aggregate functions");

        // invalid property in having clause
        exception = getStatementExceptionView(env, "select 2 * 's' from " + EVENT_ALLTYPES + "#length(1) group by intPrimitive having xxx > 5");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression '2*\"s\"': Implicit conversion from datatype 'String' to numeric is not allowed");

        // invalid having clause - not a symbol in the group-by (non-aggregate)
        exception = getStatementExceptionView(env, "select sum(intPrimitive) from " + EVENT_ALLTYPES + "#length(1) group by intBoxed having doubleBoxed > 5");
        SupportMessageAssertUtil.assertMessage(exception, "Non-aggregated property 'doubleBoxed' in the HAVING clause must occur in the group-by clause");

        // invalid outer join - not a symbol
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) as aStr " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) on xxxx=yyyy");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating outer-join expression: Failed to validate on-clause join expression 'xxxx=yyyy': Property named 'xxxx' is not valid in any stream");

        // invalid outer join for 3 streams - not a symbol
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s0.intPrimitive = s1.intPrimitive " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s0.intPrimitive = s2.yyyy");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating outer-join expression: Failed to validate on-clause join expression 's0.intPrimitive=s2.yyyy': Failed to resolve property 's2.yyyy' to a stream or nested property in a stream");

        // invalid outer join for 3 streams - wrong stream, the properties in on-clause don't refer to streams
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s0.intPrimitive = s1.intPrimitive " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s0.intPrimitive = s1.intPrimitive");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating outer-join expression: Outer join ON-clause must refer to at least one property of the joined stream for stream 2");

        // invalid outer join - referencing next stream
        exception = getStatementExceptionView(env, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s2.intPrimitive = s1.intPrimitive " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s1.intPrimitive = s2.intPrimitive");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating outer-join expression: Outer join ON-clause invalid scope for property 'intPrimitive', expecting the current or a prior stream scope");

        // invalid outer join - same properties
        exception = getStatementExceptionView(env, "select * from " + EVENT_NUM + "#length(1) as aStr " +
            "left outer join " + EVENT_ALLTYPES + "#length(1) on theString=theString");
        SupportMessageAssertUtil.assertMessage(exception, "Error validating outer-join expression: Outer join ON-clause cannot refer to properties of the same stream");

        // invalid order by
        exception = getStatementExceptionView(env, "select * from " + EVENT_NUM + "#length(1) as aStr order by X");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate order-by-clause expression 'X': Property named 'X' is not valid in any stream");

        // insert into with wildcard - not allowed
        exception = getStatementExceptionView(env, "insert into Google (a, b) select * from " + EVENT_NUM + "#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Wildcard not allowed when insert-into specifies column order");

        // insert into with duplicate column names
        exception = getStatementExceptionView(env, "insert into Google (a, b, a) select boolBoxed, boolPrimitive, intBoxed from " + EVENT_NUM + "#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Property name 'a' appears more then once in insert-into clause");

        // insert into mismatches selected columns
        exception = getStatementExceptionView(env, "insert into Google (a, b, c) select boolBoxed, boolPrimitive from " + EVENT_NUM + "#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Number of supplied values in the select or values clause does not match insert-into clause");

        // mismatched type on coalesce columns
        exception = getStatementExceptionView(env, "select coalesce(boolBoxed, theString) from SupportBean#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'coalesce(boolBoxed,theString)': Implicit conversion not allowed: Cannot coerce to Boolean type java.lang.String");

        // mismatched case compare type
        exception = getStatementExceptionView(env, "select case boolPrimitive when 1 then true end from SupportBean#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'case boolPrimitive when 1 then true end': Implicit conversion not allowed: Cannot coerce to Boolean type java.lang.Integer");

        // mismatched case result type
        exception = getStatementExceptionView(env, "select case when 1=2 then 1 when 1=3 then true end from SupportBean#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'case when 1=2 then 1 when 1=3 then ...(43 chars)': Implicit conversion not allowed: Cannot coerce types java.lang.Integer and java.lang.Boolean");

        // case expression not returning bool
        exception = getStatementExceptionView(env, "select case when 3 then 1 end from SupportBean#length(1) as aStr");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'case when 3 then 1 end': Case node 'when' expressions must return a boolean value");

        // function not known
        exception = getStatementExceptionView(env, "select gogglex(1) from " + EVENT_NUM + "#length(1)");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate select-clause expression 'gogglex(1)': Unknown single-row function, aggregation function or mapped or indexed property named 'gogglex' could not be resolved");

        // insert into column name incorrect
        exception = getStatementExceptionView(env, "insert into Xyz select 1 as dodi from SupportBean;\n" +
            "select pox from pattern[Xyz(yodo=4)]");
        SupportMessageAssertUtil.assertMessage(exception, "Failed to validate filter expression 'yodo=4': Property named 'yodo' is not valid in any stream (did you mean 'dodi'?)");
        env.undeployAll();
    }

    private void runAssertionInvalidView(RegressionEnvironment env) {
        tryInvalid(env, "select * from SupportBean(dummy='a')#length(3)");
        tryValid(env, "select * from SupportBean(theString='a')#length(3)");
        tryInvalid(env, "select * from SupportBean.dummy:length(3)");

        tryInvalid(env, "select djdjdj from SupportBean#length(3)");
        tryValid(env, "select boolBoxed as xx, intPrimitive from SupportBean#length(3)");
        tryInvalid(env, "select boolBoxed as xx, intPrimitive as xx from SupportBean#length(3)");
        tryValid(env, "select boolBoxed as xx, intPrimitive as yy from SupportBean()#length(3)");

        tryValid(env, "select boolBoxed as xx, intPrimitive as yy from SupportBean()#length(3) where boolBoxed = true");
        tryInvalid(env, "select boolBoxed as xx, intPrimitive as yy from SupportBean()#length(3) where xx = true");
    }

    private void tryInvalid(RegressionEnvironment env, String epl) {
        try {
            env.compileWCheckedEx(epl);
            fail();
        } catch (EPCompileException ex) {
            // Expected exception
        }
    }

    private EPCompileExceptionSyntaxItem getSyntaxExceptionView(RegressionEnvironment env, String expression) {
        try {
            env.compileWCheckedEx(expression);
            fail();
        } catch (EPCompileException ex) {
            if (log.isDebugEnabled()) {
                log.debug(".getSyntaxExceptionView expression=" + expression, ex);
            }
            // Expected exception
            return (EPCompileExceptionSyntaxItem) ex.getItems().get(0);
        }
        throw new IllegalStateException();
    }

    private String getSyntaxExceptionProperty(String expression, EventBean theEvent) {
        String exceptionText = null;
        try {
            theEvent.get(expression);
            fail();
        } catch (PropertyAccessException ex) {
            exceptionText = ex.getMessage();
            if (log.isDebugEnabled()) {
                log.debug(".getSyntaxExceptionProperty expression=" + expression, ex);
            }
            // Expected exception
        }

        return exceptionText;
    }

    private EPCompileExceptionItem getStatementExceptionView(RegressionEnvironment env, String expression) {
        return getStatementExceptionView(env, expression, false);
    }

    private EPCompileExceptionItem getStatementExceptionView(RegressionEnvironment env, String expression, boolean isLogException) {
        try {
            env.compileWCheckedEx(expression);
            fail();
        } catch (EPCompileException ex) {
            EPCompileExceptionItem first = ex.getItems().get(0);
            if (isLogException) {
                log.debug(".getStatementExceptionView expression=" + first, first);
            }
            if (first instanceof EPCompileExceptionSyntaxItem) {
                fail();
            }
            return first;
        }
        throw new IllegalStateException();
    }

    private void tryValid(RegressionEnvironment env, String epl) {
        env.compile(epl);
    }

    private final static Logger log = LoggerFactory.getLogger(ViewInvalid.class);
}
