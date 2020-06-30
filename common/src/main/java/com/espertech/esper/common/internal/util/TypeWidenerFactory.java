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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
    private static final SimpleTypeCasterFactory.CharacterCaster STRING_TO_CHAR_COERCER = SimpleTypeCasterFactory.CharacterCaster.INSTANCE;
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
    public static TypeWidenerSPI getCheckPropertyAssignType(String columnName, EPType columnType, EPType writeablePropertyType, String writeablePropertyName, boolean allowObjectArrayToCollectionConversion, TypeWidenerCustomizer customizer, String statementName)
            throws TypeWidenerException {
        if (customizer != null) {
            TypeWidenerSPI custom = customizer.widenerFor(columnName, columnType, writeablePropertyType, writeablePropertyName, statementName);
            if (custom != null) {
                return custom;
            }
        }
        if (writeablePropertyType == EPTypeNull.INSTANCE) {
            return null;
        }
        EPTypeClass writtenClass = (EPTypeClass) writeablePropertyType;

        if (columnType == null || columnType == EPTypeNull.INSTANCE) {
            if (writtenClass.getType().isPrimitive()) {
                String message = "Invalid assignment of column '" + columnName +
                        "' of null type to event property '" + writeablePropertyName +
                        "' typed as '" + writeablePropertyType.getTypeName() +
                        "', nullable type mismatch";
                throw new TypeWidenerException(message);
            }
        } else {
            EPTypeClass columnClass = JavaClassHelper.getBoxedType((EPTypeClass) columnType);
            writtenClass = JavaClassHelper.getBoxedType(writtenClass);

            if (columnClass.getType() != writtenClass.getType()) {
                if (columnClass.getType() == String.class && writtenClass.getType() == Character.class) {
                    return STRING_TO_CHAR_COERCER;
                } else if (allowObjectArrayToCollectionConversion &&
                    columnClass.getType().isArray() &&
                    !columnClass.getType().getComponentType().isPrimitive() &&
                    JavaClassHelper.isImplementsInterface(columnClass, Collection.class)) {
                    return OBJECT_ARRAY_TO_COLLECTION_COERCER;
                } else if (!JavaClassHelper.isAssignmentCompatible(columnClass.getType(), writtenClass.getType())) {
                    String writablePropName = writeablePropertyType.getTypeName();
                    if (writtenClass.getType().isArray()) {
                        writablePropName = writtenClass.getType().getComponentType().getName() + "[]";
                    }

                    String columnTypeName = columnType.getTypeName();
                    if (columnClass.getType().isArray()) {
                        columnTypeName = columnClass.getType().getComponentType().getName() + "[]";
                    }

                    String message = "Invalid assignment of column '" + columnName +
                        "' of type '" + columnTypeName +
                        "' to event property '" + writeablePropertyName +
                        "' typed as '" + writablePropName +
                        "', column and parameter types mismatch";
                    throw new TypeWidenerException(message);
                }

                if (JavaClassHelper.isNumeric(writtenClass)) {
                    return new TypeWidenerBoxedNumeric(SimpleNumberCoercerFactory.getCoercer(columnClass, writtenClass));
                }
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
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), TypeWidener.EPTYPE);
        CodegenMethod widen = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), originator, classScope).addParam(EPTypePremade.OBJECT.getEPType(), "input");
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
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerByteArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerShortArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((short[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.SHORTPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerShortArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerIntArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((int[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.INTEGERPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerIntArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerLongArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((long[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.LONGPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerLongArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerFloatArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((float[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.FLOATPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerFloatArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerDoubleArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((double[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.DOUBLEPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerDoubleArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerBooleanArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((boolean[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.BOOLEANPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerBooleanArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerCharArrayToCollectionCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : Arrays.asList((char[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenWidenArrayAsListMayNull(expression, EPTypePremade.CHARPRIMITIVEARRAY.getEPType(), codegenMethodScope, TypeWidenerCharArrayToCollectionCoercer.class, codegenClassScope);
        }
    }

    private static class TypeWidenerByteArrayToByteBufferCoercer implements TypeWidenerSPI {
        public Object widen(Object input) {
            return input == null ? null : ByteBuffer.wrap((byte[]) input);
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.BYTEBUFFER.getEPType(), TypeWidenerByteArrayToByteBufferCoercer.class, codegenClassScope).addParam(EPTypePremade.OBJECT.getEPType(), "input").getBlock()
                    .ifRefNullReturnNull("input")
                    .methodReturn(staticMethod(ByteBuffer.class, "wrap", cast(EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), ref("input"))));
            return localMethodBuild(method).pass(expression).call();
        }
    }

    protected static CodegenExpression codegenWidenArrayAsListMayNull(CodegenExpression expression, EPTypeClass arrayType, CodegenMethodScope codegenMethodScope, Class generator, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.COLLECTION.getEPType(), generator, codegenClassScope).addParam(EPTypePremade.OBJECT.getEPType(), "input").getBlock()
                .ifRefNullReturnNull("input")
                .methodReturn(staticMethod(Arrays.class, "asList", cast(arrayType, ref("input"))));
        return localMethodBuild(method).pass(expression).call();
    }
}
