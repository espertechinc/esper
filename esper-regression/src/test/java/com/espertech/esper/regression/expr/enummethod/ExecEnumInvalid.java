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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

public class ExecEnumInvalid implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportBeanComplexProps", SupportBeanComplexProps.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
        configuration.addImport(SupportBean_ST0_Container.class);
        configuration.addPlugInSingleRowFunction("makeTest", SupportBean_ST0_Container.class.getName(), "makeTest");
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl;

        // no parameter while one is expected
        epl = "select contained.take() from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.take()': Parameters mismatch for enumeration method 'take', the method requires an (non-lambda) expression providing count [select contained.take() from SupportBean_ST0_Container]");

        // primitive array property
        epl = "select arrayProperty.where(x=>x.boolPrimitive) from SupportBeanComplexProps";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'arrayProperty.where()': Error validating enumeration method 'where' parameter 0: Failed to validate declared expression body expression 'x.boolPrimitive': Failed to resolve property 'x.boolPrimitive' to a stream or nested property in a stream [select arrayProperty.where(x=>x.boolPrimitive) from SupportBeanComplexProps]");

        // property not there
        epl = "select contained.where(x=>x.dummy = 1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where()': Error validating enumeration method 'where' parameter 0: Failed to validate declared expression body expression 'x.dummy=1': Failed to resolve property 'x.dummy' to a stream or nested property in a stream [select contained.where(x=>x.dummy = 1) from SupportBean_ST0_Container]");
        epl = "select * from SupportBean(products.where(p => code = '1'))";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Failed to validate filter expression 'products.where()': Failed to resolve 'products.where' to a property, single-row function, aggregation function, script, stream or class name ");

        // test not an enumeration method
        epl = "select contained.notAMethod(x=>x.boolPrimitive) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.notAMethod()': Could not find event property, enumeration method or instance method named 'notAMethod' in collection of events of type 'SupportBean_ST0' [select contained.notAMethod(x=>x.boolPrimitive) from SupportBean_ST0_Container]");

        // invalid lambda expression for non-lambda func
        epl = "select makeTest(x=>1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'makeTest()': Unexpected lambda-expression encountered as parameter to UDF or static method 'makeTest' [select makeTest(x=>1) from SupportBean_ST0_Container]");

        // invalid lambda expression for non-lambda func
        epl = "select SupportBean_ST0_Container.makeTest(x=>1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'SupportBean_ST0_Container.makeTest()': Unexpected lambda-expression encountered as parameter to UDF or static method 'makeTest' [select SupportBean_ST0_Container.makeTest(x=>1) from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select contained.take('a') from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.take('a')': Failed to resolve enumeration method, date-time method or mapped property 'contained.take('a')': Error validating enumeration method 'take', expected a number-type result for expression parameter 0 but received java.lang.String [select contained.take('a') from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select contained.take(x => x.p00) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.take()': Parameters mismatch for enumeration method 'take', the method requires an (non-lambda) expression providing count, but receives a lambda expression [select contained.take(x => x.p00) from SupportBean_ST0_Container]");

        // invalid too many lambda parameter
        epl = "select contained.where((x,y,z) => true) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where()': Parameters mismatch for enumeration method 'where', the method requires a lambda expression providing predicate, but receives a 3-parameter lambda expression [select contained.where((x,y,z) => true) from SupportBean_ST0_Container]");

        // invalid no parameter
        epl = "select contained.where() from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where()': Parameters mismatch for enumeration method 'where', the method has multiple footprints accepting a lambda expression providing predicate, or a 2-parameter lambda expression providing (predicate, index), but receives no parameters [select contained.where() from SupportBean_ST0_Container]");

        // invalid no parameter
        epl = "select window(intPrimitive).takeLast() from SupportBean#length(2)";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'window(intPrimitive).takeLast()': Parameters mismatch for enumeration method 'takeLast', the method requires an (non-lambda) expression providing count [select window(intPrimitive).takeLast() from SupportBean#length(2)]");

        // invalid wrong parameter
        epl = "select contained.where(x=>true,y=>true) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where(,)': Parameters mismatch for enumeration method 'where', the method has multiple footprints accepting a lambda expression providing predicate, or a 2-parameter lambda expression providing (predicate, index), but receives a lambda expression and a lambda expression [select contained.where(x=>true,y=>true) from SupportBean_ST0_Container]");

        // invalid wrong parameter
        epl = "select contained.where(1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where(1)': Parameters mismatch for enumeration method 'where', the method requires a lambda expression providing predicate, but receives an (non-lambda) expression [select contained.where(1) from SupportBean_ST0_Container]");

        // invalid too many parameter
        epl = "select contained.where(1,2) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.where(1,2)': Parameters mismatch for enumeration method 'where', the method has multiple footprints accepting a lambda expression providing predicate, or a 2-parameter lambda expression providing (predicate, index), but receives an (non-lambda) expression and an (non-lambda) expression [select contained.where(1,2) from SupportBean_ST0_Container]");

        // subselect multiple columns
        epl = "select (select theString, intPrimitive from SupportBean#lastevent).where(x=>x.boolPrimitive) from SupportBean_ST0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'theString.where()': Error validating enumeration method 'where' parameter 0: Failed to validate declared expression body expression 'x.boolPrimitive': Failed to resolve property 'x.boolPrimitive' to a stream or nested property in a stream [select (select theString, intPrimitive from SupportBean#lastevent).where(x=>x.boolPrimitive) from SupportBean_ST0]");

        // subselect individual column
        epl = "select (select theString from SupportBean#lastevent).where(x=>x.boolPrimitive) from SupportBean_ST0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'theString.where()': Error validating enumeration method 'where' parameter 0: Failed to validate declared expression body expression 'x.boolPrimitive': Failed to resolve property 'x.boolPrimitive' to a stream or nested property in a stream [select (select theString from SupportBean#lastevent).where(x=>x.boolPrimitive) from SupportBean_ST0]");

        // aggregation
        epl = "select avg(intPrimitive).where(x=>x.boolPrimitive) from SupportBean_ST0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Incorrect syntax near '(' ('avg' is a reserved keyword) at line 1 column 10");

        // invalid incompatible params
        epl = "select contained.allOf(x => 1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.allOf()': Error validating enumeration method 'allOf', expected a boolean-type result for expression parameter 0 but received int [select contained.allOf(x => 1) from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select contained.allOf(x => 1) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.allOf()': Error validating enumeration method 'allOf', expected a boolean-type result for expression parameter 0 but received int [select contained.allOf(x => 1) from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select contained.aggregate(0, (result, item) => result || ',') from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.aggregate(0,)': Error validating enumeration method 'aggregate' parameter 1: Failed to validate declared expression body expression 'result||\",\"': Implicit conversion from datatype 'Integer' to string is not allowed [select contained.aggregate(0, (result, item) => result || ',') from SupportBean_ST0_Container]");

        // invalid incompatible params
        epl = "select contained.average(x => x.id) from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.average()': Error validating enumeration method 'average', expected a number-type result for expression parameter 0 but received java.lang.String [select contained.average(x => x.id) from SupportBean_ST0_Container]");

        // not a property
        epl = "select contained.firstof().dummy from SupportBean_ST0_Container";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.firstof().dummy()': Failed to resolve method 'dummy': Could not find enumeration method, date-time method or instance method named 'dummy' in class '" + SupportBean_ST0.class.getName() + "' taking no parameters [select contained.firstof().dummy from SupportBean_ST0_Container]");
    }
}
