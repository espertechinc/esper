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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.annotation.*;
import com.espertech.esper.common.client.soda.EPStatementFormatter;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.compiler.client.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.client.*;
import com.espertech.esper.runtime.client.EPStatement;
import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.toObjectArray;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertTrue;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.client.AnnotationAssertUtil.sortAlpha;
import static junit.framework.TestCase.*;

public class ClientRuntimeStatementAnnotation {

    private final static String NEWLINE = System.getProperty("line.separator");

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeStatementAnnotationBuiltin());
        execs.add(new ClientRuntimeStatementAnnotationAppSimple());
        execs.add(new ClientRuntimeStatementAnnotationAppNested());
        execs.add(new ClientRuntimeStatementAnnotationInvalid());
        execs.add(new ClientRuntimeStatementAnnotationSpecificImport());
        execs.add(new ClientRuntimeStatementAnnotationRecursive());
        return execs;
    }

    public static class ClientRuntimeStatementAnnotationRecursive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@MyAnnotationAPIEventType create schema ABC();\n" +
                        "@name('s0') select * from ABC;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(Collections.emptyMap(), "ABC");
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    public static class ClientRuntimeStatementAnnotationAppSimple implements RegressionExecution {
        @MyAnnotationSimple
        @MyAnnotationValue("abc")
        @MyAnnotationValuePair(stringVal = "a", intVal = -1, longVal = 2, booleanVal = true, charVal = 'x', byteVal = 10, shortVal = 20, doubleVal = 2.5)
        @MyAnnotationValueDefaulted
        @MyAnnotationValueArray(value = {1, 2, 3}, intArray = {4, 5}, doubleArray = {}, stringArray = {"X"})
        @MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_3)
        public void run(RegressionEnvironment env) {
            String stmtText =
                "@MyAnnotationSimple " +
                    "@MyAnnotationValue('abc') " +
                    "@MyAnnotationValueDefaulted " +
                    "@MyAnnotationValueEnum(supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_3) " +
                    "@MyAnnotationValuePair(stringVal='a',intVal=-1,longVal=2,booleanVal=true,charVal='x',byteVal=10,shortVal=20,doubleVal=2.5) " +
                    "@Name('STMTONE') " +
                    "select * from SupportBean";
            String stmtTextFormatted = "@MyAnnotationSimple" + NEWLINE +
                "@MyAnnotationValue('abc')" + NEWLINE +
                "@MyAnnotationValueDefaulted" + NEWLINE +
                "@MyAnnotationValueEnum(supportEnum=" + SupportEnum.class.getName() + ".ENUM_VALUE_3)" + NEWLINE +
                "@MyAnnotationValuePair(stringVal='a',intVal=-1,longVal=2,booleanVal=true,charVal='x',byteVal=10,shortVal=20,doubleVal=2.5)" + NEWLINE +
                "@Name('STMTONE')" + NEWLINE +
                "select *" + NEWLINE +
                "from SupportBean";
            env.compileDeploy(stmtText);

            Annotation[] annotations = env.statement("STMTONE").getAnnotations();
            annotations = sortAlpha(annotations);
            assertEquals(6, annotations.length);

            assertEquals(MyAnnotationSimple.class, annotations[0].annotationType());
            assertEquals("abc", ((MyAnnotationValue) annotations[1]).value());
            assertEquals("XYZ", ((MyAnnotationValueDefaulted) annotations[2]).value());
            assertEquals("STMTONE", ((Name) annotations[5]).value());

            MyAnnotationValueEnum enumval = (MyAnnotationValueEnum) annotations[3];
            assertEquals(SupportEnum.ENUM_VALUE_2, enumval.supportEnumDef());
            assertEquals(SupportEnum.ENUM_VALUE_3, enumval.supportEnum());

            MyAnnotationValuePair pair = (MyAnnotationValuePair) annotations[4];
            assertEquals("a", pair.stringVal());
            assertEquals(-1, pair.intVal());
            assertEquals(2L, pair.longVal());
            assertEquals(true, pair.booleanVal());
            assertEquals('x', pair.charVal());
            assertEquals(10, pair.byteVal());
            assertEquals(20, pair.shortVal());
            assertEquals(2.5, pair.doubleVal());
            assertEquals("def", pair.stringValDef());
            assertEquals(100, pair.intValDef());
            assertEquals(200L, pair.longValDef());
            assertEquals(true, pair.booleanValDef());
            assertEquals('D', pair.charValDef());
            assertEquals(1.1, pair.doubleValDef());

            env.undeployAll();

            // statement model
            EPStatementObjectModel model = env.eplToModel(stmtText);
            assertEquals(stmtText, model.toEPL());
            String textFormatted = model.toEPL(new EPStatementFormatter(true));
            assertEquals(stmtTextFormatted, textFormatted);
            env.compileDeploy(model).addListener("STMTONE");
            assertEquals(6, env.statement("STMTONE").getAnnotations().length);
            env.undeployAll();

            // test array
            stmtText = "@MyAnnotationValueArray(value={1,2,3},intArray={4,5},doubleArray={},stringArray={'X'}) @name('s0') select * from SupportBean";
            env.compileDeploy(stmtText);

            env.statement("s0").getAnnotations();
            assertStatement(env);
            env.undeployAll();

            // statement model
            env.eplToModelCompileDeploy(stmtText);
            assertStatement(env);
            env.undeployAll();
        }
    }

    public static class ClientRuntimeStatementAnnotationInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidAnnotation(env, "@MyAnnotationNested(nestableSimple=@MyAnnotationNestableSimple, nestableValues=@MyAnnotationNestableValues, nestableNestable=@MyAnnotationNestableNestable) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationNestableNestable' requires a value for attribute 'value' [@MyAnnotationNested(nestableSimple=@MyAnnotationNestableSimple, nestableValues=@MyAnnotationNestableValues, nestableNestable=@MyAnnotationNestableNestable) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationNested(nestableNestable=@MyAnnotationNestableNestable('A'), nestableSimple=1) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationNested' requires a MyAnnotationNestableSimple-typed value for attribute 'nestableSimple' but received a Integer-typed value [@MyAnnotationNested(nestableNestable=@MyAnnotationNestableNestable('A'), nestableSimple=1) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValuePair(stringVal='abc') select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValuePair' requires a value for attribute 'booleanVal' [@MyAnnotationValuePair(stringVal='abc') select * from Bean]");

            tryInvalidAnnotation(env, "MyAnnotationValueArray(value=5) select * from Bean", true,
                "Incorrect syntax near 'MyAnnotationValueArray' [MyAnnotationValueArray(value=5) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValueArray(value=null) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a value for attribute 'doubleArray' [@MyAnnotationValueArray(value=null) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={null},value={}) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a non-null value for array elements for attribute 'stringArray' [@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={null},value={}) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={1},value={}) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a String-typed value for array elements for attribute 'stringArray' but received a Integer-typed value [@MyAnnotationValueArray(intArray={},doubleArray={},stringArray={1},value={}) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValue(value='a', value='a') select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValue' has duplicate attribute values for attribute 'value' [@MyAnnotationValue(value='a', value='a') select * from Bean]");
            tryInvalidAnnotation(env, "@ABC select * from Bean", false,
                "Failed to process statement annotations: Failed to resolve @-annotation class: Could not load annotation class by name 'ABC', please check imports [@ABC select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationSimple(5) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationSimple' does not have an attribute 'value' [@MyAnnotationSimple(5) select * from Bean]");
            tryInvalidAnnotation(env, "@MyAnnotationSimple(null) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationSimple' does not have an attribute 'value' [@MyAnnotationSimple(null) select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValue select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValue' requires a value for attribute 'value' [@MyAnnotationValue select * from Bean]");

            tryInvalidAnnotation(env, "@MyAnnotationValue(5) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValue' requires a String-typed value for attribute 'value' but received a Integer-typed value [@MyAnnotationValue(5) select * from Bean]");
            tryInvalidAnnotation(env, "@MyAnnotationValueArray(value=\"ABC\", intArray={}, doubleArray={}, stringArray={}) select * from Bean", false,
                "Failed to process statement annotations: Annotation 'MyAnnotationValueArray' requires a long[]-typed value for attribute 'value' but received a String-typed value [@MyAnnotationValueArray(value=\"ABC\", intArray={}, doubleArray={}, stringArray={}) select * from Bean]");
            tryInvalidAnnotation(env, "@MyAnnotationValueEnum(a.b.CC) select * from Bean", false,
                "Annotation enumeration value 'a.b.CC' not recognized as an enumeration class, please check imports or type used [@MyAnnotationValueEnum(a.b.CC) select * from Bean]");

            tryInvalidAnnotation(env, "@Hint('XXX') select * from Bean", false,
                "Failed to process statement annotations: Hint annotation value 'XXX' is not one of the known values [@Hint('XXX') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('ITERATE_ONLY,XYZ') select * from Bean", false,
                "Failed to process statement annotations: Hint annotation value 'XYZ' is not one of the known values [@Hint('ITERATE_ONLY,XYZ') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('testit=5') select * from Bean", false,
                "Failed to process statement annotations: Hint annotation value 'testit' is not one of the known values [@Hint('testit=5') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('RECLAIM_GROUP_AGED') select * from Bean", false,
                "Failed to process statement annotations: Hint 'RECLAIM_GROUP_AGED' requires a parameter value [@Hint('RECLAIM_GROUP_AGED') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('ITERATE_ONLY,RECLAIM_GROUP_AGED') select * from Bean", false,
                "Failed to process statement annotations: Hint 'RECLAIM_GROUP_AGED' requires a parameter value [@Hint('ITERATE_ONLY,RECLAIM_GROUP_AGED') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('ITERATE_ONLY=5,RECLAIM_GROUP_AGED=5') select * from Bean", false,
                "Failed to process statement annotations: Hint 'ITERATE_ONLY' does not accept a parameter value [@Hint('ITERATE_ONLY=5,RECLAIM_GROUP_AGED=5') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('index(name)xxx') select * from Bean", false,
                "Failed to process statement annotations: Hint 'INDEX' has additional text after parentheses [@Hint('index(name)xxx') select * from Bean]");
            tryInvalidAnnotation(env, "@Hint('index') select * from Bean", false,
                "Failed to process statement annotations: Hint 'INDEX' requires additional parameters in parentheses [@Hint('index') select * from Bean]");
        }
    }

    public static class ClientRuntimeStatementAnnotationAppNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runNestedSimple(env);
            runNestedArray(env);
        }

        @MyAnnotationNested(
            nestableSimple = @MyAnnotationNestableSimple,
            nestableValues = @MyAnnotationNestableValues(val = 999, arr = {2, 1}),
            nestableNestable = @MyAnnotationNestableNestable("CDF"))
        private void runNestedSimple(RegressionEnvironment env) {
            String stmtText =
                "@MyAnnotationNested(\n" +
                    "            nestableSimple=@MyAnnotationNestableSimple,\n" +
                    "            nestableValues=@MyAnnotationNestableValues(val=999, arr={2, 1}),\n" +
                    "            nestableNestable=@MyAnnotationNestableNestable(\"CDF\")\n" +
                    "    ) " +
                    "@name('s0') select * from SupportBean";
            env.compileDeploy(stmtText);

            Annotation[] annotations = env.statement("s0").getAnnotations();
            annotations = sortAlpha(annotations);
            assertEquals(2, annotations.length);

            MyAnnotationNested nested = (MyAnnotationNested) annotations[0];
            assertNotNull(nested.nestableSimple());
            TestCase.assertTrue(Arrays.deepEquals(toObjectArray(nested.nestableValues().arr()), new Object[]{2, 1}));
            assertEquals(999, nested.nestableValues().val());
            assertEquals("CDF", nested.nestableNestable().value());

            env.undeployAll();
        }

        @MyAnnotationWArrayAndClass(priorities = {@Priority(1), @Priority(3)}, classOne = java.lang.String.class, classTwo = Integer.class)
        private void runNestedArray(RegressionEnvironment env) {
            String stmtText = "@MyAnnotationWArrayAndClass(priorities = {@Priority(1), @Priority(3)}, classOne = java.lang.String.class, classTwo = Integer.class) @name('s0') select * from SupportBean";
            env.compileDeploy(stmtText);

            Annotation[] annotations = env.statement("s0").getAnnotations();
            annotations = sortAlpha(annotations);
            assertEquals(2, annotations.length);

            MyAnnotationWArrayAndClass nested = (MyAnnotationWArrayAndClass) annotations[0];
            assertEquals(1, nested.priorities()[0].value());
            assertEquals(3, nested.priorities()[1].value());
            assertEquals(String.class, nested.classOne());
            assertEquals(Integer.class, nested.classTwo());

            env.undeployAll();
        }
    }

    public static class ClientRuntimeStatementAnnotationBuiltin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@Name('MyTestStmt') @Description('MyTestStmt description') @Tag(name=\"UserId\", value=\"value\") select * from SupportBean";
            env.compileDeploy(epl).addListener("MyTestStmt");
            tryAssertion(env.statement("MyTestStmt"));
            Name name = (Name) AnnotationUtil.findAnnotation(env.statement("MyTestStmt").getAnnotations(), Name.class);
            assertEquals("MyTestStmt", name.value());
            env.undeployAll();

            // try lowercase
            epl = "@name('MyTestStmt') @description('MyTestStmt description') @tag(name=\"UserId\", value=\"value\") select * from SupportBean";
            env.compileDeploy(epl).addListener("MyTestStmt");
            tryAssertion(env.statement("MyTestStmt"));
            env.undeployAll();

            // try fully-qualified
            epl = "@" + Name.class.getName() + "('MyTestStmt') @Description('MyTestStmt description') @Tag(name=\"UserId\", value=\"value\") select * from SupportBean";
            env.compileDeploy(epl).addListener("MyTestStmt");
            tryAssertion(env.statement("MyTestStmt"));
            env.undeployAll();

            // hint tests
            assertNull(HintEnum.DISABLE_RECLAIM_GROUP.getHint(null));
            assertNull(HintEnum.DISABLE_RECLAIM_GROUP.getHint(new Annotation[0]));
            env.compileDeploy("@Hint('ITERATE_ONLY') select * from SupportBean");
            env.compileDeploy("@Hint('ITERATE_ONLY,DISABLE_RECLAIM_GROUP') select * from SupportBean");
            env.compileDeploy("@Hint('ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY') select * from SupportBean");
            env.compileDeploy("@Hint('  iterate_only ') select * from SupportBean");

            Annotation[] annos = env.compileDeploy("@Hint('DISABLE_RECLAIM_GROUP') @name('s0') select * from SupportBean").statement("s0").getAnnotations();
            assertEquals("DISABLE_RECLAIM_GROUP", HintEnum.DISABLE_RECLAIM_GROUP.getHint(annos).value());

            annos = env.compileDeploy("@Hint('ITERATE_ONLY,ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY') @name('s1') select * from SupportBean").statement("s1").getAnnotations();
            assertEquals("ITERATE_ONLY,ITERATE_ONLY,DISABLE_RECLAIM_GROUP,ITERATE_ONLY", HintEnum.DISABLE_RECLAIM_GROUP.getHint(annos).value());

            annos = env.compileDeploy("@Hint('ITERATE_ONLY,reclaim_group_aged=10') @name('s2') select * from SupportBean").statement("s2").getAnnotations();
            Hint hint = HintEnum.RECLAIM_GROUP_AGED.getHint(annos);
            assertEquals("10", HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(hint));

            annos = env.compileDeploy("@Hint('reclaim_group_aged=11') @name('s3') select * from SupportBean").statement("s3").getAnnotations();
            hint = HintEnum.RECLAIM_GROUP_AGED.getHint(annos);
            assertEquals("11", HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(hint));

            annos = env.compileDeploy("@Hint('index(one, two)') @name('s4') select * from SupportBean").statement("s4").getAnnotations();
            assertEquals("one, two", HintEnum.INDEX.getHintAssignedValues(annos).get(0));

            env.undeployAll();

            // NoLock
            env.compileDeploy("@name('s0') @NoLock select * from SupportBean");
            assertEquals(1, AnnotationUtil.findAnnotations(env.statement("s0").getAnnotations(), NoLock.class).size());

            env.undeployAll();
        }
    }

    public static class ClientRuntimeStatementAnnotationSpecificImport implements RegressionExecution {
        @MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_1)
        public void run(RegressionEnvironment env) {
            tryAssertionNoClassNameRequired(env, SupportEnum.ENUM_VALUE_2, "ENUM_VALUE_2");
            tryAssertionNoClassNameRequired(env, SupportEnum.ENUM_VALUE_3, "ENUM_value_3");
            tryAssertionNoClassNameRequired(env, SupportEnum.ENUM_VALUE_1, "enum_value_1");
        }

        private void tryAssertionNoClassNameRequired(RegressionEnvironment env, SupportEnum expected, String text) {
            env.compileDeploy("@MyAnnotationValueEnum(supportEnum = " + text + ") @name('s0') select * from SupportBean");
            MyAnnotationValueEnum anno = (MyAnnotationValueEnum) env.statement("s0").getAnnotations()[0];
            assertEquals(expected, anno.supportEnum());
            env.undeployAll();
        }
    }

    public static class ClientRuntimeAnnotationImportInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // init-time import
            env.compileDeploy("@MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_1) " +
                "select * from SupportBean");

            // try invalid annotation not yet imported
            String epl = "@MyAnnotationValueEnumTwo(supportEnum = SupportEnum.ENUM_VALUE_1) select * from SupportBean";
            tryInvalidCompile(env, epl, "Failed to process statement annotations: Failed to resolve @-annotation");

            // try invalid use : these are annotation-specific imports of an annotation and an enum
            tryInvalidCompile(env, "select * from MyAnnotationValueEnumTwo",
                "Failed to resolve event type, named window or table by name 'MyAnnotationValueEnumTwo'");
            tryInvalidCompile(env, "select SupportEnum.ENUM_VALUE_1 from SupportBean",
                "Failed to validate select-clause expression 'SupportEnum.ENUM_VALUE_1'");

            env.undeployAll();
        }
    }

    private static void tryInvalidAnnotation(RegressionEnvironment env, String stmtText, boolean isSyntax, String message) {
        try {
            EPCompilerProvider.getCompiler().compile(stmtText, new CompilerArguments(env.getConfiguration()));
            fail();
        } catch (EPCompileException ex) {
            EPCompileExceptionItem first = ex.getItems().get(0);
            assertEquals(isSyntax, first instanceof EPCompileExceptionSyntaxItem);
            assertEquals(message, ex.getMessage());
        }
    }

    private static void assertStatement(RegressionEnvironment env) {
        assertEquals(2, env.statement("s0").getAnnotations().length);

        MyAnnotationValueArray array = (MyAnnotationValueArray) env.statement("s0").getAnnotations()[0];
        assertTrue(Arrays.deepEquals(toObjectArray(array.value()), new Object[]{1L, 2L, 3L}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.intArray()), new Object[]{4, 5}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.doubleArray()), new Object[]{}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.stringArray()), new Object[]{"X"}));
        assertTrue(Arrays.deepEquals(toObjectArray(array.stringArrayDef()), new Object[]{"XYZ"}));
    }

    private static void tryAssertion(EPStatement stmt) {
        Annotation[] annotations = stmt.getAnnotations();
        annotations = sortAlpha(annotations);
        assertEquals(3, annotations.length);

        assertEquals(Description.class, annotations[0].annotationType());
        assertEquals("MyTestStmt description", ((Description) annotations[0]).value());
        assertEquals("@Description(\"MyTestStmt description\")", annotations[0].toString());

        assertEquals(Name.class, annotations[1].annotationType());
        assertEquals("MyTestStmt", ((Name) annotations[1]).value());
        assertEquals("MyTestStmt", stmt.getName());
        assertEquals("@Name(\"MyTestStmt\")", annotations[1].toString());

        assertEquals(Tag.class, annotations[2].annotationType());
        assertEquals("UserId", ((Tag) annotations[2]).name());
        assertEquals("value", ((Tag) annotations[2]).value());
        assertEquals("@Tag(name=\"UserId\", value=\"value\")", annotations[2].toString());

        assertFalse(annotations[2].equals(annotations[1]));
        TestCase.assertTrue(annotations[1].equals(annotations[1]));
        TestCase.assertTrue(annotations[1].hashCode() != 0);
    }
}
