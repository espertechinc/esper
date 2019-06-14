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
package com.espertech.esper.common.internal.event.json.parser.forge;

import com.espertech.esper.common.client.annotation.JsonSchemaField;
import com.espertech.esper.common.client.json.util.JsonFieldAdapterString;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericArray;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericObject;
import com.espertech.esper.common.internal.event.json.parser.delegates.array.*;
import com.espertech.esper.common.internal.event.json.parser.delegates.array2dim.*;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.*;
import com.espertech.esper.common.internal.event.json.write.*;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.util.ConstructorHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.MethodResolver;
import com.espertech.esper.common.internal.util.MethodResolverNoSuchMethodException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class JsonForgeFactoryClassTyped {
    private final static Map<Class, JsonEndValueForge> END_VALUE_FORGES = new HashMap<>();
    private final static Map<Class, Class> START_ARRAY_FORGES = new HashMap<>();
    private final static Map<Class, JsonWriteForge> WRITE_FORGES = new HashMap<>();
    private final static Map<Class, JsonWriteForge> WRITE_ARRAY_FORGES = new HashMap<>();

    static {
        END_VALUE_FORGES.put(String.class, JsonEndValueForgeString.INSTANCE);
        END_VALUE_FORGES.put(Character.class, JsonEndValueForgeCharacter.INSTANCE);
        END_VALUE_FORGES.put(Boolean.class, JsonEndValueForgeBoolean.INSTANCE);
        END_VALUE_FORGES.put(Byte.class, JsonEndValueForgeByte.INSTANCE);
        END_VALUE_FORGES.put(Short.class, JsonEndValueForgeShort.INSTANCE);
        END_VALUE_FORGES.put(Integer.class, JsonEndValueForgeInteger.INSTANCE);
        END_VALUE_FORGES.put(Long.class, JsonEndValueForgeLong.INSTANCE);
        END_VALUE_FORGES.put(Double.class, JsonEndValueForgeDouble.INSTANCE);
        END_VALUE_FORGES.put(Float.class, JsonEndValueForgeFloat.INSTANCE);
        END_VALUE_FORGES.put(BigDecimal.class, JsonEndValueForgeBigDecimal.INSTANCE);
        END_VALUE_FORGES.put(BigInteger.class, JsonEndValueForgeBigInteger.INSTANCE);

        WRITE_FORGES.put(String.class, JsonWriteForgeString.INSTANCE);
        WRITE_FORGES.put(Character.class, JsonWriteForgeStringWithToString.INSTANCE);
        WRITE_FORGES.put(Boolean.class, JsonWriteForgeBoolean.INSTANCE);
        WRITE_FORGES.put(Byte.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(Short.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(Integer.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(Long.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(Double.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(Float.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(BigDecimal.class, JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(BigInteger.class, JsonWriteForgeNumberWithToString.INSTANCE);

        START_ARRAY_FORGES.put(String[].class, JsonDelegateArrayString.class);
        START_ARRAY_FORGES.put(Character[].class, JsonDelegateArrayCharacter.class);
        START_ARRAY_FORGES.put(Boolean[].class, JsonDelegateArrayBoolean.class);
        START_ARRAY_FORGES.put(Byte[].class, JsonDelegateArrayByte.class);
        START_ARRAY_FORGES.put(Short[].class, JsonDelegateArrayShort.class);
        START_ARRAY_FORGES.put(Integer[].class, JsonDelegateArrayInteger.class);
        START_ARRAY_FORGES.put(Long[].class, JsonDelegateArrayLong.class);
        START_ARRAY_FORGES.put(Double[].class, JsonDelegateArrayDouble.class);
        START_ARRAY_FORGES.put(Float[].class, JsonDelegateArrayFloat.class);
        START_ARRAY_FORGES.put(char[].class, JsonDelegateArrayCharacterPrimitive.class);
        START_ARRAY_FORGES.put(boolean[].class, JsonDelegateArrayBooleanPrimitive.class);
        START_ARRAY_FORGES.put(byte[].class, JsonDelegateArrayBytePrimitive.class);
        START_ARRAY_FORGES.put(short[].class, JsonDelegateArrayShortPrimitive.class);
        START_ARRAY_FORGES.put(int[].class, JsonDelegateArrayIntegerPrimitive.class);
        START_ARRAY_FORGES.put(long[].class, JsonDelegateArrayLongPrimitive.class);
        START_ARRAY_FORGES.put(double[].class, JsonDelegateArrayDoublePrimitive.class);
        START_ARRAY_FORGES.put(float[].class, JsonDelegateArrayFloatPrimitive.class);
        START_ARRAY_FORGES.put(BigDecimal[].class, JsonDelegateArrayBigDecimal.class);
        START_ARRAY_FORGES.put(BigInteger[].class, JsonDelegateArrayBigInteger.class);

        START_ARRAY_FORGES.put(String[][].class, JsonDelegateArray2DimString.class);
        START_ARRAY_FORGES.put(Character[][].class, JsonDelegateArray2DimCharacter.class);
        START_ARRAY_FORGES.put(Boolean[][].class, JsonDelegateArray2DimBoolean.class);
        START_ARRAY_FORGES.put(Byte[][].class, JsonDelegateArray2DimByte.class);
        START_ARRAY_FORGES.put(Short[][].class, JsonDelegateArray2DimShort.class);
        START_ARRAY_FORGES.put(Integer[][].class, JsonDelegateArray2DimInteger.class);
        START_ARRAY_FORGES.put(Long[][].class, JsonDelegateArray2DimLong.class);
        START_ARRAY_FORGES.put(Double[][].class, JsonDelegateArray2DimDouble.class);
        START_ARRAY_FORGES.put(Float[][].class, JsonDelegateArray2DimFloat.class);
        START_ARRAY_FORGES.put(char[][].class, JsonDelegateArray2DimCharacterPrimitive.class);
        START_ARRAY_FORGES.put(boolean[][].class, JsonDelegateArray2DimBooleanPrimitive.class);
        START_ARRAY_FORGES.put(byte[][].class, JsonDelegateArray2DimBytePrimitive.class);
        START_ARRAY_FORGES.put(short[][].class, JsonDelegateArray2DimShortPrimitive.class);
        START_ARRAY_FORGES.put(int[][].class, JsonDelegateArray2DimIntegerPrimitive.class);
        START_ARRAY_FORGES.put(long[][].class, JsonDelegateArray2DimLongPrimitive.class);
        START_ARRAY_FORGES.put(double[][].class, JsonDelegateArray2DimDoublePrimitive.class);
        START_ARRAY_FORGES.put(float[][].class, JsonDelegateArray2DimFloatPrimitive.class);
        START_ARRAY_FORGES.put(BigDecimal[][].class, JsonDelegateArray2DimBigDecimal.class);
        START_ARRAY_FORGES.put(BigInteger[][].class, JsonDelegateArray2DimBigInteger.class);

        WRITE_ARRAY_FORGES.put(String[].class, new JsonWriteForgeByMethod("writeArrayString"));
        WRITE_ARRAY_FORGES.put(Character[].class, new JsonWriteForgeByMethod("writeArrayCharacter"));
        WRITE_ARRAY_FORGES.put(Boolean[].class, new JsonWriteForgeByMethod("writeArrayBoolean"));
        WRITE_ARRAY_FORGES.put(Byte[].class, new JsonWriteForgeByMethod("writeArrayByte"));
        WRITE_ARRAY_FORGES.put(Short[].class, new JsonWriteForgeByMethod("writeArrayShort"));
        WRITE_ARRAY_FORGES.put(Integer[].class, new JsonWriteForgeByMethod("writeArrayInteger"));
        WRITE_ARRAY_FORGES.put(Long[].class, new JsonWriteForgeByMethod("writeArrayLong"));
        WRITE_ARRAY_FORGES.put(Double[].class, new JsonWriteForgeByMethod("writeArrayDouble"));
        WRITE_ARRAY_FORGES.put(Float[].class, new JsonWriteForgeByMethod("writeArrayFloat"));
        WRITE_ARRAY_FORGES.put(char[].class, new JsonWriteForgeByMethod("writeArrayCharPrimitive"));
        WRITE_ARRAY_FORGES.put(boolean[].class, new JsonWriteForgeByMethod("writeArrayBooleanPrimitive"));
        WRITE_ARRAY_FORGES.put(byte[].class, new JsonWriteForgeByMethod("writeArrayBytePrimitive"));
        WRITE_ARRAY_FORGES.put(short[].class, new JsonWriteForgeByMethod("writeArrayShortPrimitive"));
        WRITE_ARRAY_FORGES.put(int[].class, new JsonWriteForgeByMethod("writeArrayIntPrimitive"));
        WRITE_ARRAY_FORGES.put(long[].class, new JsonWriteForgeByMethod("writeArrayLongPrimitive"));
        WRITE_ARRAY_FORGES.put(double[].class, new JsonWriteForgeByMethod("writeArrayDoublePrimitive"));
        WRITE_ARRAY_FORGES.put(float[].class, new JsonWriteForgeByMethod("writeArrayFloatPrimitive"));
        WRITE_ARRAY_FORGES.put(BigDecimal[].class, new JsonWriteForgeByMethod("writeArrayBigDecimal"));
        WRITE_ARRAY_FORGES.put(BigInteger[].class, new JsonWriteForgeByMethod("writeArrayBigInteger"));

        WRITE_ARRAY_FORGES.put(String[][].class, new JsonWriteForgeByMethod("writeArray2DimString"));
        WRITE_ARRAY_FORGES.put(Character[][].class, new JsonWriteForgeByMethod("writeArray2DimCharacter"));
        WRITE_ARRAY_FORGES.put(Boolean[][].class, new JsonWriteForgeByMethod("writeArray2DimBoolean"));
        WRITE_ARRAY_FORGES.put(Byte[][].class, new JsonWriteForgeByMethod("writeArray2DimByte"));
        WRITE_ARRAY_FORGES.put(Short[][].class, new JsonWriteForgeByMethod("writeArray2DimShort"));
        WRITE_ARRAY_FORGES.put(Integer[][].class, new JsonWriteForgeByMethod("writeArray2DimInteger"));
        WRITE_ARRAY_FORGES.put(Long[][].class, new JsonWriteForgeByMethod("writeArray2DimLong"));
        WRITE_ARRAY_FORGES.put(Double[][].class, new JsonWriteForgeByMethod("writeArray2DimDouble"));
        WRITE_ARRAY_FORGES.put(Float[][].class, new JsonWriteForgeByMethod("writeArray2DimFloat"));
        WRITE_ARRAY_FORGES.put(char[][].class, new JsonWriteForgeByMethod("writeArray2DimCharPrimitive"));
        WRITE_ARRAY_FORGES.put(boolean[][].class, new JsonWriteForgeByMethod("writeArray2DimBooleanPrimitive"));
        WRITE_ARRAY_FORGES.put(byte[][].class, new JsonWriteForgeByMethod("writeArray2DimBytePrimitive"));
        WRITE_ARRAY_FORGES.put(short[][].class, new JsonWriteForgeByMethod("writeArray2DimShortPrimitive"));
        WRITE_ARRAY_FORGES.put(int[][].class, new JsonWriteForgeByMethod("writeArray2DimIntPrimitive"));
        WRITE_ARRAY_FORGES.put(long[][].class, new JsonWriteForgeByMethod("writeArray2DimLongPrimitive"));
        WRITE_ARRAY_FORGES.put(double[][].class, new JsonWriteForgeByMethod("writeArray2DimDoublePrimitive"));
        WRITE_ARRAY_FORGES.put(float[][].class, new JsonWriteForgeByMethod("writeArray2DimFloatPrimitive"));
        WRITE_ARRAY_FORGES.put(BigDecimal[][].class, new JsonWriteForgeByMethod("writeArray2DimBigDecimal"));
        WRITE_ARRAY_FORGES.put(BigInteger[][].class, new JsonWriteForgeByMethod("writeArray2DimBigInteger"));
    }

    public static JsonForgeDesc forge(Class type, String fieldName, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        type = JavaClassHelper.getBoxedType(type);
        JsonDelegateForge startObject = null;
        JsonDelegateForge startArray = null;
        JsonEndValueForge end = END_VALUE_FORGES.get(type);
        JsonWriteForge writeForge = WRITE_FORGES.get(type);

        JsonSchemaField fieldAnnotation = findFieldAnnotation(fieldName, annotations);

        if (fieldAnnotation != null && type != null) {
            Class clazz;
            try {
                clazz = services.getClasspathImportServiceCompileTime().resolveClass(fieldAnnotation.adapter(), true);
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve Json schema field adapter class: " + e.getMessage(), e);
            }
            if (!JavaClassHelper.isImplementsInterface(clazz, JsonFieldAdapterString.class)) {
                throw new ExprValidationException("Json schema field adapter class does not implement interface '" + JsonFieldAdapterString.class.getSimpleName());
            }
            if (ConstructorHelper.getRegularConstructor(clazz, new Class[0]) == null) {
                throw new ExprValidationException("Json schema field adapter class '" + clazz.getSimpleName() + "' does not have a default constructor");
            }
            Method writeMethod;
            try {
                writeMethod = MethodResolver.resolveMethod(clazz, "parse", new Class[]{String.class}, true, new boolean[1], new boolean[1]);
            } catch (MethodResolverNoSuchMethodException e) {
                throw new ExprValidationException("Failed to resolve write method of Json schema field adapter class: " + e.getMessage(), e);
            }
            if (!JavaClassHelper.isSubclassOrImplementsInterface(type, writeMethod.getReturnType())) {
                throw new ExprValidationException("Json schema field adapter class '" + clazz.getSimpleName() + "' mismatches the return type of the parse method, expected '" + type.getSimpleName() + "' but found '" + writeMethod.getReturnType().getSimpleName() + "'");
            }
            end = new JsonEndValueForgeProvidedStringAdapter(clazz);
            writeForge = new JsonWriteForgeProvidedStringAdapter(clazz);
        } else if (type == Object.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.class);
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.class);
            end = JsonEndValueForgeJsonValue.INSTANCE;
            writeForge = new JsonWriteForgeByMethod("writeJsonValue");
        } else if (type == Object[].class) {
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.class);
            end = new JsonEndValueForgeCast(type);
            writeForge = new JsonWriteForgeByMethod("writeJsonArray");
        } else if (type == Map.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.class);
            end = new JsonEndValueForgeCast(type);
            writeForge = new JsonWriteForgeByMethod("writeJsonMap");
        } else if (type.isEnum()) {
            end = new JsonEndValueForgeEnum(type);
            writeForge = JsonWriteForgeStringWithToString.INSTANCE;
        } else if (type.isArray()) {
            if (type.getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArrayEnum.class, constant(type.getComponentType()));
                writeForge = new JsonWriteForgeByMethod("writeEnumArray");
            } else if (type.getComponentType().isArray() && type.getComponentType().getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArray2DimEnum.class, constant(type.getComponentType().getComponentType()));
                writeForge = new JsonWriteForgeByMethod("writeEnumArray2Dim");
            } else {
                Class startArrayDelegateClass = START_ARRAY_FORGES.get(type);
                if (startArrayDelegateClass == null) {
                    throw getUnsupported(type, fieldName);
                }
                startArray = new JsonDelegateForgeByClass(startArrayDelegateClass);
                writeForge = WRITE_ARRAY_FORGES.get(type);
            }
            end = new JsonEndValueForgeCast(type);
        }

        if (end == null) {
            throw getUnsupported(type, fieldName);
        }
        if (writeForge == null) {
            throw getUnsupported(type, fieldName);
        }

        return new JsonForgeDesc(startObject, startArray, end, writeForge);
    }

    private static JsonSchemaField findFieldAnnotation(String fieldName, Annotation[] annotations) {
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (!(annotation instanceof JsonSchemaField)) {
                continue;
            }
            JsonSchemaField field = (JsonSchemaField) annotation;
            if (field.name().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private static UnsupportedOperationException getUnsupported(Class type, String fieldName) {
        return new UnsupportedOperationException("Unsupported type '" + type + "' for property '" + fieldName + "' (use @JsonSchemaField to declare additional information)");
    }
}
