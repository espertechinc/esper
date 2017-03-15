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

import com.espertech.esper.epl.expression.core.ExprValidationException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for type widening.
 */
public class TypeWidenerFactory {
    private static final TypeWidenerStringToCharCoercer STRING_TO_CHAR_COERCER = new TypeWidenerStringToCharCoercer();
    public static final TypeWidenerObjectArrayToCollectionCoercer OBJECT_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerObjectArrayToCollectionCoercer();
    private static final TypeWidenerByteArrayToCollectionCoercer BYTE_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerByteArrayToCollectionCoercer();
    private static final TypeWidenerShortArrayToCollectionCoercer SHORT_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerShortArrayToCollectionCoercer();
    private static final TypeWidenerIntArrayToCollectionCoercer INT_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerIntArrayToCollectionCoercer();
    private static final TypeWidenerLongArrayToCollectionCoercer LONG_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerLongArrayToCollectionCoercer();
    private static final TypeWidenerFloatArrayToCollectionCoercer FLOAT_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerFloatArrayToCollectionCoercer();
    private static final TypeWidenerDoubleArrayToCollectionCoercer DOUBLE_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerDoubleArrayToCollectionCoercer();
    private static final TypeWidenerBooleanArrayToCollectionCoercer BOOLEAN_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerBooleanArrayToCollectionCoercer();
    private static final TypeWidenerCharArrayToCollectionCoercer CHAR_ARRAY_TO_COLLECTION_COERCER = new TypeWidenerCharArrayToCollectionCoercer();
    public static final TypeWidenerByteArrayToByteBufferCoercer BYTE_ARRAY_TO_BYTE_BUFFER_COERCER = new TypeWidenerByteArrayToByteBufferCoercer();

    /**
     * Returns the widener.
     *
     * @param columnName            name of column
     * @param columnType            type of column
     * @param writeablePropertyType property type
     * @param writeablePropertyName propery name
     * @param allowObjectArrayToCollectionConversion whether we widen object-array to collection
     * @param customizer customization if any
     * @param engineURI engine URI
     * @param statementName statement name
     * @return type widender
     * @throws ExprValidationException if type validation fails
     */
    public static TypeWidener getCheckPropertyAssignType(String columnName, Class columnType, Class writeablePropertyType, String writeablePropertyName, boolean allowObjectArrayToCollectionConversion, TypeWidenerCustomizer customizer, String statementName, String engineURI)
            throws ExprValidationException {
        Class columnClassBoxed = JavaClassHelper.getBoxedType(columnType);
        Class targetClassBoxed = JavaClassHelper.getBoxedType(writeablePropertyType);

        if (customizer != null) {
            TypeWidener custom = customizer.widenerFor(columnName, columnType, writeablePropertyType, writeablePropertyName, statementName, engineURI);
            if (custom != null) {
                return custom;
            }
        }

        if (columnType == null) {
            if (writeablePropertyType.isPrimitive()) {
                String message = "Invalid assignment of column '" + columnName +
                        "' of null type to event property '" + writeablePropertyName +
                        "' typed as '" + writeablePropertyType.getName() +
                        "', nullable type mismatch";
                throw new ExprValidationException(message);
            }
        } else if (columnClassBoxed != targetClassBoxed) {
            if (columnClassBoxed == String.class && targetClassBoxed == Character.class) {
                return STRING_TO_CHAR_COERCER;
            } else if (allowObjectArrayToCollectionConversion &&
                    columnClassBoxed.isArray() &&
                    !columnClassBoxed.getComponentType().isPrimitive() &&
                    JavaClassHelper.isImplementsInterface(targetClassBoxed, Collection.class)) {
                return OBJECT_ARRAY_TO_COLLECTION_COERCER;
            } else if (!JavaClassHelper.isAssignmentCompatible(columnClassBoxed, targetClassBoxed)) {
                String writablePropName = writeablePropertyType.getName();
                if (writeablePropertyType.isArray()) {
                    writablePropName = writeablePropertyType.getComponentType().getName() + "[]";
                }

                String columnTypeName = columnType.getName();
                if (columnType.isArray()) {
                    columnTypeName = columnType.getComponentType().getName() + "[]";
                }

                String message = "Invalid assignment of column '" + columnName +
                        "' of type '" + columnTypeName +
                        "' to event property '" + writeablePropertyName +
                        "' typed as '" + writablePropName +
                        "', column and parameter types mismatch";
                throw new ExprValidationException(message);
            }

            if (JavaClassHelper.isNumeric(writeablePropertyType)) {
                return new TypeWidenerBoxedNumeric(SimpleNumberCoercerFactory.getCoercer(columnClassBoxed, targetClassBoxed));
            }
        }

        return null;
    }

    public static TypeWidener getArrayToCollectionCoercer(Class componentType) {
        if (!componentType.isPrimitive()) {
            return OBJECT_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == byte.class) {
            return BYTE_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == short.class) {
            return SHORT_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == int.class) {
            return INT_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == long.class) {
            return LONG_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == float.class) {
            return FLOAT_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == double.class) {
            return DOUBLE_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == boolean.class) {
            return BOOLEAN_ARRAY_TO_COLLECTION_COERCER;
        } else if (componentType == char.class) {
            return CHAR_ARRAY_TO_COLLECTION_COERCER;
        }
        throw new IllegalStateException("Unrecognized class " + componentType);
    }

    private static class TypeWidenerByteArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((byte[]) input);
        }
    }

    private static class TypeWidenerShortArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((short[]) input);
        }
    }

    private static class TypeWidenerIntArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((int[]) input);
        }
    }

    private static class TypeWidenerLongArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((long[]) input);
        }
    }

    private static class TypeWidenerFloatArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((float[]) input);
        }
    }

    private static class TypeWidenerDoubleArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((double[]) input);
        }
    }

    private static class TypeWidenerBooleanArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((boolean[]) input);
        }
    }

    private static class TypeWidenerCharArrayToCollectionCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((char[]) input);
        }
    }

    private static class TypeWidenerByteArrayToByteBufferCoercer implements TypeWidener {
        public Object widen(Object input) {
            return input == null ? null : ByteBuffer.wrap((byte[]) input);
        }
    }
}
