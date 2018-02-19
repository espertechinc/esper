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
package com.espertech.esper.util;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.supportunit.bean.*;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.espertech.esper.util.JavaClassHelper.isArrayTypeCompatible;
import static com.espertech.esper.util.JavaClassHelper.isCollectionMapOrArray;

public class TestJavaClassHelper extends TestCase {
    public void testArrayTypeCompatible() {
        assertTrue(isArrayTypeCompatible(int.class, int.class));
        assertTrue(isArrayTypeCompatible(int.class, Integer.class));
        assertTrue(isArrayTypeCompatible(Integer.class, int.class));
        assertTrue(isArrayTypeCompatible(Number.class, int.class));
        assertTrue(isArrayTypeCompatible(Number.class, Integer.class));
        assertTrue(isArrayTypeCompatible(Object.class, int.class));
        assertTrue(isArrayTypeCompatible(Object.class, Integer.class));

        assertTrue(isArrayTypeCompatible(Collection.class, Collection.class));
        assertTrue(isArrayTypeCompatible(Collection.class, ArrayList.class));
        assertTrue(isArrayTypeCompatible(Object.class, ArrayList.class));
        assertTrue(isArrayTypeCompatible(Object.class, Collection.class));

        assertFalse(isArrayTypeCompatible(Boolean.class, int.class));
        assertFalse(isArrayTypeCompatible(Integer.class, boolean.class));
        assertFalse(isArrayTypeCompatible(Long.class, Integer.class));
        assertFalse(isArrayTypeCompatible(Integer.class, byte.class));
    }

    public void testIsCollectionMapOrArray() {
        for (Class clazz : Arrays.asList(HashMap.class, Map.class, Collection.class, ArrayList.class, int[].class, Object[].class)) {
            assertTrue(isCollectionMapOrArray(clazz));
        }
        for (Class clazz : Arrays.asList(null, JavaClassHelper.class)) {
            assertFalse(isCollectionMapOrArray(clazz));
        }
    }

    public void testTakeFirstN() {
        Class[] classes = new Class[]{String.class};
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{String.class}, JavaClassHelper.takeFirstN(classes, 1));

