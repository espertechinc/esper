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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_N;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecViewInvalid implements RegressionExecution {
    private final static String EVENT_NUM = SupportBean_N.class.getName();
    private final static String EVENT_ALLTYPES = SupportBean.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalidPropertyExpression(epService);
        runAssertionInvalidSyntax(epService);
        runAssertionStatementException(epService);
        runAssertionInvalidView(epService);
    }

    private void runAssertionInvalidPropertyExpression(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("@IterableUnbound select * from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = stmt.iterator().next();

        String exceptionText = getSyntaxExceptionProperty("", theEvent);
        assertEquals("Property named '' is not a valid property name for this type", exceptionText);

        exceptionText = getSyntaxExceptionProperty("-", theEvent);
        assertEquals("Property named '-' is not a valid property name for this type", exceptionText);

        exceptionText = getSyntaxExceptionProperty("a[]", theEvent);
        assertEquals("Property named 'a[]' is not a valid property name for this type", exceptionText);

        stmt.destroy();
    }

    private void runAssertionInvalidSyntax(EPServiceProvider epService) {
        // keyword in select clause
        EPStatementSyntaxException exception = getSyntaxExceptionView(epService, "select inner from MyStream");
        assertMessage(exception, "Incorrect syntax near 'inner' (a reserved keyword) at line 1 column 7, please check the select clause [");

        // keyword in from clause
        exception = getSyntaxExceptionView(epService, "select something from Outer");
        assertMessage(exception, "Incorrect syntax near 'Outer' (a reserved keyword) at line 1 column 22, please check the from clause [");

        // keyword used in package
        exception = getSyntaxExceptionView(epService, "select * from com.true.mycompany.MyEvent");
        assertMessage(exception, "Incorrect syntax near 'true' (a reserved keyword) expecting an identifier but found 'true' at line 1 column 18, please check the view specifications within the from clause [");

        // keyword as part of identifier
        exception = getSyntaxExceptionView(epService, "select * from MyEvent, MyEvent2 where a.day=b.day");
        assertMessage(exception, "Incorrect syntax near 'day' (a reserved keyword) at line 1 column 40, please check the where clause [");

        exception = getSyntaxExceptionView(epService, "select * * from " + EVENT_NUM);
        assertMessage(exception, "Incorrect syntax near '*' at line 1 column 9 near reserved keyword 'from' [");

        // keyword in select clause
        exception = getSyntaxExceptionView(epService, "select day from MyEvent, MyEvent2");
        assertMessage(exception, "Incorrect syntax near 'day' (a reserved keyword) at line 1 column 7, please check the select clause [");
    }

    private void runAssertionStatementException(EPServiceProvider epService) throws Exception {
        EPStatementException exception;

        // property near to spelling
        exception = getStatementExceptionView(epService, "select s0.intPrimitv from " + SupportBean.class.getName() + " as s0");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 's0.intPrimitv': Property named 'intPrimitv' is not valid in stream 's0' (did you mean 'intPrimitive'?) [");

        exception = getStatementExceptionView(epService, "select INTPRIMITIVE from " + SupportBean.class.getName());
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'INTPRIMITIVE': Property named 'INTPRIMITIVE' is not valid in any stream (did you mean 'intPrimitive'?) [");

        exception = getStatementExceptionView(epService, "select theStrring from " + SupportBean.class.getName());
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'theStrring': Property named 'theStrring' is not valid in any stream (did you mean 'theString'?) [");

        // aggregation in where clause known
        exception = getStatementExceptionView(epService, "select * from " + SupportBean.class.getName() + " where sum(intPrimitive) > 10");
        assertMessage(exception, "Aggregation functions not allowed within filters [");

        // class not found
        exception = getStatementExceptionView(epService, "select * from dummypkg.dummy()#length(10)");
        assertMessage(exception, "Failed to resolve event type: Event type or class named 'dummypkg.dummy' was not found [select * from dummypkg.dummy()#length(10)]");

        // invalid view
        exception = getStatementExceptionView(epService, "select * from " + EVENT_NUM + ".dummy:dummy(10)");
        assertMessage(exception, "Error starting statement: View name 'dummy:dummy' is not a known view name [");

        // keyword used
        exception = getSyntaxExceptionView(epService, "select order from " + SupportBean.class.getName());
        assertMessage(exception, "Incorrect syntax near 'order' (a reserved keyword) at line 1 column 7, please check the select clause [");

        // invalid view parameter
        exception = getStatementExceptionView(epService, "select * from " + EVENT_NUM + "#length('s')");
        assertMessage(exception, "Error starting statement: Error in view 'length', Length view requires a single integer-type parameter [");

        // where-clause relational op has invalid type
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) where theString > 5");
        assertMessage(exception, "Error validating expression: Failed to validate filter expression 'theString>5': Implicit conversion from datatype 'String' to numeric is not allowed [");

        // where-clause has aggregation function
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) where sum(intPrimitive) > 5");
        assertMessage(exception, "Error validating expression: An aggregate function may not appear in a WHERE clause (use the HAVING clause) [");

        // invalid numerical expression
        exception = getStatementExceptionView(epService, "select 2 * 's' from " + EVENT_ALLTYPES + "#length(1)");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression '2*\"s\"': Implicit conversion from datatype 'String' to numeric is not allowed [");

        // invalid property in select
        exception = getStatementExceptionView(epService, "select a[2].m('a') from " + EVENT_ALLTYPES + "#length(1)");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'a[2].m('a')': Failed to resolve enumeration method, date-time method or mapped property 'a[2].m('a')': Failed to resolve 'a[2].m' to a property, single-row function, aggregation function, script, stream or class name [");

        // select clause uses same "as" name twice
        exception = getStatementExceptionView(epService, "select 2 as m, 2 as m from " + EVENT_ALLTYPES + "#length(1)");
        assertMessage(exception, "Error starting statement: Column name 'm' appears more then once in select clause [");

        // class in method invocation not found
        exception = getStatementExceptionView(epService, "select unknownClass.method() from " + EVENT_NUM + "#length(10)");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'unknownClass.method()': Failed to resolve 'unknownClass.method' to a property, single-row function, aggregation function, script, stream or class name [");

        // method not found
        exception = getStatementExceptionView(epService, "select Math.unknownMethod() from " + EVENT_NUM + "#length(10)");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'Math.unknownMethod()': Failed to resolve 'Math.unknownMethod' to a property, single-row function, aggregation function, script, stream or class name [");

        // invalid property in group-by
        exception = getStatementExceptionView(epService, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by xxx");
        assertMessage(exception, "Error starting statement: Failed to validate group-by-clause expression 'xxx': Property named 'xxx' is not valid in any stream [");

        // group-by not specifying a property
        exception = getStatementExceptionView(epService, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by 5");
        assertMessage(exception, "Error starting statement: Group-by expressions must refer to property names [");

        // group-by specifying aggregates
        exception = getStatementExceptionView(epService, "select intPrimitive from " + EVENT_ALLTYPES + "#length(1) group by sum(intPrimitive)");
        assertMessage(exception, "Error starting statement: Group-by expressions cannot contain aggregate functions [");

        // invalid property in having clause
        exception = getStatementExceptionView(epService, "select 2 * 's' from " + EVENT_ALLTYPES + "#length(1) group by intPrimitive having xxx > 5");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression '2*\"s\"': Implicit conversion from datatype 'String' to numeric is not allowed [");

        // invalid having clause - not a symbol in the group-by (non-aggregate)
        exception = getStatementExceptionView(epService, "select sum(intPrimitive) from " + EVENT_ALLTYPES + "#length(1) group by intBoxed having doubleBoxed > 5");
        assertMessage(exception, "Error starting statement: Non-aggregated property 'doubleBoxed' in the HAVING clause must occur in the group-by clause [");

        // invalid outer join - not a symbol
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) as aStr " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) on xxxx=yyyy");
        assertMessage(exception, "Error validating expression: Failed to validate on-clause join expression 'xxxx=yyyy': Property named 'xxxx' is not valid in any stream [");

        // invalid outer join for 3 streams - not a symbol
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s0.intPrimitive = s1.intPrimitive " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s0.intPrimitive = s2.yyyy");
        assertMessage(exception, "Error validating expression: Failed to validate on-clause join expression 's0.intPrimitive=s2.yyyy': Failed to resolve property 's2.yyyy' to a stream or nested property in a stream [");

        // invalid outer join for 3 streams - wrong stream, the properties in on-clause don't refer to streams
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s0.intPrimitive = s1.intPrimitive " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s0.intPrimitive = s1.intPrimitive");
        assertMessage(exception, "Error validating expression: Outer join ON-clause must refer to at least one property of the joined stream for stream 2 [");

        // invalid outer join - referencing next stream
        exception = getStatementExceptionView(epService, "select * from " + EVENT_ALLTYPES + "#length(1) as s0 " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s1 on s2.intPrimitive = s1.intPrimitive " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) as s2 on s1.intPrimitive = s2.intPrimitive");
        assertMessage(exception, "Error validating expression: Outer join ON-clause invalid scope for property 'intPrimitive', expecting the current or a prior stream scope [");

        // invalid outer join - same properties
        exception = getStatementExceptionView(epService, "select * from " + EVENT_NUM + "#length(1) as aStr " +
                "left outer join " + EVENT_ALLTYPES + "#length(1) on theString=theString");
        assertMessage(exception, "Error validating expression: Outer join ON-clause cannot refer to properties of the same stream [");

        // invalid order by
        exception = getStatementExceptionView(epService, "select * from " + EVENT_NUM + "#length(1) as aStr order by X");
        assertMessage(exception, "Error starting statement: Failed to validate order-by-clause expression 'X': Property named 'X' is not valid in any stream [");

        // insert into with wildcard - not allowed
        exception = getStatementExceptionView(epService, "insert into Google (a, b) select * from " + EVENT_NUM + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Wildcard not allowed when insert-into specifies column order [");

        // insert into with duplicate column names
        exception = getStatementExceptionView(epService, "insert into Google (a, b, a) select boolBoxed, boolPrimitive, intBoxed from " + EVENT_NUM + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Property name 'a' appears more then once in insert-into clause [");

        // insert into mismatches selected columns
        exception = getStatementExceptionView(epService, "insert into Google (a, b, c) select boolBoxed, boolPrimitive from " + EVENT_NUM + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Number of supplied values in the select or values clause does not match insert-into clause [");

        // mismatched type on coalesce columns
        exception = getStatementExceptionView(epService, "select coalesce(boolBoxed, theString) from " + SupportBean.class.getName() + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'coalesce(boolBoxed,theString)': Implicit conversion not allowed: Cannot coerce to Boolean type java.lang.String [");

        // mismatched case compare type
        exception = getStatementExceptionView(epService, "select case boolPrimitive when 1 then true end from " + SupportBean.class.getName() + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'case boolPrimitive when 1 then true end': Implicit conversion not allowed: Cannot coerce to Boolean type java.lang.Integer [");

        // mismatched case result type
        exception = getStatementExceptionView(epService, "select case when 1=2 then 1 when 1=3 then true end from " + SupportBean.class.getName() + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'case when 1=2 then 1 when 1=3 then ...(43 chars)': Implicit conversion not allowed: Cannot coerce types java.lang.Integer and java.lang.Boolean [");

        // case expression not returning bool
        exception = getStatementExceptionView(epService, "select case when 3 then 1 end from " + SupportBean.class.getName() + "#length(1) as aStr");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'case when 3 then 1 end': Case node 'when' expressions must return a boolean value [");

        // function not known
        exception = getStatementExceptionView(epService, "select gogglex(1) from " + EVENT_NUM + "#length(1)");
        assertMessage(exception, "Error starting statement: Failed to validate select-clause expression 'gogglex(1)': Unknown single-row function, aggregation function or mapped or indexed property named 'gogglex' could not be resolved [");

        // insert into column name incorrect
        epService.getEPAdministrator().createEPL("insert into Xyz select 1 as dodi from java.lang.String");
        exception = getStatementExceptionView(epService, "select pox from pattern[Xyz(yodo=4)]");
        assertMessage(exception, "Failed to validate filter expression 'yodo=4': Property named 'yodo' is not valid in any stream (did you mean 'dodi'?) [select pox from pattern[Xyz(yodo=4)]]");
    }

    private void runAssertionInvalidView(EPServiceProvider epService) {
        String eventClass = SupportBean.class.getName();

        tryInvalid(epService, "select * from " + eventClass + "(dummy='a')#length(3)");
        tryValid(epService, "select * from " + eventClass + "(theString='a')#length(3)");
        tryInvalid(epService, "select * from " + eventClass + ".dummy:length(3)");

        tryInvalid(epService, "select djdjdj from " + eventClass + "#length(3)");
        tryValid(epService, "select boolBoxed as xx, intPrimitive from " + eventClass + "#length(3)");
        tryInvalid(epService, "select boolBoxed as xx, intPrimitive as xx from " + eventClass + "#length(3)");
        tryValid(epService, "select boolBoxed as xx, intPrimitive as yy from " + eventClass + "()#length(3)");

        tryValid(epService, "select boolBoxed as xx, intPrimitive as yy from " + eventClass + "()#length(3)" +
                " where boolBoxed = true");
        tryInvalid(epService, "select boolBoxed as xx, intPrimitive as yy from " + eventClass + "()#length(3)" +
                " where xx = true");
    }

    private void tryInvalid(EPServiceProvider epService, String viewStmt) {
        try {
            epService.getEPAdministrator().createEPL(viewStmt);
            fail();
        } catch (EPException ex) {
            // Expected exception
        }
    }

    private EPStatementSyntaxException getSyntaxExceptionView(EPServiceProvider epService, String expression) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        } catch (EPStatementSyntaxException ex) {
            if (log.isDebugEnabled()) {
                log.debug(".getSyntaxExceptionView expression=" + expression, ex);
            }
            // Expected exception
            return ex;
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

    private EPStatementException getStatementExceptionView(EPServiceProvider epService, String expression) throws Exception {
        return getStatementExceptionView(epService, expression, false);
    }

    private EPStatementException getStatementExceptionView(EPServiceProvider epService, String expression, boolean isLogException) throws Exception {
        try {
            epService.getEPAdministrator().createEPL(expression, "MyStatement");
            fail();
        } catch (EPStatementSyntaxException es) {
            throw es;
        } catch (EPStatementException ex) {
            // Expected exception
            if (isLogException) {
                log.debug(".getStatementExceptionView expression=" + expression, ex);
            }
            return ex;
        }
        throw new IllegalStateException();
    }

    private void tryValid(EPServiceProvider epService, String viewStmt) {
        epService.getEPAdministrator().createEPL(viewStmt);
    }

    private final static Logger log = LoggerFactory.getLogger(ExecViewInvalid.class);
}
