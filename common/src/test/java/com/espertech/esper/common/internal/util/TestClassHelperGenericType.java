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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static com.espertech.esper.common.internal.util.ClassHelperGenericType.*;

public class TestClassHelperGenericType extends TestCase {
    public void testMethodAndField() {
        assertType("intPrimitive", EPTypePremade.INTEGERPRIMITIVE.getEPType());
        assertType("list", new EPTypeClass(List.class));
        assertType("listOfObject", EPTypeClassParameterized.from(List.class, Object.class));
        assertType("listOfString", EPTypeClassParameterized.from(List.class, String.class));
        assertType("listOfOptionalInteger", new EPTypeClassParameterized(List.class, new EPTypeClass[] {EPTypeClassParameterized.from(Optional.class, Integer.class)}));
        assertType("listUpperBoundedWildcard", EPTypeClassParameterized.from(List.class, Comparable.class));
        assertType("listUnboundedWildcard", EPTypeClassParameterized.from(List.class, Object.class));
        assertType("listLowerBoundedWildcard", EPTypeClassParameterized.from(List.class, Object.class));
        assertType("mapOfStringAndInteger", new EPTypeClassParameterized(Map.class, new EPTypeClass[] {EPTypePremade.STRING.getEPType(), EPTypePremade.INTEGERBOXED.getEPType()}));
        assertType("intPrimitiveArray", new EPTypeClass(int[].class));
        assertType("listArrayOfString", new EPTypeClassParameterized(List[].class, new EPTypeClass[] {EPTypePremade.STRING.getEPType()}));
        assertType("listOfStringArray", new EPTypeClassParameterized(List.class, new EPTypeClass[] {new EPTypeClass(String[].class)}));
        assertType("listArray2DimOfString", new EPTypeClassParameterized(List[][].class, new EPTypeClass[] {EPTypePremade.STRING.getEPType()}));
        assertType("listOfStringArray2Dim", new EPTypeClassParameterized(List.class, new EPTypeClass[] {new EPTypeClass(String[][].class)}));
        assertType("listOfT", new EPTypeClassParameterized(List.class, new EPTypeClass[] {new EPTypeClass(Object.class)}));
    }

    public void testParameterized() {
        assertTypeParameterized("listOfT", new EPTypeClass(String.class), EPTypeClassParameterized.from(List.class, String.class));
        assertTypeParameterized("listOfT", new EPTypeClass(Integer.class), EPTypeClassParameterized.from(List.class, Integer.class));
        assertTypeParameterized("listOfTArray", new EPTypeClass(String.class), EPTypeClassParameterized.from(List.class, String[].class));
        assertTypeParameterized("mapOfTtoDouble", new EPTypeClass(Byte.class), EPTypeClassParameterized.from(Map.class, Byte.class, Double.class));

        EPTypeClass inner = EPTypeClassParameterized.from(List.class, String.class);
        assertTypeParameterized("mapOfIntegerToListOfT", new EPTypeClass(String.class), new EPTypeClassParameterized(Map.class, new EPTypeClass[] {EPTypePremade.INTEGERBOXED.getEPType(), inner}));
    }

    private void assertTypeParameterized(String methodName, EPTypeClass parameter, EPTypeClass expected) {
        Method method = getMethod(methodName, MyClassForParameterized.class);
        EPTypeClass received = getMethodReturnEPType(method, from(MyClassForParameterized.class, parameter));
        assertEquals(expected, received);
    }

    private void assertType(String methodAndFieldName, EPTypeClass expected) {
        Method method = getMethod(methodAndFieldName, MyInterfaceForMethodReturnType.class);
        EPTypeClass receivedMethod = getMethodReturnEPType(method);
        assertEquals(expected, receivedMethod);

        Field field;
        try {
            field = MyFieldReturnType.class.getField(methodAndFieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        EPTypeClass receivedField = getFieldEPType(field);
        assertEquals(expected, receivedField);
    }

    private Method getMethod(String methodName, Class clazz) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public interface MyInterfaceForMethodReturnType<T> {
        int intPrimitive();
        List list();
        List<Object> listOfObject();
        List<String> listOfString();
        List<Optional<Integer>> listOfOptionalInteger();
        List<? extends Comparable> listUpperBoundedWildcard();
        List<?> listUnboundedWildcard();
        List<? super Integer> listLowerBoundedWildcard();
        Map<String, Integer> mapOfStringAndInteger();
        int[] intPrimitiveArray();
        List<String>[] listArrayOfString();
        List<String[]> listOfStringArray();
        List<String>[][] listArray2DimOfString();
        List<String[][]> listOfStringArray2Dim();
        List<T> listOfT();
    }

    public static class MyFieldReturnType<T> {
        public int intPrimitive;
        public List list;
        public List<Object> listOfObject;
        public List<String> listOfString;
        public List<Optional<Integer>> listOfOptionalInteger;
        public List<? extends Comparable> listUpperBoundedWildcard;
        public List<?> listUnboundedWildcard;
        public List<? super Integer> listLowerBoundedWildcard;
        public Map<String, Integer> mapOfStringAndInteger;
        public int[] intPrimitiveArray;
        public List<String>[] listArrayOfString;
        public List<String[]> listOfStringArray;
        public List<String>[][] listArray2DimOfString;
        public List<String[][]> listOfStringArray2Dim;
        public List<T> listOfT;
    }

    public static class MyClassForParameterized<T> {
        public List<T> listOfT() {return null;};
        public List<T[]> listOfTArray() {return null;};
        public Map<T, Double> mapOfTtoDouble() {return null;};
        public Map<Integer, List<T>> mapOfIntegerToListOfT() {return null;};
    }
}