        classes = new Class[]{String.class, Integer.class};
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{String.class, Integer.class}, JavaClassHelper.takeFirstN(classes, 2));

        classes = new Class[]{String.class, Integer.class, Double.class};
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{String.class}, JavaClassHelper.takeFirstN(classes, 1));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{String.class, Integer.class}, JavaClassHelper.takeFirstN(classes, 2));
    }

    public void testIsFragmentableType() {
        Class[] notFragmentables = new Class[]{
                String.class, int.class, Character.class, long.class, Map.class, HashMap.class, SupportEnum.class,
        };

        Class[] yesFragmentables = new Class[]{
                SupportBeanCombinedProps.class, SupportBeanCombinedProps.NestedLevOne.class, SupportBean.class
        };

        for (Class notFragmentable : notFragmentables) {
            assertFalse(JavaClassHelper.isFragmentableType(notFragmentable));
        }
        for (Class yesFragmentable : yesFragmentables) {
            assertTrue(JavaClassHelper.isFragmentableType(yesFragmentable));
        }
    }

    public void testGetParameterAsString() {
        Object[][] testCases = {
                {new Class[]{String.class, int.class}, "String, int"},
                {new Class[]{Integer.class, Boolean.class}, "Integer, Boolean"},
                {new Class[]{}, ""},
                {new Class[]{null}, "null (any type)"},
                {new Class[]{byte.class, null}, "byte, null (any type)"},
                {new Class[]{SupportBean.class, int[].class, int[][].class, Map.class}, "SupportBean, int[], int[][], Map"},
                {new Class[]{SupportBean[].class, SupportEnum.class, SupportBeanComplexProps.SupportBeanSpecialGetterNested.class}, "SupportBean[], SupportEnum, SupportBeanSpecialGetterNested"},
        };

        for (int i = 0; i < testCases.length; i++) {
            Class[] parameters = (Class[]) testCases[i][0];
            assertEquals(testCases[i][1], JavaClassHelper.getParameterAsString(parameters));
        }
    }

    public void testCanCoerce() {
        final Class[] primitiveClasses = {
                float.class, double.class, byte.class, short.class, int.class, long.class};

        final Class[] boxedClasses = {
                Float.class, Double.class, Byte.class, Short.class, Integer.class, Long.class};

        for (int i = 0; i < primitiveClasses.length; i++) {
            assertTrue(JavaClassHelper.canCoerce(primitiveClasses[i], boxedClasses[i]));
            assertTrue(JavaClassHelper.canCoerce(boxedClasses[i], boxedClasses[i]));
            assertTrue(JavaClassHelper.canCoerce(primitiveClasses[i], primitiveClasses[i]));
            assertTrue(JavaClassHelper.canCoerce(boxedClasses[i], primitiveClasses[i]));
        }

        assertTrue(JavaClassHelper.canCoerce(float.class, Double.class));
        assertFalse(JavaClassHelper.canCoerce(double.class, float.class));
        assertTrue(JavaClassHelper.canCoerce(int.class, long.class));
        assertFalse(JavaClassHelper.canCoerce(long.class, int.class));
        assertTrue(JavaClassHelper.canCoerce(long.class, double.class));
        assertTrue(JavaClassHelper.canCoerce(int.class, double.class));

        assertTrue(JavaClassHelper.canCoerce(BigInteger.class, BigInteger.class));
        assertTrue(JavaClassHelper.canCoerce(long.class, BigInteger.class));
        assertTrue(JavaClassHelper.canCoerce(Integer.class, BigInteger.class));
        assertTrue(JavaClassHelper.canCoerce(short.class, BigInteger.class));

        assertTrue(JavaClassHelper.canCoerce(float.class, BigDecimal.class));
        assertTrue(JavaClassHelper.canCoerce(Double.class, BigDecimal.class));
        assertTrue(JavaClassHelper.canCoerce(BigInteger.class, BigDecimal.class));
        assertTrue(JavaClassHelper.canCoerce(long.class, BigDecimal.class));
        assertTrue(JavaClassHelper.canCoerce(Integer.class, BigDecimal.class));
        assertTrue(JavaClassHelper.canCoerce(short.class, BigDecimal.class));

        try {
            JavaClassHelper.canCoerce(String.class, Float.class);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            JavaClassHelper.canCoerce(Float.class, Boolean.class);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testCoerceBoxed() {
        assertEquals(1d, JavaClassHelper.coerceBoxed(1d, Double.class));
        assertEquals(5d, JavaClassHelper.coerceBoxed(5, Double.class));
        assertEquals(6d, JavaClassHelper.coerceBoxed((byte) 6, Double.class));
        assertEquals(3f, JavaClassHelper.coerceBoxed((long) 3, Float.class));
        assertEquals((short) 2, JavaClassHelper.coerceBoxed((long) 2, Short.class));
        assertEquals(4, JavaClassHelper.coerceBoxed((long) 4, Integer.class));
        assertEquals((byte) 5, JavaClassHelper.coerceBoxed((long) 5, Byte.class));
        assertEquals(8l, JavaClassHelper.coerceBoxed((long) 8, Long.class));
        assertEquals(BigInteger.valueOf(8), JavaClassHelper.coerceBoxed(8, BigInteger.class));
        assertEquals(new BigDecimal(8), JavaClassHelper.coerceBoxed(8, BigDecimal.class));
        assertEquals(new BigDecimal(8d), JavaClassHelper.coerceBoxed(8d, BigDecimal.class));

        try {
            JavaClassHelper.coerceBoxed(10, int.class);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testIsNumeric() {
        final Class[] numericClasses = {
                float.class, Float.class, double.class, Double.class,
                byte.class, Byte.class, short.class, Short.class, int.class, Integer.class,
                long.class, Long.class, BigInteger.class, BigDecimal.class};

        final Class[] nonnumericClasses = {
                String.class, boolean.class, Boolean.class, TestCase.class};

        for (Class clazz : numericClasses) {
            assertTrue(JavaClassHelper.isNumeric(clazz));
        }

        for (Class clazz : nonnumericClasses) {
            assertFalse(JavaClassHelper.isNumeric(clazz));
        }
    }

    public void testIsNumericNonFP() {
        final Class[] numericClasses = {
                byte.class, Byte.class, short.class, Short.class, int.class, Integer.class,
                long.class, Long.class};

        final Class[] nonnumericClasses = {
                float.class, Float.class, double.class, Double.class, String.class, boolean.class, Boolean.class, TestCase.class};

        for (Class clazz : numericClasses) {
            assertTrue(JavaClassHelper.isNumericNonFP(clazz));
        }

        for (Class clazz : nonnumericClasses) {
            assertFalse(JavaClassHelper.isNumericNonFP(clazz));
        }
    }

    public void testGetBoxed() {
        final Class[] primitiveClasses = {
                boolean.class, float.class, double.class, byte.class, short.class, int.class, long.class, char.class};

        final Class[] boxedClasses = {
                Boolean.class, Float.class, Double.class, Byte.class, Short.class, Integer.class, Long.class, Character.class};

        final Class[] otherClasses = {
                String.class, TestCase.class};

        for (int i = 0; i < primitiveClasses.length; i++) {
            Class boxed = JavaClassHelper.getBoxedType(primitiveClasses[i]);
            assertEquals(boxed, boxedClasses[i]);
        }

        for (int i = 0; i < boxedClasses.length; i++) {
            Class boxed = JavaClassHelper.getBoxedType(boxedClasses[i]);
            assertEquals(boxed, boxedClasses[i]);
        }

        for (int i = 0; i < otherClasses.length; i++) {
            Class boxed = JavaClassHelper.getBoxedType(otherClasses[i]);
            assertEquals(boxed, otherClasses[i]);
        }
    }

    public void testGetPrimitive() {
        final Class[] primitiveClasses = {
                boolean.class, float.class, double.class, byte.class, short.class, int.class, long.class, char.class};

        final Class[] boxedClasses = {
                Boolean.class, Float.class, Double.class, Byte.class, Short.class, Integer.class, Long.class, Character.class};

        final Class[] otherClasses = {
                String.class, TestCase.class};

        for (int i = 0; i < primitiveClasses.length; i++) {
            Class primitive = JavaClassHelper.getPrimitiveType(boxedClasses[i]);
            assertEquals(primitive, primitiveClasses[i]);
        }

        for (int i = 0; i < boxedClasses.length; i++) {
            Class primitive = JavaClassHelper.getPrimitiveType(primitiveClasses[i]);
            assertEquals(primitive, primitiveClasses[i]);
        }

        for (int i = 0; i < otherClasses.length; i++) {
            Class clazz = JavaClassHelper.getPrimitiveType(otherClasses[i]);
            assertEquals(clazz, otherClasses[i]);
        }
    }

    public void testIsAssignmentCompatible() {
        Class[][] successCases = new Class[][]{
                {boolean.class, Boolean.class},
                {byte.class, short.class},
                {byte.class, Short.class},
                {byte.class, int.class},
                {byte.class, Integer.class},
                {Byte.class, long.class},
                {byte.class, Long.class},
                {byte.class, Double.class},
                {byte.class, double.class},
                {Byte.class, float.class},
                {byte.class, Float.class},
                {short.class, short.class},
                {Short.class, Short.class},
                {short.class, int.class},
                {short.class, Integer.class},
                {short.class, long.class},
                {Short.class, Long.class},
                {short.class, Double.class},
                {short.class, double.class},
                {short.class, float.class},
                {short.class, Float.class},
                {char.class, char.class},
                {Character.class, char.class},
                {char.class, Character.class},
                {char.class, int.class},
                {char.class, Integer.class},
                {char.class, long.class},
                {Character.class, Long.class},
                {char.class, Double.class},
                {char.class, double.class},
                {Character.class, float.class},
                {char.class, Float.class},
                {int.class, long.class},
                {Integer.class, Long.class},
                {int.class, Double.class},
                {Integer.class, double.class},
                {int.class, float.class},
                {int.class, Float.class},
                {Long.class, long.class},
                {long.class, Long.class},
                {long.class, Double.class},
                {Long.class, double.class},
                {long.class, float.class},
                {long.class, Float.class},
                {float.class, Double.class},
                {float.class, double.class},
                {float.class, float.class},
                {Float.class, Float.class},
                {HashSet.class, Set.class},
                {HashSet.class, Collection.class},
                {HashSet.class, Iterable.class},
                {HashSet.class, Cloneable.class},
                {HashSet.class, Serializable.class},
                {LineNumberReader.class, BufferedReader.class},
                {LineNumberReader.class, Reader.class},
                {LineNumberReader.class, Object.class},
                {LineNumberReader.class, Readable.class},
                {SortedSet.class, Set.class},
                {Set.class, Collection.class},
                {Set.class, Object.class},
                // widening of arrays allowed if supertype
                {Integer[].class, Number[].class},
                {Integer[].class, Object[].class},
                {LineNumberReader[].class, Reader[].class},
                {LineNumberReader[].class, Readable[].class},
                {LineNumberReader[].class, Object[].class},
                {ISupportAImplSuperG.class, ISupportA.class},
                {ISupportAImplSuperGImpl.class, ISupportA.class},
                {ISupportAImplSuperGImplPlus.class, ISupportA.class},
                {ISupportAImplSuperGImplPlus.class, ISupportB.class},
                {ISupportAImplSuperGImplPlus.class, ISupportC.class},
                {ISupportAImplSuperGImplPlus.class, ISupportAImplSuperG.class},
                {null, Object.class},
        };

        Class[][] failCases = new Class[][]{
                {int.class, Byte.class},
                {short.class, byte.class},
                {String.class, Boolean.class},
                {Boolean.class, String.class},
                {Byte.class, String.class},
                {char.class, byte.class},
                {char.class, short.class},
                {Character.class, short.class},
                {int.class, short.class},
                {long.class, int.class},
                {float.class, long.class},
                {Float.class, byte.class},
                {Double.class, char.class},
                {double.class, long.class},
                {Collection.class, Set.class},
                {Object.class, Collection.class},
                {Integer[].class, Float[].class},
                {Integer[].class, int[].class},
                {Integer[].class, double[].class},
                {Reader[].class, LineNumberReader[].class},
                {Readable[].class, Reader[].class},
        };

        for (int i = 0; i < successCases.length; i++) {
            assertTrue("Failed asserting success case " + successCases[i][0] +
                    " and " + successCases[i][1], JavaClassHelper.isAssignmentCompatible(successCases[i][0], successCases[i][1]));
        }
        for (int i = 0; i < failCases.length; i++) {
            assertFalse("Failed asserting fail case " + failCases[i][0] +
                    " and " + failCases[i][1], JavaClassHelper.isAssignmentCompatible(failCases[i][0], failCases[i][1]));
        }
    }

    public void testIsBoolean() {
        assertTrue(JavaClassHelper.isBoolean(Boolean.class));
        assertTrue(JavaClassHelper.isBoolean(boolean.class));
        assertFalse(JavaClassHelper.isBoolean(String.class));
    }

    public void testGetArithmaticCoercionType() {
        assertEquals(Double.class, JavaClassHelper.getArithmaticCoercionType(Double.class, int.class));
        assertEquals(Double.class, JavaClassHelper.getArithmaticCoercionType(byte.class, double.class));
        assertEquals(Long.class, JavaClassHelper.getArithmaticCoercionType(byte.class, long.class));
        assertEquals(Long.class, JavaClassHelper.getArithmaticCoercionType(byte.class, long.class));
        assertEquals(Double.class, JavaClassHelper.getArithmaticCoercionType(float.class, long.class));
        assertEquals(Double.class, JavaClassHelper.getArithmaticCoercionType(byte.class, float.class));
        assertEquals(Integer.class, JavaClassHelper.getArithmaticCoercionType(byte.class, int.class));
        assertEquals(Integer.class, JavaClassHelper.getArithmaticCoercionType(Integer.class, int.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getArithmaticCoercionType(Integer.class, BigDecimal.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getArithmaticCoercionType(BigDecimal.class, Integer.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getArithmaticCoercionType(BigInteger.class, float.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getArithmaticCoercionType(float.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getArithmaticCoercionType(Integer.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getArithmaticCoercionType(BigInteger.class, int.class));

        try {
            JavaClassHelper.getArithmaticCoercionType(String.class, float.class);
            fail();
        } catch (CoercionException ex) {
            // Expected
        }

        try {
            JavaClassHelper.getArithmaticCoercionType(int.class, boolean.class);
            fail();
        } catch (CoercionException ex) {
            // Expected
        }
    }

    public void testIsFloatingPointNumber() {
        assertTrue(JavaClassHelper.isFloatingPointNumber(1d));
        assertTrue(JavaClassHelper.isFloatingPointNumber(1f));
        assertTrue(JavaClassHelper.isFloatingPointNumber(new Double(1)));
        assertTrue(JavaClassHelper.isFloatingPointNumber(new Float(1)));

        assertFalse(JavaClassHelper.isFloatingPointNumber(1));
        assertFalse(JavaClassHelper.isFloatingPointNumber(new Integer(1)));
    }

    public void testIsFloatingPointClass() {
        assertTrue(JavaClassHelper.isFloatingPointClass(double.class));
        assertTrue(JavaClassHelper.isFloatingPointClass(float.class));
        assertTrue(JavaClassHelper.isFloatingPointClass(Double.class));
        assertTrue(JavaClassHelper.isFloatingPointClass(Float.class));

        assertFalse(JavaClassHelper.isFloatingPointClass(String.class));
        assertFalse(JavaClassHelper.isFloatingPointClass(int.class));
        assertFalse(JavaClassHelper.isFloatingPointClass(Integer.class));
    }

    public void testGetCompareToCoercionType() {
        assertEquals(String.class, JavaClassHelper.getCompareToCoercionType(String.class, String.class));
        assertEquals(Boolean.class, JavaClassHelper.getCompareToCoercionType(Boolean.class, Boolean.class));
        assertEquals(Boolean.class, JavaClassHelper.getCompareToCoercionType(Boolean.class, boolean.class));
        assertEquals(Boolean.class, JavaClassHelper.getCompareToCoercionType(boolean.class, Boolean.class));
        assertEquals(Boolean.class, JavaClassHelper.getCompareToCoercionType(boolean.class, boolean.class));

        assertEquals(Double.class, JavaClassHelper.getCompareToCoercionType(int.class, float.class));
        assertEquals(Double.class, JavaClassHelper.getCompareToCoercionType(double.class, byte.class));
        assertEquals(Float.class, JavaClassHelper.getCompareToCoercionType(float.class, float.class));
        assertEquals(Double.class, JavaClassHelper.getCompareToCoercionType(float.class, Double.class));

        assertEquals(Integer.class, JavaClassHelper.getCompareToCoercionType(int.class, int.class));
        assertEquals(Integer.class, JavaClassHelper.getCompareToCoercionType(Short.class, Integer.class));

        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(BigDecimal.class, int.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(Double.class, BigDecimal.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(byte.class, BigDecimal.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(BigInteger.class, BigDecimal.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(BigDecimal.class, BigDecimal.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(double.class, BigInteger.class));
        assertEquals(BigDecimal.class, JavaClassHelper.getCompareToCoercionType(Float.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getCompareToCoercionType(BigInteger.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getCompareToCoercionType(long.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getCompareToCoercionType(short.class, BigInteger.class));
        assertEquals(BigInteger.class, JavaClassHelper.getCompareToCoercionType(Integer.class, BigInteger.class));

        assertEquals(SupportBean.class, JavaClassHelper.getCompareToCoercionType(SupportBean.class, SupportBean.class));
        assertEquals(Object.class, JavaClassHelper.getCompareToCoercionType(SupportBean.class, SupportBean_A.class));

        assertEquals("Types cannot be compared: java.lang.Boolean and java.math.BigInteger",
                tryInvalidGetRelational(Boolean.class, BigInteger.class));
        tryInvalidGetRelational(String.class, BigDecimal.class);
        tryInvalidGetRelational(String.class, int.class);
        tryInvalidGetRelational(Long.class, String.class);
        tryInvalidGetRelational(Long.class, Boolean.class);
        tryInvalidGetRelational(boolean.class, int.class);
    }

    public void testGetBoxedClassName() throws Exception {
        String[][] tests = new String[][]{
                {Integer.class.getName(), int.class.getName()},
                {Long.class.getName(), long.class.getName()},
                {Short.class.getName(), short.class.getName()},
                {Double.class.getName(), double.class.getName()},
                {Float.class.getName(), float.class.getName()},
                {Boolean.class.getName(), boolean.class.getName()},
                {Byte.class.getName(), byte.class.getName()},
                {Character.class.getName(), char.class.getName()}
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals(tests[i][0], JavaClassHelper.getBoxedClassName(tests[i][1]));
        }
    }

    public void testClassForName() throws Exception {
        Object[][] tests = new Object[][]{
                {int.class, int.class.getName()},
                {long.class, long.class.getName()},
                {short.class, short.class.getName()},
                {double.class, double.class.getName()},
                {float.class, float.class.getName()},
                {boolean.class, boolean.class.getName()},
                {byte.class, byte.class.getName()},
                {char.class, char.class.getName()}};

        for (int i = 0; i < tests.length; i++) {
            assertEquals(tests[i][0], JavaClassHelper.getClassForName((String) tests[i][1], ClassForNameProviderDefault.INSTANCE));
        }
    }

    public void testClassForSimpleName() throws Exception {
        Object[][] tests = new Object[][]{
                {"Boolean", Boolean.class},
                {"Bool", Boolean.class},
                {"boolean", Boolean.class},
                {"java.lang.Boolean", Boolean.class},
                {"int", Integer.class},
                {"inTeger", Integer.class},
                {"java.lang.Integer", Integer.class},
                {"long", Long.class},
                {"LONG", Long.class},
                {"java.lang.Short", Short.class},
                {"short", Short.class},
                {"  short  ", Short.class},
                {"double", Double.class},
                {" douBle", Double.class},
                {"java.lang.Double", Double.class},
                {"float", Float.class},
                {"float  ", Float.class},
                {"java.lang.Float", Float.class},
                {"byte", Byte.class},
                {"   bYte ", Byte.class},
                {"java.lang.Byte", Byte.class},
                {"char", Character.class},
                {"character", Character.class},
                {"java.lang.Character", Character.class},
                {"string", String.class},
                {"java.lang.String", String.class},
                {"varchar", String.class},
                {"varchar2", String.class},
                {SupportBean.class.getName(), SupportBean.class},
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals("error in row:" + i, tests[i][1], JavaClassHelper.getClassForSimpleName((String) tests[i][0], ClassForNameProviderDefault.INSTANCE));
        }
    }

    public void testParse() throws Exception {
        Object[][] tests = new Object[][]{
                {Boolean.class, "TrUe", true},
                {Boolean.class, "false", false},
                {boolean.class, "false", false},
                {boolean.class, "true", true},
                {int.class, "73737474 ", 73737474},
                {Integer.class, " -1 ", -1},
                {long.class, "123456789001222L", 123456789001222L},
                {Long.class, " -2 ", -2L},
                {Long.class, " -2L ", -2L},
                {Long.class, " -2l ", -2L},
                {Short.class, " -3 ", (short) -3},
                {short.class, "111", (short) 111},
                {Double.class, " -3d ", -3d},
                {double.class, "111.38373", 111.38373d},
                {Double.class, " -3.1D ", -3.1D},
                {Float.class, " -3f ", -3f},
                {float.class, "111.38373", 111.38373f},
                {Float.class, " -3.1F ", -3.1f},
                {Byte.class, " -3 ", (byte) -3},
                {byte.class, " 1 ", (byte) 1},
                {char.class, "ABC", 'A'},
                {Character.class, " AB", ' '},
                {String.class, "AB", "AB"},
                {String.class, " AB ", " AB "},
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals("error in row:" + i, tests[i][2], JavaClassHelper.parse((Class) tests[i][0], (String) tests[i][1]));
        }
    }

    public void testGetParser() throws Exception {
        Object[][] tests = new Object[][]{
                {Boolean.class, "TrUe", true},
                {Boolean.class, "false", false},
                {boolean.class, "false", false},
                {boolean.class, "true", true},
                {int.class, "73737474 ", 73737474},
                {Integer.class, " -1 ", -1},
                {long.class, "123456789001222L", 123456789001222L},
                {Long.class, " -2 ", -2L},
                {Long.class, " -2L ", -2L},
                {Long.class, " -2l ", -2L},
                {Short.class, " -3 ", (short) -3},
                {short.class, "111", (short) 111},
                {Double.class, " -3d ", -3d},
                {double.class, "111.38373", 111.38373d},
                {Double.class, " -3.1D ", -3.1D},
                {Float.class, " -3f ", -3f},
                {float.class, "111.38373", 111.38373f},
                {Float.class, " -3.1F ", -3.1f},
                {Byte.class, " -3 ", (byte) -3},
                {byte.class, " 1 ", (byte) 1},
                {char.class, "ABC", 'A'},
                {Character.class, " AB", ' '},
                {String.class, "AB", "AB"},
                {String.class, " AB ", " AB "},
        };

        for (int i = 0; i < tests.length; i++) {
            SimpleTypeParser parser = SimpleTypeParserFactory.getParser((Class) tests[i][0]);
            assertEquals("error in row:" + i, tests[i][2], parser.parse((String) tests[i][1]));
        }
    }

    public void testIsJavaBuiltinDataType() {
        Class[] classesDataType = new Class[]{int.class, Long.class, double.class, boolean.class, Boolean.class,
                char.class, Character.class, String.class, CharSequence.class};
        Class[] classesNotDataType = new Class[]{SupportBean.class, Math.class, Class.class, Object.class};

        for (int i = 0; i < classesDataType.length; i++) {
            assertTrue(JavaClassHelper.isJavaBuiltinDataType(classesDataType[i]));
        }
        for (int i = 0; i < classesNotDataType.length; i++) {
            assertFalse(JavaClassHelper.isJavaBuiltinDataType(classesNotDataType[i]));
        }
        assertTrue(JavaClassHelper.isJavaBuiltinDataType(null));
    }

    private String tryInvalidGetRelational(Class classOne, Class classTwo) {
        try {
            JavaClassHelper.getCompareToCoercionType(classOne, classTwo);
            fail();
            return null;
        } catch (CoercionException ex) {
            return ex.getMessage();
        }
    }

    public void testGetCommonCoercionType() {
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{String.class}));
        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{boolean.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{long.class}));

        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{String.class, null}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{String.class, String.class}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{String.class, String.class, String.class}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{String.class, String.class, null}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, String.class, null}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, String.class, String.class}));
        assertEquals(String.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, null, String.class, String.class}));

        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{Boolean.class, Boolean.class}));
        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{Boolean.class, boolean.class}));
        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{boolean.class, Boolean.class}));
        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{boolean.class, boolean.class}));
        assertEquals(Boolean.class, JavaClassHelper.getCommonCoercionType(new Class[]{Boolean.class, boolean.class, boolean.class}));
        assertEquals(Integer.class, JavaClassHelper.getCommonCoercionType(new Class[]{int.class, byte.class, int.class}));
        assertEquals(Integer.class, JavaClassHelper.getCommonCoercionType(new Class[]{Integer.class, Byte.class, Short.class}));
        assertEquals(Integer.class, JavaClassHelper.getCommonCoercionType(new Class[]{byte.class, short.class, short.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{Integer.class, Byte.class, Double.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{Long.class, Double.class, Double.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{double.class, byte.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{double.class, byte.class, null}));
        assertEquals(Float.class, JavaClassHelper.getCommonCoercionType(new Class[]{float.class, float.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{float.class, int.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{Integer.class, int.class, Float.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{Integer.class, int.class, long.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{long.class, int.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{long.class, int.class, int.class, int.class, byte.class, short.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{long.class, null, int.class, null, int.class, int.class, null, byte.class, short.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{Integer.class, int.class, long.class}));
        assertEquals(Character.class, JavaClassHelper.getCommonCoercionType(new Class[]{char.class, char.class, char.class}));
        assertEquals(Long.class, JavaClassHelper.getCommonCoercionType(new Class[]{int.class, int.class, int.class, long.class, int.class, int.class}));
        assertEquals(Double.class, JavaClassHelper.getCommonCoercionType(new Class[]{int.class, long.class, int.class, double.class, int.class, int.class}));
        assertEquals(null, JavaClassHelper.getCommonCoercionType(new Class[]{null, null}));
        assertEquals(null, JavaClassHelper.getCommonCoercionType(new Class[]{null, null, null}));
        assertEquals(SupportBean.class, JavaClassHelper.getCommonCoercionType(new Class[]{SupportBean.class, null, null}));
        assertEquals(SupportBean.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, SupportBean.class, null}));
        assertEquals(SupportBean.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, SupportBean.class}));
        assertEquals(SupportBean.class, JavaClassHelper.getCommonCoercionType(new Class[]{null, null, SupportBean.class}));
        assertEquals(SupportBean.class, JavaClassHelper.getCommonCoercionType(new Class[]{SupportBean.class, null, SupportBean.class, SupportBean.class}));
        assertEquals(Object.class, JavaClassHelper.getCommonCoercionType(new Class[]{SupportBean.class, SupportBean_A.class, null, SupportBean.class, SupportBean.class}));

        assertEquals(int[].class, JavaClassHelper.getCommonCoercionType(new Class[]{int[].class, int[].class}));
        assertEquals(long[].class, JavaClassHelper.getCommonCoercionType(new Class[]{long[].class, long[].class}));
        assertEquals(String[].class, JavaClassHelper.getCommonCoercionType(new Class[]{String[].class, String[].class}));
        assertEquals(Object[].class, JavaClassHelper.getCommonCoercionType(new Class[]{String[].class, Integer[].class}));
        assertEquals(Object[].class, JavaClassHelper.getCommonCoercionType(new Class[]{Object[].class, Integer[].class}));

        assertEquals("Cannot coerce to String type java.lang.Boolean", tryInvalidGetCommonCoercionType(new Class[]{String.class, Boolean.class}));
        tryInvalidGetCommonCoercionType(new Class[]{String.class, String.class, Boolean.class});
        tryInvalidGetCommonCoercionType(new Class[]{Boolean.class, String.class, Boolean.class});
        tryInvalidGetCommonCoercionType(new Class[]{Boolean.class, Boolean.class, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{long.class, Boolean.class, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{double.class, long.class, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{null, double.class, long.class, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{String.class, String.class, long.class});
        tryInvalidGetCommonCoercionType(new Class[]{String.class, SupportBean.class});
        tryInvalidGetCommonCoercionType(new Class[]{boolean.class, null, null, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{int.class, null, null, String.class});
        tryInvalidGetCommonCoercionType(new Class[]{SupportBean.class, Boolean.class});
        tryInvalidGetCommonCoercionType(new Class[]{String.class, SupportBean.class});
        tryInvalidGetCommonCoercionType(new Class[]{SupportBean.class, String.class, SupportBean.class});
        tryInvalidGetCommonCoercionType(new Class[]{int[].class, Integer[].class});
        tryInvalidGetCommonCoercionType(new Class[]{Object[].class, boolean[].class, Integer[].class});

        try {
            JavaClassHelper.getCommonCoercionType(new Class[0]);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testGetPrimitiveClassForName() {
        Object[][] tests = new Object[][]{
                {"int", int.class},
                {"Long", long.class},
                {"SHort", short.class},
                {"DOUBLE", double.class},
                {"float", float.class},
                {"boolean", boolean.class},
                {"ByTe", byte.class},
                {"char", char.class},
                {"jfjfjf", null},
                {SupportBean.class.getName(), null},
                {"string", String.class},
                {"STRINg", String.class}
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals(tests[i][1], JavaClassHelper.getPrimitiveClassForName((String) tests[i][0]));
        }
    }

    public void testImplementsInterface() {
        Object[][] tests = new Object[][]{
                {HashMap.class, Map.class, true},
                {AbstractMap.class, Map.class, true},
                {TreeMap.class, Map.class, true},
                {String.class, Map.class, false},
                {SupportBean_S0.class, SupportMarkerInterface.class, false},
                {SupportBean_E.class, SupportMarkerInterface.class, true},
                {SupportBean_F.class, SupportMarkerInterface.class, true},
                {SupportBeanBase.class, SupportMarkerInterface.class, true},
                {SupportOverrideOneB.class, SupportMarkerInterface.class, true}
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals("test failed for " + tests[i][0], tests[i][2], JavaClassHelper.isImplementsInterface((Class) tests[i][0], (Class) tests[i][1]));
        }
    }

    public void testImplementsOrExtends() {
        Object[][] tests = new Object[][]{
                {HashMap.class, Map.class, true},
                {AbstractMap.class, Map.class, true},
                {TreeMap.class, Map.class, true},
                {String.class, Map.class, false},
                {SupportBean_S0.class, SupportMarkerInterface.class, false},
                {SupportBean_E.class, SupportMarkerInterface.class, true},
                {SupportBean_F.class, SupportMarkerInterface.class, true},
                {SupportBeanBase.class, SupportMarkerInterface.class, true},
                {SupportOverrideOneB.class, SupportMarkerInterface.class, true},
                {SupportOverrideBase.class, SupportOverrideBase.class, true},
                {SupportBean_F.class, SupportOverrideBase.class, false},
                {SupportOverrideOne.class, SupportOverrideBase.class, true},
                {SupportOverrideOneA.class, SupportOverrideBase.class, true},
                {SupportOverrideOneB.class, SupportOverrideBase.class, true},
                {SupportOverrideOneB.class, Serializable.class, true},
                {SupportOverrideOneB.class, String.class, false},
        };

        for (int i = 0; i < tests.length; i++) {
            assertEquals("test failed for " + tests[i][0] + " and " + tests[i][1], tests[i][2],
                    JavaClassHelper.isSubclassOrImplementsInterface((Class) tests[i][0], (Class) tests[i][1]));
        }
    }

    public void testIsSimpleNameFullyQualfied() {
        assertTrue(JavaClassHelper.isSimpleNameFullyQualfied("ABC", "ABC"));
        assertTrue(JavaClassHelper.isSimpleNameFullyQualfied("ABC", "com.abc.ABC"));
        assertTrue(JavaClassHelper.isSimpleNameFullyQualfied("ABC", "abc.ABC"));
        assertFalse(JavaClassHelper.isSimpleNameFullyQualfied("DABC", "abc.ABC"));
        assertFalse(JavaClassHelper.isSimpleNameFullyQualfied("AB", "abc.ABC"));
        assertFalse(JavaClassHelper.isSimpleNameFullyQualfied("AB", "ABC"));
    }

    public void testIsBigNumberType() {
        assertTrue(JavaClassHelper.isBigNumberType(BigInteger.class));
        assertTrue(JavaClassHelper.isBigNumberType(BigDecimal.class));
        assertFalse(JavaClassHelper.isBigNumberType(String.class));
        assertFalse(JavaClassHelper.isBigNumberType(Double.class));
    }

    public void testGetGenericReturnType() throws Exception {
        Object[][] testcases = new Object[][]{
                {"getList", String.class},
                {"getListObject", Object.class},
                {"getListUndefined", null},
                {"getIterator", Integer.class},
                {"getNested", MyClassWithGetters.class},
                {"getIntPrimitive", null},
                {"getIntBoxed", null},
        };

        for (int i = 0; i < testcases.length; i++) {
            String name = testcases[i][0].toString();
            Method m = MyClassWithGetters.class.getMethod(name);
            Class expected = (Class) testcases[i][1];
            assertEquals("Testing " + name, expected, JavaClassHelper.getGenericReturnType(m, true));
        }
    }

    public void testGetGenericFieldType() throws Exception {
        Object[][] testcases = new Object[][]{
                {"list", String.class},
                {"listObject", Object.class},
                {"listUndefined", null},
                {"iterable", Integer.class},
                {"nested", MyClassWithGetters.class},
                {"intPrimitive", null},
                {"intBoxed", null},
        };

        for (int i = 0; i < testcases.length; i++) {
            String name = testcases[i][0].toString();
            Field f = MyClassWithFields.class.getField(name);
            Class expected = (Class) testcases[i][1];
            assertEquals("Testing " + name, expected, JavaClassHelper.getGenericFieldType(f, true));
        }
    }

    public void testGetGenericFieldTypeMap() throws Exception {
        Object[][] testcases = new Object[][]{
                {"mapUndefined", null},
                {"mapObject", Object.class},
                {"mapBoolean", Boolean.class},
                {"mapNotMap", null},
        };

        for (int i = 0; i < testcases.length; i++) {
            String name = testcases[i][0].toString();
            Field f = MyClassWithFields.class.getField(name);
            Class expected = (Class) testcases[i][1];
            assertEquals("Testing " + name, expected, JavaClassHelper.getGenericFieldTypeMap(f, true));
        }
    }

    public void testGetGenericReturnTypeMap() throws Exception {
        Object[][] testcases = new Object[][]{
                {"getMapUndefined", null},
                {"getMapObject", Object.class},
                {"getMapBoolean", Boolean.class},
                {"getMapNotMap", null},
        };

        for (int i = 0; i < testcases.length; i++) {
            String name = testcases[i][0].toString();
            Method m = MyClassWithGetters.class.getMethod(name);
            Class expected = (Class) testcases[i][1];
            assertEquals("Testing " + name, expected, JavaClassHelper.getGenericReturnTypeMap(m, true));
        }
    }

    public void testGetClassObjectFromPropertyTypeNames() throws Exception {
        Properties props = new Properties();
        props.put("p0", "string");
        props.put("p1", "int");
        props.put("p2", SupportBean.class.getName());

        Map<String, Object> map = JavaClassHelper.getClassObjectFromPropertyTypeNames(props, ClassForNameProviderDefault.INSTANCE);
        assertEquals(String.class, map.get("p0"));
        assertEquals(Integer.class, map.get("p1"));
        assertEquals(SupportBean.class, map.get("p2"));
    }

    private String tryInvalidGetCommonCoercionType(Class[] types) {
        try {
            JavaClassHelper.getCommonCoercionType(types);
            fail();
            return null;
        } catch (CoercionException ex) {
            return ex.getMessage();
        }
    }

    class MyStringList extends ArrayList<String> {
    }

    class MyClassWithGetters {
        public ArrayList<Object> getListObject() {
            return null;
        }

        public ArrayList getListUndefined() {
            return null;
        }

        public ArrayList<String> getList() {
            return null;
        }

        public Iterator<Integer> getIterator() {
            return null;
        }

        public Set<MyClassWithGetters> getNested() {
            return null;
        }

        public Integer getIntBoxed() {
            return null;
        }

        public int getIntPrimitive() {
            return 1;
        }

        public Map getMapUndefined() {
            return null;
        }

        public Map<String, Object> getMapObject() {
            return null;
        }

        public Map<String, Boolean> getMapBoolean() {
            return null;
        }

        public Integer getMapNotMap() {
            return null;
        }
    }

    class MyClassWithFields {
        public ArrayList<Object> listObject;
        public ArrayList listUndefined;
        public ArrayList<String> list;
        public Iterable<Integer> iterable;
        public Set<MyClassWithGetters> nested;
        public Integer intBoxed;
        public int intPrimitive;

        public Map mapUndefined;
        public Map<String, Object> mapObject;
        public Map<String, Boolean> mapBoolean;
        public Integer mapNotMap;
    }
}
