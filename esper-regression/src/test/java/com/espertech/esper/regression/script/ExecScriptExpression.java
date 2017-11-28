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
package com.espertech.esper.regression.script;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.script.MyImportedClass;
import com.espertech.esper.supportregression.script.SupportScriptUtil;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecScriptExpression implements RegressionExecution {

    private static final boolean TEST_MVEL = false;

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create schema ItemEvent(id string)");

        runAssertionQuoteEscape(epService);
        runAssertionScriptReturningEvents(epService);
        runAssertionDocSamples(epService);
        runAssertionInvalidRegardlessDialect(epService);
        runAssertionInvalidScriptJS(epService);
        runAssertionInvalidScriptMVEL(epService);
        runAssertionScripts(epService);
        runAssertionParserMVELSelectNoArgConstant(epService);
        runAssertionJavaScriptStatelessReturnPassArgs(epService);
        runAssertionMVELStatelessReturnPassArgs(epService);
        runAssertionSubqueryParam(epService);
    }

    private void runAssertionSubqueryParam(EPServiceProvider epService) throws Exception {
        String epl = "expression double js:myJSFunc(stringvalue) [\n" +
                "  calcScore(stringvalue);\n" +
                "  function calcScore(stringvalue) {\n" +
                "    return parseFloat(stringvalue);\n" +
                "  }\n" +
                "]\n" +
                "select myJSFunc((select theString from SupportBean#lastevent)) as c0 from SupportBean_S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("20", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(20d, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportBean("30", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(30d, listener.assertOneGetNewAndReset().get("c0"));

        stmt.destroy();
    }

    private void runAssertionQuoteEscape(EPServiceProvider epService) throws Exception {
        String eplSLComment = "create expression f(params)[\n" +
                "  // I'am...\n" +
                "];";
        DeploymentResult resultOne = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplSLComment);

        String eplMLComment = "create expression g(params)[\n" +
                "  /* I'am... */" +
                "];";
        DeploymentResult resultTwo = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplMLComment);

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultOne.getDeploymentId());
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultTwo.getDeploymentId());
    }

    private void runAssertionScriptReturningEvents(EPServiceProvider epService) {
        runAssertionScriptReturningEvents(epService, false);
        runAssertionScriptReturningEvents(epService, true);

        SupportMessageAssertUtil.tryInvalid(epService, "expression double @type(ItemEvent) fib(num) [] select fib(1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'fib(1)': The @type annotation is only allowed when the invocation target returns EventBean instances");
    }

    private void runAssertionDocSamples(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(ColorEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(RFIDEvent.class);
        String epl;

        epl = "expression double fib(num) [" +
                "fib(num); " +
                "function fib(n) { " +
                "  if(n <= 1) " +
                "    return n; " +
                "  return fib(n-1) + fib(n-2); " +
                "};" +
                "]" +
                "select fib(intPrimitive) from SupportBean";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        if (TEST_MVEL) {
            epl = "expression mvel:printColors(colors) [" +
                    "String c = null;" +
                    "for (c : colors) {" +
                    "   System.out.println(c);" +
                    "}" +
                    "]" +
                    "select printColors(colors) from ColorEvent";
            EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
            stmt.addListener(listener);
            epService.getEPRuntime().sendEvent(new ColorEvent());
            stmt.destroy();
        }

        if (SupportScriptUtil.JAVA_VERSION <= 1.7) {
            epl = "expression js:printColors(colorEvent) [" +
                    "importClass (java.lang.System);" +
                    "importClass (java.util.Arrays);" +
                    "System.out.println(Arrays.toString(colorEvent.getColors()));" +
                    "]" +
                    "select printColors(colorEvent) from ColorEvent as colorEvent";
        } else {
            epl = "expression js:printColors(colorEvent) [" +
                    "print(java.util.Arrays.toString(colorEvent.getColors()));" +
                    "]" +
                    "select printColors(colorEvent) from ColorEvent as colorEvent";
        }
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new ColorEvent());
        epService.getEPAdministrator().destroyAllStatements();

        epl = "expression boolean js:setFlag(name, value, returnValue) [\n" +
                "  if (returnValue) epl.setScriptAttribute(name, value);\n" +
                "  returnValue;\n" +
                "]\n" +
                "expression js:getFlag(name) [\n" +
                "  epl.getScriptAttribute(name);\n" +
                "]\n" +
                "select getFlag('loc') as flag from RFIDEvent(zone = 'Z1' and \n" +
                "  (setFlag('loc', true, loc = 'A') or setFlag('loc', false, loc = 'B')) )";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidRegardlessDialect(EPServiceProvider epService) {
        // parameter defined twice
        tryInvalidExact(epService, "expression js:abc(p1, p1) [/* text */] select * from SupportBean",
                "Invalid script parameters for script 'abc', parameter 'p1' is defined more then once [expression js:abc(p1, p1) [/* text */] select * from SupportBean]");

        // invalid dialect
        tryInvalidExact(epService, "expression dummy:abc() [10] select * from SupportBean",
                "Failed to obtain script engine for dialect 'dummy' for script 'abc' [expression dummy:abc() [10] select * from SupportBean]");

        // not found
        tryInvalidExact(epService, "select abc() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'abc': Unknown single-row function, expression declaration, script or aggregation function named 'abc' could not be resolved [select abc() from SupportBean]");

        // test incorrect number of parameters
        tryInvalidExact(epService, "expression js:abc() [10] select abc(1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'abc(1)': Invalid number of parameters for script 'abc', expected 0 parameters but received 1 parameters [expression js:abc() [10] select abc(1) from SupportBean]");

        // test expression name overlap
        tryInvalidExact(epService, "expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean]");

        // test expression name overlap with parameters
        tryInvalidExact(epService, "expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean]");

        // test script name overlap with expression declaration
        tryInvalidExact(epService, "expression js:abc() [10] expression abc {10} select abc() from SupportBean",
                "Script name 'abc' overlaps with another expression of the same name [expression js:abc() [10] expression abc {10} select abc() from SupportBean]");

        // fails to resolve return type
        tryInvalidExact(epService, "expression dummy js:abc() [10] select abc() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'abc()': Failed to resolve return type 'dummy' specified for script 'abc' [expression dummy js:abc() [10] select abc() from SupportBean]");
    }

    private void runAssertionInvalidScriptJS(EPServiceProvider epService) {

        if (SupportScriptUtil.JAVA_VERSION <= 1.7) {
            tryInvalidContains(epService, "expression js:abc[dummy abc = 1;] select * from SupportBean",
                    "missing ; before statement");

            tryInvalidContains(epService, "expression js:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                    "invalid return");
        } else {
            tryInvalidContains(epService, "expression js:abc[dummy abc = 1;] select * from SupportBean",
                    "Expected ; but found");

            tryInvalidContains(epService, "expression js:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                    "Invalid return statement");
        }

        tryInvalidExact(epService, "expression js:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 18 near reserved keyword 'select' [expression js:abc[] select * from SupportBean]");

        // empty script
        epService.getEPAdministrator().createEPL("expression js:abc[\n] select * from SupportBean");

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression js:abc() [throw new Error(\"Some error\");] select * from SupportBean#keepall where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression js:abc[dummy;] select * from SupportBean#keepall where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('ABC') expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ] select callIt().countOf(v => v < 6) from SupportBean").addListener(listener);
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        } catch (Exception ex) {
            assertTrue("Message is: " + ex.getMessage(), ex.getMessage().contains("Unexpected exception in statement 'ABC': "));
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidScriptMVEL(EPServiceProvider epService) {

        if (!TEST_MVEL) {
            return;
        }

        // mvel return type check
        tryInvalidExact(epService, "expression java.lang.String mvel:abc[10] select * from SupportBean where abc()",
                "Failed to validate filter expression 'abc()': Return type and declared type not compatible for script 'abc', known return type is java.lang.Integer versus declared return type java.lang.String [expression java.lang.String mvel:abc[10] select * from SupportBean where abc()]");

        // undeclared variable
        tryInvalidExact(epService, "expression mvel:abc[dummy;] select * from SupportBean",
                "For script 'abc' the variable 'dummy' has not been declared and is not a parameter [expression mvel:abc[dummy;] select * from SupportBean]");

        // invalid assignment
        tryInvalidContains(epService, "expression mvel:abc[dummy abc = 1;] select * from SupportBean",
                "Exception compiling MVEL script 'abc'");

        // syntax problem
        tryInvalidContains(epService, "expression mvel:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                "unable to resolve method using strict-mode");

        // empty brackets
        tryInvalidExact(epService, "expression mvel:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 20 near reserved keyword 'select' [expression mvel:abc[] select * from SupportBean]");

        // empty script
        epService.getEPAdministrator().createEPL("expression mvel:abc[/* */] select * from SupportBean");

        // unused expression
        epService.getEPAdministrator().createEPL("expression mvel:abc(aa) [return aa..bb(1);] select * from SupportBean");

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression mvel:abc() [Integer a = null; a + 1;] select * from SupportBean#keepall where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    public void tryInvalidExact(EPServiceProvider epService, String expression, String message) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        } catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public void tryInvalidContains(EPServiceProvider epService, String expression, String part) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        } catch (EPStatementException ex) {
            assertTrue("Message not containing text '" + part + "' : " + ex.getMessage(), ex.getMessage().contains(part));
        }
    }

    private void runAssertionScripts(EPServiceProvider epService) throws Exception {

        // test different return types
        tryReturnTypes(epService, "js");
        if (TEST_MVEL) {
            tryReturnTypes(epService, "mvel");
        }

        // test void return type
        tryVoidReturnType(epService, "js");
        if (TEST_MVEL) {
            tryVoidReturnType(epService, "js");
        }

        // test enumeration method
        // Not supported: tryEnumeration("expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ]"); returns NativeArray which is a Rhino-specific array wrapper
        if (TEST_MVEL) {
            tryEnumeration(epService, "expression Integer[] mvel:callIt() [ Integer[] array = {2, 8, 5, 9}; return array; ]");
        }

        // test script props
        trySetScriptProp(epService, "js");
        if (TEST_MVEL) {
            trySetScriptProp(epService, "mvel");
        }

        // test variable
        tryPassVariable(epService, "js");
        if (TEST_MVEL) {
            tryPassVariable(epService, "mvel");
        }

        // test passing an event
        tryPassEvent(epService, "js");
        if (TEST_MVEL) {
            tryPassEvent(epService, "mvel");
        }

        // test returning an object
        tryReturnObject(epService, "js");
        if (TEST_MVEL) {
            tryReturnObject(epService, "mvel");
        }

        // test datetime method
        tryDatetime(epService, "js");
        if (TEST_MVEL) {
            tryDatetime(epService, "mvel");
        }

        // test unnamed expression
        tryUnnamedInSelectClause(epService, "js");
        if (TEST_MVEL) {
            tryUnnamedInSelectClause(epService, "mvel");
        }

        // test import
        epService.getEPAdministrator().getConfiguration().addImport(MyImportedClass.class);
        if (SupportScriptUtil.JAVA_VERSION <= 1.7) {
            tryImports(epService, "expression MyImportedClass js:callOne() [ importClass(" + MyImportedClass.class.getName() + "); new MyImportedClass() ] ");
        } else {
            tryImports(epService, "expression MyImportedClass js:callOne() [ " +
                    "var MyJavaClass = Java.type('" + MyImportedClass.class.getName() + "');" +
                    "new MyJavaClass() ] ");
        }
        if (TEST_MVEL) {
            tryImports(epService, "expression MyImportedClass mvel:callOne() [ import " + MyImportedClass.class.getName() + "; new MyImportedClass() ] ");
        }

        // test overloading script
        epService.getEPAdministrator().getConfiguration().addImport(MyImportedClass.class);
        tryOverloaded(epService, "js");
        if (TEST_MVEL) {
            tryOverloaded(epService, "mvel");
        }

        // test nested invocation
        tryNested(epService, "js");
        if (TEST_MVEL) {
            tryNested(epService, "mvel");
        }

        tryAggregation(epService);

        tryDeployArrayInScript(epService);

        tryCreateExpressionWArrayAllocate(epService);
    }

    private void tryCreateExpressionWArrayAllocate(EPServiceProvider epService) {
        String epl = "@name('first') create expression double js:test(bar) [\n" +
                "test(bar);\n" +
                "function test(bar) {\n" +
                "  var test=[];\n" +
                "  return -1.0;\n" +
                "}]\n";
        epService.getEPAdministrator().createEPL(epl);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select test('a') as c0 from SupportBean_S0").addListener(listener);
        listener.reset();
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{-1d});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryDeployArrayInScript(EPServiceProvider epService) throws Exception {
        String epl = "expression string js:myFunc(arg) [\n" +
                "  function replace(text, values, replacement){\n" +
                "    return text.replace(replacement, values[0]);\n" +
                "  }\n" +
                "  replace(\"A B C\", [\"X\"], \"B\")\n" +
                "]\n" +
                "select\n" +
                "myFunc(*)\n" +
                "from SupportBean;";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());
    }

    private void runAssertionParserMVELSelectNoArgConstant(EPServiceProvider epService) {
        if (TEST_MVEL) {
            tryParseMVEL(epService, "\n\t  10    \n\n\t\t", Integer.class, 10);
            tryParseMVEL(epService, "10", Integer.class, 10);
            tryParseMVEL(epService, "5*5", Integer.class, 25);
            tryParseMVEL(epService, "\"abc\"", String.class, "abc");
            tryParseMVEL(epService, " \"abc\"     ", String.class, "abc");
            tryParseMVEL(epService, "'def'", String.class, "def");
            tryParseMVEL(epService, " 'def' ", String.class, "def");
            tryParseMVEL(epService, " new String[] {'a'}", String[].class, new String[]{"a"});
        }

        tryParseJS(epService, "\n\t  10.0    \n\n\t\t", Object.class, 10.0);
        tryParseJS(epService, "10.0", Object.class, 10.0);
        tryParseJS(epService, "5*5.0", Object.class, 25.0);
        tryParseJS(epService, "\"abc\"", Object.class, "abc");
        tryParseJS(epService, " \"abc\"     ", Object.class, "abc");
        tryParseJS(epService, "'def'", Object.class, "def");
        tryParseJS(epService, " 'def' ", Object.class, "def");
    }

    private void runAssertionJavaScriptStatelessReturnPassArgs(EPServiceProvider epService) {
        Object[][] testData;
        String expression;

        expression = "fib(num);" +
                "function fib(n) {" +
                "  if(n <= 1) return n; " +
                "  return fib(n-1) + fib(n-2); " +
                "};";
        testData = new Object[][]{
                {new SupportBean("E1", 20), 6765.0},
        };
        trySelect(epService, "expression double js:abc(num) [ " + expression + " ]", "abc(intPrimitive)", Double.class, testData);

        testData = new Object[][]{
                {new SupportBean("E1", 5), 50.0},
                {new SupportBean("E1", 6), 60.0}
        };
        trySelect(epService, "expression js:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", Object.class, testData);
    }

    private void runAssertionMVELStatelessReturnPassArgs(EPServiceProvider epService) {
        if (!TEST_MVEL) {
            return;
        }

        Object[][] testData;
        String expression;

        testData = new Object[][]{
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60}
        };
        trySelect(epService, "expression mvel:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", Integer.class, testData);

        expression = "if (theString.equals('E1')) " +
                "  return myint * 10;" +
                "else " +
                "  return myint * 5;";
        testData = new Object[][]{
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60},
                {new SupportBean("E2", 7), 35}
        };
        trySelect(epService, "expression mvel:abc(myint, theString) [" + expression + "]", "abc(intPrimitive, theString)", Object.class, testData);
        trySelect(epService, "expression int mvel:abc(myint, theString) [" + expression + "]", "abc(intPrimitive, theString)", Integer.class, testData);

        expression = "a + Integer.toString(b)";
        testData = new Object[][]{
                {new SupportBean("E1", 5), "E15"},
                {new SupportBean("E1", 6), "E16"},
                {new SupportBean("E2", 7), "E27"}
        };
        trySelect(epService, "expression mvel:abc(a, b) [" + expression + "]", "abc(theString, intPrimitive)", String.class, testData);
    }

    private void tryVoidReturnType(EPServiceProvider epService, String dialect) {
        Object[][] testData;
        String expression;

        expression = "expression void " + dialect + ":mysetter() [ epl.setScriptAttribute('a', 1); ]";
        testData = new Object[][]{
                {new SupportBean("E1", 20), null},
                {new SupportBean("E1", 10), null},
        };
        trySelect(epService, expression, "mysetter()", Object.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void trySetScriptProp(EPServiceProvider epService, String dialect) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "expression " + dialect + ":getFlag() [" +
                        "  epl.getScriptAttribute('flag');" +
                        "]" +
                        "expression boolean " + dialect + ":setFlag(flagValue) [" +
                        "  epl.setScriptAttribute('flag', flagValue);" +
                        "  flagValue;" +
                        "]" +
                        "select getFlag() as val from SupportBean(theString = 'E1' or setFlag(intPrimitive > 0))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        assertEquals(true, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryPassVariable(EPServiceProvider epService, String dialect) {

        Object[][] testData;
        String expression;

        epService.getEPAdministrator().createEPL("create variable long THRESHOLD = 100");

        expression = "expression long " + dialect + ":thresholdAdder(numToAdd, th) [ th + numToAdd; ]";
        testData = new Object[][]{
                {new SupportBean("E1", 20), 120L},
                {new SupportBean("E1", 10), 110L},
        };
        trySelect(epService, expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        epService.getEPRuntime().setVariableValue("THRESHOLD", 1);
        testData = new Object[][]{
                {new SupportBean("E1", 20), 21L},
                {new SupportBean("E1", 10), 11L},
        };
        trySelect(epService, expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryPassEvent(EPServiceProvider epService, String dialect) {

        Object[][] testData;
        String expression;

        expression = "expression int " + dialect + ":callIt(bean) [ bean.getIntPrimitive() + 1; ]";
        testData = new Object[][]{
                {new SupportBean("E1", 20), 21},
                {new SupportBean("E1", 10), 11},
        };
        trySelect(epService, expression, "callIt(sb)", Integer.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryReturnObject(EPServiceProvider epService, String dialect) {

        String expression = "expression " + SupportBean.class.getName() + " " + dialect + ":callIt() [ new " + SupportBean.class.getName() + "('E1', 10); ]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression + " select callIt() as val0, callIt().getTheString() as val1 from SupportBean as sb");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.theString,val0.intPrimitive,val1".split(","), new Object[]{"E1", 10, "E1"});

        stmt.destroy();
    }

    private void tryDatetime(EPServiceProvider epService, String dialect) {

        long msecDate = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
        String expression = "expression long " + dialect + ":callIt() [ " + msecDate + "]";
        String epl = expression + " select callIt().getHourOfDay() as val0, callIt().getDayOfWeek() as val1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtTwo = epService.getEPAdministrator().create(model);
        stmtTwo.addListener(listener);
        assertEquals(epl, stmtTwo.getText());

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        stmtTwo.destroy();
    }

    private void tryNested(EPServiceProvider epService, String dialect) {

        String epl = "expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
                "expression int " + dialect + ":abc(p1) [p1*10]\n" +
                "select abc(abc(2), 5) as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{1000});

        stmt.destroy();
    }

    private void tryReturnTypes(EPServiceProvider epService, String dialect) {

        String epl = "expression string " + dialect + ":one() ['x']\n" +
                "select one() as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(String.class, stmt.getEventType().getPropertyType("c0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{"x"});

        stmt.destroy();
    }

    private void tryOverloaded(EPServiceProvider epService, String dialect) {

        String epl = "expression int " + dialect + ":abc() [10]\n" +
                "expression int " + dialect + ":abc(p1) [p1*10]\n" +
                "expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
                "select abc() as c0, abc(2) as c1, abc(2,3) as c2 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2".split(","), new Object[]{10, 20, 60});

        stmt.destroy();
    }

    private void tryUnnamedInSelectClause(EPServiceProvider epService, String dialect) {

        String expressionOne = "expression int " + dialect + ":callOne() [1] ";
        String expressionTwo = "expression int " + dialect + ":callTwo(a) [1] ";
        String expressionThree = "expression int " + dialect + ":callThree(a,b) [1] ";
        String epl = expressionOne + expressionTwo + expressionThree + " select callOne(),callTwo(1),callThree(1, 2) from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean outEvent = listener.assertOneGetNewAndReset();
        for (String col : Arrays.asList("callOne()", "callTwo(1)", "callThree(1,2)")) {
            assertEquals(Integer.class, stmt.getEventType().getPropertyType(col));
            assertEquals(1, outEvent.get(col));
        }

        stmt.destroy();
    }

    private void tryImports(EPServiceProvider epService, String expression) {

        String epl = expression + " select callOne() as val0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.p00".split(","), new Object[]{MyImportedClass.VALUE_P00});

        stmt.destroy();
    }

    private void tryEnumeration(EPServiceProvider epService, String expression) {

        String epl = expression + " select callIt().countOf(v => v<6) as val0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtTwo = epService.getEPAdministrator().create(model);
        stmtTwo.addListener(listener);
        assertEquals(epl, stmtTwo.getText());

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        stmtTwo.destroy();
    }

    private void trySelect(EPServiceProvider epService, String scriptPart, String selectExpr, Class expectedType, Object[][] testdata) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(scriptPart +
                " select " + selectExpr + " as val from SupportBean as sb");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(expectedType, stmt.getEventType().getPropertyType("val"));

        for (int row = 0; row < testdata.length; row++) {
            Object theEvent = testdata[row][0];
            Object expected = testdata[row][1];

            epService.getEPRuntime().sendEvent(theEvent);
            EventBean outEvent = listener.assertOneGetNewAndReset();
            assertEquals(expected, outEvent.get("val"));
        }

        stmt.destroy();
    }

    private void tryParseJS(EPServiceProvider epService, String js, Class type, Object value) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "expression js:getResultOne [" +
                        js +
                        "] " +
                        "select getResultOne() from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(type, stmt.getEventType().getPropertyType("getResultOne()"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(value, theEvent.get("getResultOne()"));
        stmt.destroy();
    }

    private void tryAggregation(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create expression change(open, close) [ (open - close) / close ]");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select change(first(intPrimitive), last(intPrimitive)) as ch from SupportBean#time(1 day)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryParseMVEL(EPServiceProvider epService, String mvelExpression, Class type, Object value) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "expression mvel:getResultOne [" +
                        mvelExpression +
                        "] " +
                        "select getResultOne() from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "getResultOne()".split(","), new Object[]{value});
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL(
                "expression mvel:getResultOne [" +
                        mvelExpression +
                        "] " +
                        "expression mvel:getResultTwo [" +
                        mvelExpression +
                        "] " +
                        "select getResultOne() as val0, getResultTwo() as val1 from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(type, stmt.getEventType().getPropertyType("val0"));
        assertEquals(type, stmt.getEventType().getPropertyType("val1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{value, value});

        stmt.destroy();
    }

    private void runAssertionScriptReturningEvents(EPServiceProvider epService, boolean soda) {
        String script = "create expression EventBean[] @type(ItemEvent) js:myScriptReturnsEvents() [\n" +
                "myScriptReturnsEvents();" +
                "function myScriptReturnsEvents() {" +
                "  var EventBeanArray = Java.type(\"com.espertech.esper.client.EventBean[]\");\n" +
                "  var events = new EventBeanArray(3);\n" +
                "  events[0] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id1\"), \"ItemEvent\");\n" +
                "  events[1] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id2\"), \"ItemEvent\");\n" +
                "  events[2] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id3\"), \"ItemEvent\");\n" +
                "  return events;\n" +
                "}]";
        EPStatement stmtScript = SupportModelHelper.createByCompileOrParse(epService, soda, script);

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select myScriptReturnsEvents().where(v => v.id in ('id1', 'id3')) as c0 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        Collection<Map> coll = (Collection<Map>) listener.assertOneGetNewAndReset().get("c0");
        EPAssertionUtil.assertPropsPerRow(coll.toArray(new Map[coll.size()]), "id".split(","), new Object[][]{{"id1"}, {"id3"}});

        stmtSelect.destroy();
        stmtScript.destroy();
    }

    public static class ColorEvent {
        private String[] colors = {"Red", "Blue"};

        public String[] getColors() {
            return colors;
        }
    }

    public static class RFIDEvent {
        private String zone;
        private String loc;

        public String getZone() {
            return zone;
        }

        public String getLoc() {
            return loc;
        }
    }
}
