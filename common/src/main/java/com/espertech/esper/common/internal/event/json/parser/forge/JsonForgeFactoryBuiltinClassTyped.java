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
import com.espertech.esper.common.internal.event.json.compiletime.JsonApplicationClassDelegateDesc;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class JsonForgeFactoryBuiltinClassTyped {
    private final static Map<Class, JsonEndValueForge> END_VALUE_FORGES = new HashMap<>();
    private final static Map<Class, Class> START_ARRAY_FORGES = new HashMap<>();
    private final static Map<Class, Class> START_COLLECTION_FORGES = new HashMap<>();
    private final static Map<Class, JsonWriteForge> WRITE_FORGES = new HashMap<>();
    private final static Map<Class, JsonWriteForge> WRITE_ARRAY_FORGES = new HashMap<>();
    private final static Map<Class, JsonWriteForge> WRITE_COLLECTION_FORGES = new HashMap<>();

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

        END_VALUE_FORGES.put(UUID.class, JsonEndValueForgeUUID.INSTANCE);
        END_VALUE_FORGES.put(OffsetDateTime.class, JsonEndValueForgeOffsetDateTime.INSTANCE);
        END_VALUE_FORGES.put(LocalDate.class, JsonEndValueForgeLocalDate.INSTANCE);
        END_VALUE_FORGES.put(LocalDateTime.class, JsonEndValueForgeLocalDateTime.INSTANCE);
        END_VALUE_FORGES.put(ZonedDateTime.class, JsonEndValueForgeZonedDateTime.INSTANCE);
        END_VALUE_FORGES.put(URL.class, JsonEndValueForgeURL.INSTANCE);
        END_VALUE_FORGES.put(URI.class, JsonEndValueForgeURI.INSTANCE);

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
        for (Class clazz : new Class[]{UUID.class, OffsetDateTime.class, LocalDate.class, LocalDateTime.class, ZonedDateTime.class,
            URL.class, URI.class}) {
            WRITE_FORGES.put(clazz, JsonWriteForgeStringWithToString.INSTANCE);
        }

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
        START_ARRAY_FORGES.put(UUID[].class, JsonDelegateArrayUUID.class);
        START_ARRAY_FORGES.put(OffsetDateTime[].class, JsonDelegateArrayOffsetDateTime.class);
        START_ARRAY_FORGES.put(LocalDate[].class, JsonDelegateArrayLocalDate.class);
        START_ARRAY_FORGES.put(LocalDateTime[].class, JsonDelegateArrayLocalDateTime.class);
        START_ARRAY_FORGES.put(ZonedDateTime[].class, JsonDelegateArrayZonedDateTime.class);
        START_ARRAY_FORGES.put(URL[].class, JsonDelegateArrayURL.class);
        START_ARRAY_FORGES.put(URI[].class, JsonDelegateArrayURI.class);

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
        START_ARRAY_FORGES.put(UUID[][].class, JsonDelegateArray2DimUUID.class);
        START_ARRAY_FORGES.put(OffsetDateTime[][].class, JsonDelegateArray2DimOffsetDateTime.class);
        START_ARRAY_FORGES.put(LocalDate[][].class, JsonDelegateArray2DimLocalDate.class);
        START_ARRAY_FORGES.put(LocalDateTime[][].class, JsonDelegateArray2DimLocalDateTime.class);
        START_ARRAY_FORGES.put(ZonedDateTime[][].class, JsonDelegateArray2DimZonedDateTime.class);
        START_ARRAY_FORGES.put(URL[][].class, JsonDelegateArray2DimURL.class);
        START_ARRAY_FORGES.put(URI[][].class, JsonDelegateArray2DimURI.class);

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
        for (Class clazz : new Class[]{UUID[].class, OffsetDateTime[].class, LocalDate[].class, LocalDateTime[].class, ZonedDateTime[].class,
            URL[].class, URI[].class}) {
            WRITE_ARRAY_FORGES.put(clazz, new JsonWriteForgeByMethod("writeArrayObjectToString"));
        }

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
        for (Class clazz : new Class[]{UUID[][].class, OffsetDateTime[][].class, LocalDate[][].class, LocalDateTime[][].class, ZonedDateTime[][].class,
            URL[][].class, URI[][].class}) {
            WRITE_ARRAY_FORGES.put(clazz, new JsonWriteForgeByMethod("writeArray2DimObjectToString"));
        }

        START_COLLECTION_FORGES.put(String.class, JsonDelegateCollectionString.class);
        START_COLLECTION_FORGES.put(Character.class, JsonDelegateCollectionCharacter.class);
        START_COLLECTION_FORGES.put(Boolean.class, JsonDelegateCollectionBoolean.class);
        START_COLLECTION_FORGES.put(Byte.class, JsonDelegateCollectionByte.class);
        START_COLLECTION_FORGES.put(Short.class, JsonDelegateCollectionShort.class);
        START_COLLECTION_FORGES.put(Integer.class, JsonDelegateCollectionInteger.class);
        START_COLLECTION_FORGES.put(Long.class, JsonDelegateCollectionLong.class);
        START_COLLECTION_FORGES.put(Double.class, JsonDelegateCollectionDouble.class);
        START_COLLECTION_FORGES.put(Float.class, JsonDelegateCollectionFloat.class);
        START_COLLECTION_FORGES.put(BigDecimal.class, JsonDelegateCollectionBigDecimal.class);
        START_COLLECTION_FORGES.put(BigInteger.class, JsonDelegateCollectionBigInteger.class);
        START_COLLECTION_FORGES.put(UUID.class, JsonDelegateCollectionUUID.class);
        START_COLLECTION_FORGES.put(OffsetDateTime.class, JsonDelegateCollectionOffsetDateTime.class);
        START_COLLECTION_FORGES.put(LocalDate.class, JsonDelegateCollectionLocalDate.class);
        START_COLLECTION_FORGES.put(LocalDateTime.class, JsonDelegateCollectionLocalDateTime.class);
        START_COLLECTION_FORGES.put(ZonedDateTime.class, JsonDelegateCollectionZonedDateTime.class);
        START_COLLECTION_FORGES.put(URL.class, JsonDelegateCollectionURL.class);
        START_COLLECTION_FORGES.put(URI.class, JsonDelegateCollectionURI.class);

        WRITE_COLLECTION_FORGES.put(String.class, new JsonWriteForgeByMethod("writeCollectionString"));
        WRITE_COLLECTION_FORGES.put(Character.class, new JsonWriteForgeByMethod("writeCollectionWToString"));
        WRITE_COLLECTION_FORGES.put(Boolean.class, new JsonWriteForgeByMethod("writeCollectionBoolean"));
        WRITE_COLLECTION_FORGES.put(Byte.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(Short.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(Integer.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(Long.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(Double.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(Float.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(BigDecimal.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(BigInteger.class, new JsonWriteForgeByMethod("writeCollectionNumber"));
        for (Class clazz : new Class[]{UUID.class, OffsetDateTime.class, LocalDate.class, LocalDateTime.class, ZonedDateTime.class,
            URL.class, URI.class}) {
            WRITE_COLLECTION_FORGES.put(clazz, new JsonWriteForgeByMethod("writeCollectionWToString"));
        }
    }

    public static JsonForgeDesc forge(Class type, String fieldName, Field optionalField, Map<Class, JsonApplicationClassDelegateDesc> deepClasses, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        type = JavaClassHelper.getBoxedType(type);
        JsonDelegateForge startObject = null;
        JsonDelegateForge startArray = null;
        JsonEndValueForge end = END_VALUE_FORGES.get(type);
        JsonWriteForge write = WRITE_FORGES.get(type);

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
            if (!ConstructorHelper.hasDefaultConstructor(clazz)) {
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
            write = new JsonWriteForgeProvidedStringAdapter(clazz);
        } else if (type == Object.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.class);
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.class);
            end = JsonEndValueForgeJsonValue.INSTANCE;
            write = new JsonWriteForgeByMethod("writeJsonValue");
        } else if (type == Object[].class) {
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.class);
            end = new JsonEndValueForgeCast(type);
            write = new JsonWriteForgeByMethod("writeJsonArray");
        } else if (type == Map.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.class);
            end = new JsonEndValueForgeCast(type);
            write = new JsonWriteForgeByMethod("writeJsonMap");
        } else if (type.isEnum()) {
            end = new JsonEndValueForgeEnum(type);
            write = JsonWriteForgeStringWithToString.INSTANCE;
        } else if (type.isArray()) {
            if (type.getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArrayEnum.class, constant(type.getComponentType()));
                write = new JsonWriteForgeByMethod("writeEnumArray");
            } else if (type.getComponentType().isArray() && type.getComponentType().getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArray2DimEnum.class, constant(type.getComponentType().getComponentType()));
                write = new JsonWriteForgeByMethod("writeEnumArray2Dim");
            } else {
                Class arrayType = JavaClassHelper.getArrayComponentTypeInnermost(type);
                JsonApplicationClassDelegateDesc classNames = deepClasses.get(arrayType);
                if (classNames != null && JavaClassHelper.getArrayDimensions(arrayType) <= 2) {
                    if (type.getComponentType().isArray()) {
                        startArray = new JsonDelegateForgeWithDelegateFactoryArray2Dim(classNames.getDelegateFactoryClassName(), type.getComponentType());
                        write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeArray2DimAppClass");
                    } else {
                        startArray = new JsonDelegateForgeWithDelegateFactoryArray(classNames.getDelegateFactoryClassName(), arrayType);
                        write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeArrayAppClass");
                    }
                } else {
                    Class startArrayDelegateClass = START_ARRAY_FORGES.get(type);
                    if (startArrayDelegateClass == null) {
                        throw getUnsupported(type, fieldName);
                    }
                    startArray = new JsonDelegateForgeByClass(startArrayDelegateClass);
                    write = WRITE_ARRAY_FORGES.get(type);
                }
            }
            end = new JsonEndValueForgeCast(type);
        } else if (type == List.class) {
            if (optionalField != null) {
                Class genericType = JavaClassHelper.getGenericFieldType(optionalField, true);
                if (genericType == null) {
                    return null;
                }
                end = new JsonEndValueForgeCast(List.class); // we are casting to list
                JsonApplicationClassDelegateDesc classNames = deepClasses.get(genericType);
                if (classNames != null) {
                    startArray = new JsonDelegateForgeWithDelegateFactoryCollection(classNames.getDelegateFactoryClassName());
                    write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeCollectionAppClass");
                } else {
                    if (genericType.isEnum()) {
                        startArray = new JsonDelegateForgeByClass(JsonDelegateCollectionEnum.class, constant(genericType));
                        write = new JsonWriteForgeByMethod("writeEnumCollection");
                    } else {
                        Class startArrayDelegateClass = START_COLLECTION_FORGES.get(genericType);
                        if (startArrayDelegateClass == null) {
                            throw getUnsupported(genericType, fieldName);
                        }
                        startArray = new JsonDelegateForgeByClass(startArrayDelegateClass);
                        write = WRITE_COLLECTION_FORGES.get(genericType);
                    }
                }
            }
        }

        if (end == null) {
            JsonApplicationClassDelegateDesc delegateDesc = deepClasses.get(type);
            if (delegateDesc == null) {
                throw getUnsupported(type, fieldName);
            }
            end = new JsonEndValueForgeCast(type);
            write = new JsonWriteForgeDelegate(delegateDesc.getDelegateFactoryClassName());
            if (optionalField != null && optionalField.getDeclaringClass() == optionalField.getType()) {
                startObject = new JsonDelegateForgeWithDelegateFactorySelf(delegateDesc.getDelegateClassName(), optionalField.getType());
            } else {
                startObject = new JsonDelegateForgeWithDelegateFactory(delegateDesc.getDelegateFactoryClassName());
            }
        }
        if (write == null) {
            throw getUnsupported(type, fieldName);
        }

        return new JsonForgeDesc(fieldName, startObject, startArray, end, write);
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
        return new UnsupportedOperationException("Unsupported type '" + type.getName() + "' for property '" + fieldName + "' (use @JsonSchemaField to declare additional information)");
    }
}
