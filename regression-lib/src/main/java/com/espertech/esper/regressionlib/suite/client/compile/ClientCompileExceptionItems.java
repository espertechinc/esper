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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompileExceptionItem;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class ClientCompileExceptionItems {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileExceptionTwoItems());
        execs.add(new ClientCompileExceptionMultiLineMultiItem());
        execs.add(new ClientCompileExeptionEPLWNewline());
        return execs;
    }

    public static class ClientCompileExceptionMultiLineMultiItem implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema\n" +
                "MySchemaOne\n" +
                "(\n" +
                "  col1 Wrong\n" +
                ");\n" +
                "create schema\n" +
                "MySchemaTwo\n" +
                "(\n" +
                "  col1 WrongTwo\n" +
                ");\n";
            try {
                EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments());
                fail();
            } catch (EPCompileException ex) {
                assertMessage(ex, "Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1'");
                assertEquals(2, ex.getItems().size());
                assertItem(ex.getItems().get(0), "create schema MySchemaOne (   col1 Wrong )", 1, "Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1'");
                assertItem(ex.getItems().get(1), "create schema MySchemaTwo (   col1 WrongTwo )", 6, "Nestable type configuration encountered an unexpected property type name 'WrongTwo' for property 'col1'");
            }
        }
    }

    public static class ClientCompileExceptionTwoItems implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema MySchemaOne (col1 Wrong);\n" +
                "create schema MySchemaTwo (col1 WrongTwo);\n";
            try {
                EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments());
                fail();
            } catch (EPCompileException ex) {
                assertMessage(ex, "Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1'");
                assertEquals(2, ex.getItems().size());
                assertItem(ex.getItems().get(0), "create schema MySchemaOne (col1 Wrong)", 1, "Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1'");
                assertItem(ex.getItems().get(1), "create schema MySchemaTwo (col1 WrongTwo)", 2, "Nestable type configuration encountered an unexpected property type name 'WrongTwo' for property 'col1'");
            }
        }
    }

    public static class ClientCompileExeptionEPLWNewline implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
                EPCompilerProvider.getCompiler().compile("XX\nX", new CompilerArguments());
                fail();
            } catch (EPCompileException ex) {
                assertMessage(ex, "Incorrect syntax near 'XX' [XX X]");
                assertEquals(1, ex.getItems().size());
                assertItem(ex.getItems().get(0), "XX X", 1, "Incorrect syntax near 'XX'");
            }
        }
    }

    private static void assertItem(EPCompileExceptionItem item, String expression, int lineNumber, String expectedMsg) {
        assertEquals(expression, item.getExpression());
        assertEquals(lineNumber, item.getLineNumber());
        assertMessage(item.getCause().getMessage(), expectedMsg);
    }
}
