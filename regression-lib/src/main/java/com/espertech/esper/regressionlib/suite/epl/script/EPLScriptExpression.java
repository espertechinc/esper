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
package com.espertech.esper.regressionlib.suite.epl.script;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportColorEvent;
import com.espertech.esper.regressionlib.support.script.MyImportedClass;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.*;

public class EPLScriptExpression {

    private static final boolean TEST_MVEL = false;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLScriptScripts());
        execs.add(new EPLScriptQuoteEscape());
        execs.add(new EPLScriptScriptReturningEvents());
        execs.add(new EPLScriptDocSamples());
        execs.add(new EPLScriptInvalidRegardlessDialect());
        execs.add(new EPLScriptInvalidScriptJS());
        execs.add(new EPLScriptInvalidScriptMVEL());
        execs.add(new EPLScriptParserMVELSelectNoArgConstant());
        execs.add(new EPLScriptJavaScriptStatelessReturnPassArgs());
        execs.add(new EPLScriptMVELStatelessReturnPassArgs());
        execs.add(new EPLScriptSubqueryParam());
        execs.add(new EPLScriptReturnNullWhenNumeric());
        return execs;
    }

    private static class EPLScriptReturnNullWhenNumeric implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema Event(host string); " +
                "create window DnsTrafficProfile#time(5 minutes) (host string); " +
                "expression double js:doSomething(p) [ " +
                "doSomething(p); " +
                "function doSomething(p) { " +
                "  java.lang.System.out.println(p);" +
                "  java.lang.System.out.println(p.length);" +
                " } " +
                "] " +
                "@name('out') select doSomething((select window(z.*) from DnsTrafficProfile as z)) as score from DnsTrafficProfile;" +
                "insert into DnsTrafficProfile select * from Event; ";
            env.compileDeployWBusPublicType(epl, new RegressionPath());
            env.addListener("out");

            Map event = new HashMap();
            event.put("host", "test.domain.com");
            env.sendEventMap(event, "Event");

            env.undeployAll();
        }
    }

    private static class EPLScriptSubqueryParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression double js:myJSFunc(stringvalue) [\n" +
                "  calcScore(stringvalue);\n" +
                "  function calcScore(stringvalue) {\n" +
                "    return parseFloat(stringvalue);\n" +
                "  }\n" +
                "]\n" +
                "select myJSFunc((select theString from SupportBean#lastevent)) as c0 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(new SupportBean("20", 0));
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(20d, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean("30", 0));
            env.sendEventBean(new SupportBean_S0(1));
            assertEquals(30d, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class EPLScriptQuoteEscape implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplSLComment = "create expression f(params)[\n" +
                "  // I'am...\n" +
                "];";
            env.compileDeploy(eplSLComment);

            String eplMLComment = "create expression g(params)[\n" +
                "  /* I'am... */" +
                "];";
            env.compileDeploy(eplMLComment);

            env.undeployAll();
        }
    }

    private static class EPLScriptScriptReturningEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionScriptReturningEvents(env, false);
            runAssertionScriptReturningEvents(env, true);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema ItemEvent(id string)", path);
            tryInvalidCompile(env, path, "expression double @type(ItemEvent) fib(num) [] select fib(1) from SupportBean",
                "Failed to validate select-clause expression 'fib(1)': The @type annotation is only allowed when the invocation target returns EventBean instances");
            env.undeployAll();
        }
    }

    private static class EPLScriptDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@name('s0') expression double fib(num) [" +
                "fib(num); " +
                "function fib(n) { " +
                "  if(n <= 1) " +
                "    return n; " +
                "  return fib(n-1) + fib(n-2); " +
                "};" +
                "]" +
                "select fib(intPrimitive) from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));
            env.undeployAll();

            if (TEST_MVEL) {
                epl = "@name('s0') expression mvel:printColors(colors) [" +
                    "String c = null;" +
                    "for (c : colors) {" +
                    "   System.out.println(c);" +
                    "}" +
                    "]" +
                    "select printColors(colors) from SupportColorEvent";
                env.compileDeploy(epl).addListener("s0");
                env.sendEventBean(new SupportColorEvent());
                env.undeployAll();
            }

            if (SupportJavaVersionUtil.JAVA_VERSION <= 1.7) {
                epl = "@name('s0') expression js:printColors(colorEvent) [" +
                    "importClass (java.lang.System);" +
                    "importClass (java.util.Arrays);" +
                    "System.out.println(Arrays.toString(colorEvent.getColors()));" +
                    "]" +
                    "select printColors(colorEvent) from SupportColorEvent as colorEvent";
            } else {
                epl = "@name('s0') expression js:printColors(colorEvent) [" +
                    "print(java.util.Arrays.toString(colorEvent.getColors()));" +
                    "]" +
                    "select printColors(colorEvent) from SupportColorEvent as colorEvent";
            }
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportColorEvent());
            env.undeployAll();

            epl = "@name('s0') expression boolean js:setFlag(name, value, returnValue) [\n" +
                "  if (returnValue) epl.setScriptAttribute(name, value);\n" +
                "  returnValue;\n" +
                "]\n" +
                "expression js:getFlag(name) [\n" +
                "  epl.getScriptAttribute(name);\n" +
                "]\n" +
                "select getFlag('loc') as flag from SupportRFIDSimpleEvent(zone = 'Z1' and \n" +
                "  (setFlag('loc', true, loc = 'A') or setFlag('loc', false, loc = 'B')) )";
            env.compileDeploy(epl);
            env.undeployAll();
        }
    }

    private static class EPLScriptInvalidRegardlessDialect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // parameter defined twice
            tryInvalidCompile(env, "expression js:abc(p1, p1) [/* text */] select * from SupportBean",
                "Invalid script parameters for script 'abc', parameter 'p1' is defined more then once [expression js:abc(p1, p1) [/* text */] select * from SupportBean]");

            // invalid dialect
            tryInvalidCompile(env, "expression dummy:abc() [10] select * from SupportBean",
                "Failed to obtain script runtime for dialect 'dummy' for script 'abc' [expression dummy:abc() [10] select * from SupportBean]");

            // not found
            tryInvalidCompile(env, "select abc() from SupportBean",
                "Failed to validate select-clause expression 'abc': Unknown single-row function, expression declaration, script or aggregation function named 'abc' could not be resolved [select abc() from SupportBean]");

            // test incorrect number of parameters
            tryInvalidCompile(env, "expression js:abc() [10] select abc(1) from SupportBean",
                "Failed to validate select-clause expression 'abc(1)': Invalid number of parameters for script 'abc', expected 0 parameters but received 1 parameters [expression js:abc() [10] select abc(1) from SupportBean]");

            // test expression name overlap
            tryInvalidCompile(env, "expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean]");

            // test expression name overlap with parameters
            tryInvalidCompile(env, "expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean]");

            // test script name overlap with expression declaration
            tryInvalidCompile(env, "expression js:abc() [10] expression abc {10} select abc() from SupportBean",
                "Script name 'abc' overlaps with another expression of the same name [expression js:abc() [10] expression abc {10} select abc() from SupportBean]");

            // fails to resolve return type
            tryInvalidCompile(env, "expression dummy js:abc() [10] select abc() from SupportBean",
                "Failed to validate select-clause expression 'abc()': Failed to resolve return type 'dummy' specified for script 'abc' [expression dummy js:abc() [10] select abc() from SupportBean]");
        }
    }

    private static class EPLScriptInvalidScriptJS implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            if (SupportJavaVersionUtil.JAVA_VERSION <= 1.7) {
                tryInvalidContains(env, "expression js:abc[dummy abc = 1;] select * from SupportBean",
                    "missing ; before statement");

                tryInvalidContains(env, "expression js:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                    "invalid return");
            } else {
                tryInvalidContains(env, "expression js:abc[dummy abc = 1;] select * from SupportBean",
                    "Expected ; but found");

                tryInvalidContains(env, "expression js:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                    "Invalid return statement");
            }

            tryInvalidCompile(env, "expression js:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 18 near reserved keyword 'select' [expression js:abc[] select * from SupportBean]");

            // empty script
            env.compileDeploy("expression js:abc[\n] select * from SupportBean");

            // execution problem
            env.undeployAll();
            env.compileDeploy("expression js:abc() [throw new Error(\"Some error\");] select * from SupportBean#keepall where abc() = 1");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (Exception ex) {
                assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
            }

            // execution problem
            env.undeployAll();
            env.compileDeploy("expression js:abc[dummy;] select * from SupportBean#keepall where abc() = 1");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (Exception ex) {
                assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
            }

            // execution problem
            env.undeployAll();
            env.compileDeploy("@Name('ABC') expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ] select callIt().countOf(v => v < 6) from SupportBean").addListener("ABC");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (Exception ex) {
                assertTrue("Message is: " + ex.getMessage(), ex.getMessage().contains("Unexpected exception in statement 'ABC': "));
            }
            env.undeployAll();
        }
    }

    private static class EPLScriptInvalidScriptMVEL implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            if (!TEST_MVEL) {
                return;
            }

            // mvel return type check
            tryInvalidCompile(env, "expression java.lang.String mvel:abc[10] select * from SupportBean where abc()",
                "Failed to validate filter expression 'abc()': Return type and declared type not compatible for script 'abc', known return type is java.lang.Integer versus declared return type java.lang.String [expression java.lang.String mvel:abc[10] select * from SupportBean where abc()]");

            // undeclared variable
            tryInvalidCompile(env, "expression mvel:abc[dummy;] select * from SupportBean",
                "For script 'abc' the variable 'dummy' has not been declared and is not a parameter [expression mvel:abc[dummy;] select * from SupportBean]");

            // invalid assignment
            tryInvalidContains(env, "expression mvel:abc[dummy abc = 1;] select * from SupportBean",
                "Exception compiling MVEL script 'abc'");

            // syntax problem
            tryInvalidContains(env, "expression mvel:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                "unable to resolve method using strict-mode");

            // empty brackets
            tryInvalidCompile(env, "expression mvel:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 20 near reserved keyword 'select' [expression mvel:abc[] select * from SupportBean]");

            // empty script
            env.compileDeploy("expression mvel:abc[/* */] select * from SupportBean");

            // unused expression
            env.compileDeploy("expression mvel:abc(aa) [return aa..bb(1);] select * from SupportBean");

            // execution problem
            env.undeployAll();

            env.compileDeploy("expression mvel:abc() [Integer a = null; a + 1;] select * from SupportBean#keepall where abc() = 1");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (Exception ex) {
                assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
            }
            env.undeployAll();
        }
    }

    private static class EPLScriptScripts implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test different return types
            tryReturnTypes(env, "js");
            if (TEST_MVEL) {
                tryReturnTypes(env, "mvel");
            }

            // test void return type
            tryVoidReturnType(env, "js");
            if (TEST_MVEL) {
                tryVoidReturnType(env, "js");
            }

            // test enumeration method
            // Not supported: tryEnumeration("expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ]"); returns NativeArray which is a Rhino-specific array wrapper
            if (TEST_MVEL) {
                tryEnumeration(env, "expression Integer[] mvel:callIt() [ Integer[] array = {2, 8, 5, 9}; return array; ]");
            }

            // test script props
            trySetScriptProp(env, "js");
            if (TEST_MVEL) {
                trySetScriptProp(env, "mvel");
            }

            // test variable
            tryPassVariable(env, "js");
            if (TEST_MVEL) {
                tryPassVariable(env, "mvel");
            }
            // test passing an event
            tryPassEvent(env, "js");
            if (TEST_MVEL) {
                tryPassEvent(env, "mvel");
            }

            // test returning an object
            tryReturnObject(env, "js");
            if (TEST_MVEL) {
                tryReturnObject(env, "mvel");
            }

            // test datetime method
            tryDatetime(env, "js");
            if (TEST_MVEL) {
                tryDatetime(env, "mvel");
            }

            // test unnamed expression
            tryUnnamedInSelectClause(env, "js");
            if (TEST_MVEL) {
                tryUnnamedInSelectClause(env, "mvel");
            }

            // test import
            if (SupportJavaVersionUtil.JAVA_VERSION <= 1.7) {
                tryImports(env, "expression MyImportedClass js:callOne() [ importClass(" + MyImportedClass.class.getName() + "); new MyImportedClass() ] ");
            } else {
                tryImports(env, "expression MyImportedClass js:callOne() [ " +
                    "var MyJavaClass = Java.type('" + MyImportedClass.class.getName() + "');" +
                    "new MyJavaClass() ] ");
            }
            if (TEST_MVEL) {
                tryImports(env, "expression MyImportedClass mvel:callOne() [ import " + MyImportedClass.class.getName() + "; new MyImportedClass() ] ");
            }

            // test overloading script
            tryOverloaded(env, "js");
            if (TEST_MVEL) {
                tryOverloaded(env, "mvel");
            }

            // test nested invocation
            tryNested(env, "js");
            if (TEST_MVEL) {
                tryNested(env, "mvel");
            }

            tryAggregation(env);

            tryDeployArrayInScript(env);

            tryCreateExpressionWArrayAllocate(env);
        }
    }

    private static class EPLScriptParserMVELSelectNoArgConstant implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (TEST_MVEL) {
                tryParseMVEL(env, "\n\t  10    \n\n\t\t", Integer.class, 10);
                tryParseMVEL(env, "10", Integer.class, 10);
                tryParseMVEL(env, "5*5", Integer.class, 25);
                tryParseMVEL(env, "\"abc\"", String.class, "abc");
                tryParseMVEL(env, " \"abc\"     ", String.class, "abc");
                tryParseMVEL(env, "'def'", String.class, "def");
                tryParseMVEL(env, " 'def' ", String.class, "def");
                tryParseMVEL(env, " new String[] {'a'}", String[].class, new String[]{"a"});
            }

            tryParseJS(env, "\n\t  10.0    \n\n\t\t", Object.class, 10.0);
            tryParseJS(env, "10.0", Object.class, 10.0);
            tryParseJS(env, "5*5.0", Object.class, 25.0);
            tryParseJS(env, "\"abc\"", Object.class, "abc");
            tryParseJS(env, " \"abc\"     ", Object.class, "abc");
            tryParseJS(env, "'def'", Object.class, "def");
            tryParseJS(env, " 'def' ", Object.class, "def");
        }
    }

    private static class EPLScriptJavaScriptStatelessReturnPassArgs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Object[][] testData;
            String expression;
            RegressionPath path = new RegressionPath();

            expression = "fib(num);" +
                "function fib(n) {" +
                "  if(n <= 1) return n; " +
                "  return fib(n-1) + fib(n-2); " +
                "};";
            testData = new Object[][]{
                {new SupportBean("E1", 20), 6765.0},
            };
            trySelect(env, path, "expression double js:abc(num) [ " + expression + " ]", "abc(intPrimitive)", Double.class, testData);
            path.clear();

            testData = new Object[][]{
                {new SupportBean("E1", 5), 50.0},
                {new SupportBean("E1", 6), 60.0}
            };
            trySelect(env, path, "expression js:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", Object.class, testData);
            path.clear();
        }
    }

    private static class EPLScriptMVELStatelessReturnPassArgs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (!TEST_MVEL) {
                return;
            }

            Object[][] testData;
            String expression;
            RegressionPath path = new RegressionPath();

            testData = new Object[][]{
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60}
            };
            trySelect(env, path, "expression mvel:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", Integer.class, testData);
            path.clear();

            expression = "if (theString.equals('E1')) " +
                "  return myint * 10;" +
                "else " +
                "  return myint * 5;";
            testData = new Object[][]{
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60},
                {new SupportBean("E2", 7), 35}
            };
            trySelect(env, path, "expression mvel:abc(myint, theString) [" + expression + "]", "abc(intPrimitive, theString)", Object.class, testData);
            path.clear();

            trySelect(env, path, "expression int mvel:abc(myint, theString) [" + expression + "]", "abc(intPrimitive, theString)", Integer.class, testData);
            path.clear();

            expression = "a + Integer.toString(b)";
            testData = new Object[][]{
                {new SupportBean("E1", 5), "E15"},
                {new SupportBean("E1", 6), "E16"},
                {new SupportBean("E2", 7), "E27"}
            };
            trySelect(env, path, "expression mvel:abc(a, b) [" + expression + "]", "abc(theString, intPrimitive)", String.class, testData);
        }
    }

    private static void runAssertionScriptReturningEvents(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('type') create schema ItemEvent(id string)", path);

        String script = "@name('script') create expression EventBean[] @type(ItemEvent) js:myScriptReturnsEvents() [\n" +
            "myScriptReturnsEvents();" +
            "function myScriptReturnsEvents() {" +
            "  var EventBeanArray = Java.type(\"com.espertech.esper.common.client.EventBean[]\");\n" +
            "  var events = new EventBeanArray(3);\n" +
            "  events[0] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id1\"), \"ItemEvent\");\n" +
            "  events[1] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id2\"), \"ItemEvent\");\n" +
            "  events[2] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id3\"), \"ItemEvent\");\n" +
            "  return events;\n" +
            "}]";
        env.compileDeploy(soda, script, path);
        assertEquals(StatementType.CREATE_EXPRESSION, env.statement("script").getProperty(StatementProperty.STATEMENTTYPE));
        assertEquals("myScriptReturnsEvents", env.statement("script").getProperty(StatementProperty.CREATEOBJECTNAME));

        env.compileDeploy("@name('s0') select myScriptReturnsEvents().where(v => v.id in ('id1', 'id3')) as c0 from SupportBean", path);
        env.addListener("s0");

        env.sendEventBean(new SupportBean());
        Collection<Map> coll = (Collection<Map>) env.listener("s0").assertOneGetNewAndReset().get("c0");
        EPAssertionUtil.assertPropsPerRow(coll.toArray(new Map[coll.size()]), "id".split(","), new Object[][]{{"id1"}, {"id3"}});

        env.undeployModuleContaining("s0");
        env.undeployModuleContaining("script");
        env.undeployModuleContaining("type");
    }

    private static void tryVoidReturnType(RegressionEnvironment env, String dialect) {
        Object[][] testData;
        String expression;
        RegressionPath path = new RegressionPath();

        expression = "expression void " + dialect + ":mysetter() [ epl.setScriptAttribute('a', 1); ]";
        testData = new Object[][]{
            {new SupportBean("E1", 20), null},
            {new SupportBean("E1", 10), null},
        };
        trySelect(env, path, expression, "mysetter()", Object.class, testData);

        env.undeployAll();
    }

    private static void trySetScriptProp(RegressionEnvironment env, String dialect) {
        env.compileDeploy("@name('s0') expression " + dialect + ":getFlag() [" +
            "  epl.getScriptAttribute('flag');" +
            "]" +
            "expression boolean " + dialect + ":setFlag(flagValue) [" +
            "  epl.setScriptAttribute('flag', flagValue);" +
            "  flagValue;" +
            "]" +
            "select getFlag() as val from SupportBean(theString = 'E1' or setFlag(intPrimitive > 0))");
        env.addListener("s0");

        env.sendEventBean(new SupportBean("E2", 10));
        assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("val"));

        env.undeployAll();
    }

    private static void tryPassVariable(RegressionEnvironment env, String dialect) {

        Object[][] testData;
        String expression;

        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('var') create variable long THRESHOLD = 100", path);

        expression = "expression long " + dialect + ":thresholdAdder(numToAdd, th) [ th + numToAdd; ]";
        testData = new Object[][]{
            {new SupportBean("E1", 20), 120L},
            {new SupportBean("E1", 10), 110L},
        };
        trySelect(env, path, expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        env.runtime().getVariableService().setVariableValue(env.deploymentId("var"), "THRESHOLD", 1);
        testData = new Object[][]{
            {new SupportBean("E1", 20), 21L},
            {new SupportBean("E1", 10), 11L},
        };
        trySelect(env, path, expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        env.undeployAll();
    }

    private static void tryPassEvent(RegressionEnvironment env, String dialect) {

        Object[][] testData;
        String expression;
        RegressionPath path = new RegressionPath();

        expression = "expression int " + dialect + ":callIt(bean) [ bean.getIntPrimitive() + 1; ]";
        testData = new Object[][]{
            {new SupportBean("E1", 20), 21},
            {new SupportBean("E1", 10), 11},
        };
        trySelect(env, path, expression, "callIt(sb)", Integer.class, testData);

        env.undeployAll();
    }

    private static void tryReturnObject(RegressionEnvironment env, String dialect) {

        String expression = "@name('s0') expression " + SupportBean.class.getName() + " " + dialect + ":callIt() [ new " + SupportBean.class.getName() + "('E1', 10); ]";
        env.compileDeploy(expression + " select callIt() as val0, callIt().getTheString() as val1 from SupportBean as sb").addListener("s0");
        assertEquals(SupportBean.class, env.statement("s0").getEventType().getPropertyType("val0"));

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0.theString,val0.intPrimitive,val1".split(","), new Object[]{"E1", 10, "E1"});

        env.undeployAll();
    }

    private static void tryDatetime(RegressionEnvironment env, String dialect) {

        long msecDate = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
        String expression = "expression long " + dialect + ":callIt() [ " + msecDate + "]";
        String epl = "@name('s0') " + expression + " select callIt().getHourOfDay() as val0, callIt().getDayOfWeek() as val1 from SupportBean";
        env.compileDeploy(epl).addListener("s0");
        assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("val0"));

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        env.undeployAll();

        env.eplToModelCompileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        env.undeployAll();
    }

    private static void tryNested(RegressionEnvironment env, String dialect) {

        String epl = "@name('s0') expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
            "expression int " + dialect + ":abc(p1) [p1*10]\n" +
            "select abc(abc(2), 5) as c0 from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{1000});

        env.undeployAll();
    }

    private static void tryReturnTypes(RegressionEnvironment env, String dialect) {

        String epl = "@name('s0') expression string " + dialect + ":one() ['x']\n" +
            "select one() as c0 from SupportBean";
        env.compileDeploy(epl).addListener("s0");
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("c0"));

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{"x"});

        env.undeployAll();
    }

    private static void tryOverloaded(RegressionEnvironment env, String dialect) {

        String epl = "@name('s0') expression int " + dialect + ":abc() [10]\n" +
            "expression int " + dialect + ":abc(p1) [p1*10]\n" +
            "expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
            "select abc() as c0, abc(2) as c1, abc(2,3) as c2 from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2".split(","), new Object[]{10, 20, 60});

        env.undeployAll();
    }

    private static void tryUnnamedInSelectClause(RegressionEnvironment env, String dialect) {

        String expressionOne = "expression int " + dialect + ":callOne() [1] ";
        String expressionTwo = "expression int " + dialect + ":callTwo(a) [1] ";
        String expressionThree = "expression int " + dialect + ":callThree(a,b) [1] ";
        String epl = "@name('s0') " + expressionOne + expressionTwo + expressionThree + " select callOne(),callTwo(1),callThree(1, 2) from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean());
        EventBean outEvent = env.listener("s0").assertOneGetNewAndReset();
        for (String col : Arrays.asList("callOne()", "callTwo(1)", "callThree(1,2)")) {
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(col));
            assertEquals(1, outEvent.get(col));
        }

        env.undeployAll();
    }

    private static void tryImports(RegressionEnvironment env, String expression) {

        String epl = "@name('s0') " + expression + " select callOne() as val0 from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0.p00".split(","), new Object[]{MyImportedClass.VALUE_P00});

        env.undeployAll();
    }

    private static void tryEnumeration(RegressionEnvironment env, String expression) {

        String epl = "@name('s0') " + expression + " select callIt().countOf(v => v<6) as val0 from SupportBean";
        env.compileDeploy(epl).addListener("s0");
        assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("val0"));

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        env.undeployAll();

        env.eplToModelCompileDeploy(epl).addListener("s0");
        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        env.undeployAll();
    }

    private static void trySelect(RegressionEnvironment env, RegressionPath path, String scriptPart, String selectExpr, Class expectedType, Object[][] testdata) {
        env.compileDeploy("@name('s0') " + scriptPart +
            " select " + selectExpr + " as val from SupportBean as sb", path).addListener("s0");
        assertEquals(expectedType, env.statement("s0").getEventType().getPropertyType("val"));

        for (int row = 0; row < testdata.length; row++) {
            Object theEvent = testdata[row][0];
            Object expected = testdata[row][1];

            env.sendEventBean(theEvent);
            EventBean outEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(expected, outEvent.get("val"));
        }

        env.undeployModuleContaining("s0");
    }

    private static void tryParseJS(RegressionEnvironment env, String js, Class type, Object value) {
        env.compileDeploy("@name('s0') expression js:getResultOne [" +
            js +
            "] " +
            "select getResultOne() from SupportBean").addListener("s0");

        env.sendEventBean(new SupportBean());
        assertEquals(type, env.statement("s0").getEventType().getPropertyType("getResultOne()"));
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(value, theEvent.get("getResultOne()"));
        env.undeployAll();
    }

    private static void tryAggregation(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create expression change(open, close) [ (open - close) / close ]", path);
        env.compileDeploy("@name('s0') select change(first(intPrimitive), last(intPrimitive)) as ch from SupportBean#time(1 day)", path).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "ch".split(","), new Object[]{0d});

        env.sendEventBean(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "ch".split(","), new Object[]{-0.9d});

        env.undeployAll();
    }

    private static void tryParseMVEL(RegressionEnvironment env, String mvelExpression, Class type, Object value) {
        env.compileDeploy("@name('s0') expression mvel:getResultOne [" +
            mvelExpression +
            "] " +
            "select getResultOne() from SupportBean").addListener("s0");

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "getResultOne()".split(","), new Object[]{value});
        env.undeployAll();

        env.compileDeploy("@name('s0') expression mvel:getResultOne [" +
            mvelExpression +
            "] " +
            "expression mvel:getResultTwo [" +
            mvelExpression +
            "] " +
            "select getResultOne() as val0, getResultTwo() as val1 from SupportBean").addListener("s0");

        env.sendEventBean(new SupportBean());
        assertEquals(type, env.statement("s0").getEventType().getPropertyType("val0"));
        assertEquals(type, env.statement("s0").getEventType().getPropertyType("val1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{value, value});

        env.undeployAll();
    }

    private static void tryCreateExpressionWArrayAllocate(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        String epl = "@name('first') create expression double js:test(bar) [\n" +
            "test(bar);\n" +
            "function test(bar) {\n" +
            "  var test=[];\n" +
            "  return -1.0;\n" +
            "}]\n";
        env.compileDeploy(epl, path);

        env.compileDeploy("@name('s0') select test('a') as c0 from SupportBean_S0", path).addListener("s0");
        env.listener("s0").reset();
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{-1d});

        env.undeployAll();
    }

    private static void tryDeployArrayInScript(RegressionEnvironment env) {
        String epl = "expression string js:myFunc(arg) [\n" +
            "  function replace(text, values, replacement){\n" +
            "    return text.replace(replacement, values[0]);\n" +
            "  }\n" +
            "  replace(\"A B C\", [\"X\"], \"B\")\n" +
            "]\n" +
            "select\n" +
            "myFunc(*)\n" +
            "from SupportBean;";
        env.compileDeploy(epl).undeployAll();
    }

    private static void tryInvalidContains(RegressionEnvironment env, String expression, String part) {
        try {
            env.compileWCheckedEx(expression);
            fail();
        } catch (EPCompileException ex) {
            assertTrue("Message not containing text '" + part + "' : " + ex.getMessage(), ex.getMessage().contains(part));
        }
    }
}
