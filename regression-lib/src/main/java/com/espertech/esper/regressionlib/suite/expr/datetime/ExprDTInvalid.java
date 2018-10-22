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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprDTInvalid implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String epl;

        // invalid incompatible params
        epl = "select contained.set('hour', 1) from SupportBean_ST0_Container";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.set(\"hour\",1)': Date-time enumeration method 'set' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property but received collection of events of type '" + SupportBean_ST0.class.getName() + "'");

        // invalid incompatible params
        epl = "select window(*).set('hour', 1) from SupportBean#keepall";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'window(*).set(\"hour\",1)': Date-time enumeration method 'set' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property but received collection of events of type 'SupportBean'");

        // invalid incompatible params
        epl = "select utildate.set('invalid') from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.set(\"invalid\")': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value");

        // invalid lambda parameter
        epl = "select utildate.set(x => true) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.set()': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value");

        // invalid no parameter
        epl = "select utildate.set() from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.set()': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value");

        // invalid wrong parameter
        epl = "select utildate.set(1) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.set(1)': Parameters mismatch for date-time method 'set', the method requires an expression providing a string-type calendar field name and an expression providing an integer-type value");

        // invalid wrong parameter
        epl = "select utildate.between('a', 'b') from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.between(\"a\",\"b\")': Error validating date-time method 'between', expected a long-typed, Date-typed or Calendar-typed result for expression parameter 0 but received java.lang.String");

        // invalid wrong parameter
        epl = "select utildate.between(utildate, utildate, 1, true) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.between(utildate,utildate,...(42 chars)': Error validating date-time method 'between', expected a boolean-type result for expression parameter 2 but received int");

        // mispatch parameter to input
        epl = "select utildate.format(java.time.format.DateTimeFormatter.ISO_ORDINAL_DATE) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.format(ParseCaseSensitive(...(114 chars)': Date-time enumeration method 'format' invalid format, expected string-format or DateFormat but received java.time.format.DateTimeFormatter");
        epl = "select zoneddate.format(SimpleDateFormat.getInstance()) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'zoneddate.format(SimpleDateFormat.g...(48 chars)': Date-time enumeration method 'format' invalid format, expected string-format or DateTimeFormatter but received java.text.DateFormat");

        // invalid date format null
        epl = "select utildate.format(null) from SupportDateTime";
        tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'utildate.format(null)': Error validating date-time method 'format', expected any of [String, DateFormat, DateTimeFormatter]-type result for expression parameter 0 but received null");
    }
}
