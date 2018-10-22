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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for type widening.
 */
public class TypeWidenerFactory {
    private static final SimpleTypeCasterFactory.CharacterCaster STRING_TO_CHAR_COERCER = new SimpleTypeCasterFactory.CharacterCaster();
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
     * @param columnName                             name of column
     * @param columnType                             type of column
     * @param writeablePropertyType                  property type
     * @param writeablePropertyName                  propery name
     * @param allowObjectArrayToCollectionConversion whether we widen object-array to collection
     * @param customizer                             customization if any
     * @param statementName                          statement name
     * @return type widender
     * @throws TypeWidenerException if type validation fails
     */
    public static TypeWidenerSPI getCheckPropertyAssignType(String columnName, Class columnType, Class writeablePropertyType, String writeablePropertyName, boolean allowObjectArrayToCollectionConversion, TypeWidenerCustomizer customizer, String statementName)
            throws TypeWidenerException {
        Class columnClassBoxed = JavaClassHelper.getBoxedType(columnType);
        Class targetClassBoxed = JavaClassHelper.getBoxedType(writeablePropertyType);

        if (customizer != null) {
            TypeWidenerSPI custom = customizer.widenerFor(columnName, columnType, writeablePropertyType, writeablePropertyName, statementName);
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
                throw new TypeWidenerException(message);
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
                throw new TypeWidenerException(message);
            }

            if (JavaClassHelper.isNumeric(writeablePropertyType)) {
                return new TypeWidenerBoxedNumeric(SimpleNumberCoercerFactory.getCoercer(columnClassBoxed, targetClassBoxed));
            }
        }

        return null;
    }

    public static TypeWidenerSPI getArrayToCollectionCoercer(Class componentType) {
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

    public static CodegenExpression codegenWidener(TypeWidenerSPI widener, CodegenMethod method, Class originator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), TypeWidener.class);
        CodegenMethod widen = CodegenMethod.makeParentNode(Object.class, originator, classScope).addParam(Object.class, "input");
        anonymousClass.addMethod("widen", widen);
        CodegenExpression widenResult = widener.widenCodegen(ref("input"), method, classScope);
        widen.getBlock().methodReturn(widenResult);
        return anonymousClass;
    }

    private static class TypeWidenerByteArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((byte[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, byte[].class, codegenMethodScope, TypeWidenerByteArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerShortArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((short[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, short[].class, codegenMethodScope, TypeWidenerShortArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerIntArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((int[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, int[].class, codegenMethodScope, TypeWidenerIntArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerLongArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((long[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, long[].class, codegenMethodScope, TypeWidenerLongArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerFloatArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((float[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, float[].class, codegenMethodScope, TypeWidenerFloatArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerDoubleArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((double[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, double[].class, codegenMethodScope, TypeWidenerDoubleArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerBooleanArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((boolean[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, boolean[].class, codegenMethodScope, TypeWidenerBooleanArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerCharArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((char[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, char[].class, codegenMethodScope, TypeWidenerCharArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerByteArrayToByteBufferCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : ByteBuffer.wrap((byte[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(ByteBuffer.class, TypeWidenerByteArrayToByteBufferCoercer.class, codegenClassScope).addParam(Object.class, "input").getBlock()
                    .ifRefNullReturnNull("input")
                    .methodReturn(staticMethod(ByteBuffer.class, "wrap", cast(byte[].class, ref("input"))));
            return localMethodBuild(method).pass(expression).call();
        }
    }

    protected static CodegenExpression codegenWidenArrayAsListMayNull(CodegenExpression expression, Class arrayType, CodegenMethodScope codegenMethodScope, Class generator, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Collection.class, generator, codegenClassScope).addParam(Object.class, "input").getBlock()
                .ifRefNullReturnNull("input")
                .methodReturn(staticMethod(Arrays.class, "asList", cast(arrayType, ref("input"))));
        return localMethodBuild(method).pass(expression).call();
    }
}
