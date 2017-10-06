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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for conversion/coercion and widening implementations for numbers.
 */
public class SimpleNumberCoercerFactory {

    /**
     * Returns a coercer/widener to BigDecimal for a given type.
     *
     * @param fromType to widen
     * @return widener
     */
    public static SimpleNumberBigDecimalCoercer getCoercerBigDecimal(Class fromType) {
        if (fromType == BigDecimal.class) {
            return SimpleNumberCoercerBigDecNull.INSTANCE;
        }
        if (JavaClassHelper.isFloatingPointClass(fromType)) {
            return SimpleNumberCoercerBigDecDouble.INSTANCE;
        }
        return SimpleNumberCoercerBigDecLong.INSTANCE;
    }

    /**
     * Returns a coercer/widener to BigInteger for a given type.
     *
     * @param fromType to widen
     * @return widener
     */
    public static SimpleNumberBigIntegerCoercer getCoercerBigInteger(Class fromType) {
        if (fromType == BigInteger.class) {
            return SimpleNumberCoercerBigIntNull.INSTANCE;
        }
        return SimpleNumberCoercerBigInt.INSTANCE;
    }

    /**
     * Returns a coercer/widener/narrower to a result number type from a given type.
     *
     * @param fromType        to widen/narrow, can be null to indicate that no shortcut-coercer is used
     * @param resultBoxedType type to widen/narrow to
     * @return widener/narrower
     */
    public static SimpleNumberCoercer getCoercer(Class fromType, Class resultBoxedType) {
        if (fromType == resultBoxedType) {
            return SimpleNumberCoercerNull.INSTANCE;
        }
        if (resultBoxedType == Double.class) {
            return SimpleNumberCoercerDouble.INSTANCE;
        }
        if (resultBoxedType == Long.class) {
            return SimpleNumberCoercerLong.INSTANCE;
        }
        if (resultBoxedType == Float.class) {
            return SimpleNumberCoercerFloat.INSTANCE;
        }
        if (resultBoxedType == Integer.class) {
            return SimpleNumberCoercerInt.INSTANCE;
        }
        if (resultBoxedType == Short.class) {
            return SimpleNumberCoercerShort.INSTANCE;
        }
        if (resultBoxedType == Byte.class) {
            return SimpleNumberCoercerByte.INSTANCE;
        }
        if (resultBoxedType == BigInteger.class) {
            return SimpleNumberCoercerBigInt.INSTANCE;
        }
        if (resultBoxedType == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return SimpleNumberCoercerBigDecDouble.INSTANCE;
            }
            return SimpleNumberCoercerBigDecLong.INSTANCE;
        }
        if (resultBoxedType == Object.class || resultBoxedType == Number.class) {
            return SimpleNumberCoercerNull.INSTANCE;
        }
        throw new IllegalArgumentException("Cannot coerce to number subtype " + resultBoxedType.getName());
    }

    private static class SimpleNumberCoercerNull implements SimpleNumberCoercer {
        public static final SimpleNumberCoercerNull INSTANCE = new SimpleNumberCoercerNull();

        private SimpleNumberCoercerNull() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return value;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return value;
        }
    }

    public static class SimpleNumberCoercerDouble implements SimpleNumberCoercer {
        public static final SimpleNumberCoercerDouble INSTANCE = new SimpleNumberCoercerDouble();

        private SimpleNumberCoercerDouble() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.doubleValue();
        }

        public Class getReturnType() {
            return Double.class;
        }

        public static CodegenExpression codegenDouble(CodegenExpression param, Class type) {
            return codegenCoerceNonNull(double.class, Double.class, "doubleValue", param, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenDouble(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(double.class, Double.class, "doubleValue", param, type, codegenMethodScope, SimpleNumberCoercerDouble.class, codegenClassScope);
        }

        public static CodegenExpression codegenDoubleMayNullBoxedIncludeBig(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueType == BigInteger.class || valueType == BigDecimal.class) {
                return exprDotMethod(value, "doubleValue");
            }
            return codegenCoerceMayNull(double.class, Double.class, "doubleValue", value, valueType, codegenMethodScope, SimpleNumberCoercerDouble.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerLong implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerLong INSTANCE = new SimpleNumberCoercerLong();

        private SimpleNumberCoercerLong() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.longValue();
        }

        public Class getReturnType() {
            return Long.class;
        }

        public static CodegenExpression codegenLong(CodegenExpression param, Class type) {
            return codegenCoerceNonNull(long.class, Long.class, "longValue", param, type);
        }

        public static CodegenExpression codegenLongMayNullBox(CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(long.class, Long.class, "longValue", param, type, codegenMethodScope, SimpleNumberCoercerLong.class, codegenClassScope);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenLong(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenLongMayNullBox(param, type, codegenMethodScope, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerInt implements SimpleNumberCoercer {

        public static final SimpleNumberCoercerInt INSTANCE = new SimpleNumberCoercerInt();

        private SimpleNumberCoercerInt() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.intValue();
        }

        public Class getReturnType() {
            return Integer.class;
        }

        public static CodegenExpression codegenInt(CodegenExpression param, Class type) {
            return codegenCoerceNonNull(int.class, Integer.class, "intValue", param, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenInt(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(int.class, Integer.class, "intValue", param, type, codegenMethodScope, SimpleNumberCoercerInt.class, codegenClassScope);
        }

        public static CodegenExpression coerceCodegenMayNull(CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(int.class, Integer.class, "intValue", param, type, codegenMethodScope, SimpleNumberCoercerInt.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerFloat implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.floatValue();
        }

        public final static SimpleNumberCoercerFloat INSTANCE = new SimpleNumberCoercerFloat();

        private SimpleNumberCoercerFloat() {
        }

        public Class getReturnType() {
            return Float.class;
        }

        public static CodegenExpression codegenFloat(CodegenExpression ref, Class type) {
            return codegenCoerceNonNull(float.class, Float.class, "floatValue", ref, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenFloat(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(float.class, Float.class, "floatValue", value, valueType, codegenMethodScope, SimpleNumberCoercerFloat.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerShort implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerShort INSTANCE = new SimpleNumberCoercerShort();

        private SimpleNumberCoercerShort() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.shortValue();
        }

        public Class getReturnType() {
            return Short.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenShort(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(short.class, Short.class, "shortValue", value, valueType, codegenMethodScope, SimpleNumberCoercerShort.class, codegenClassScope);
        }

        public static CodegenExpression codegenShort(CodegenExpression input, Class inputType) {
            return codegenCoerceNonNull(short.class, Short.class, "shortValue", input, inputType);
        }
    }

    public static class SimpleNumberCoercerByte implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerByte INSTANCE = new SimpleNumberCoercerByte();

        private SimpleNumberCoercerByte() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.byteValue();
        }

        public Class getReturnType() {
            return Byte.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenByte(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(byte.class, Byte.class, "bteValue", value, valueType, codegenMethodScope, SimpleNumberCoercerByte.class, codegenClassScope);
        }

        public static CodegenExpression codegenByte(CodegenExpression input, Class inputType) {
            return codegenCoerceNonNull(byte.class, Byte.class, "byteValue", input, inputType);
        }
    }

    public static class SimpleNumberCoercerBigInt implements SimpleNumberCoercer, SimpleNumberBigIntegerCoercer {
        public final static SimpleNumberCoercerBigInt INSTANCE = new SimpleNumberCoercerBigInt();

        private SimpleNumberCoercerBigInt() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return BigInteger.valueOf(numToCoerce.longValue());
        }

        public BigInteger coerceBoxedBigInt(Number numToCoerce) {
            return BigInteger.valueOf(numToCoerce.longValue());
        }

        public Class getReturnType() {
            return Long.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenBigInt(value, valueType);
        }

        public CodegenExpression coerceBoxedBigIntCodegen(CodegenExpression expr, Class type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueType == null) {
                return value;
            }
            if (valueType.isPrimitive()) {
                return codegenBigInt(value, valueType);
            }
            if (valueType == BigInteger.class) {
                return value;
            }
            CodegenMethodNode method = codegenMethodScope.makeChild(BigInteger.class, SimpleNumberCoercerBigInt.class, codegenClassScope).addParam(valueType, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigInt(ref("value"), valueType));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigInt(CodegenExpression value, Class valueType) {
            if (valueType == BigInteger.class) {
                return value;
            }
            if (valueType == long.class || valueType == Long.class) {
                return staticMethod(BigInteger.class, "valueOf", value);
            }
            if (valueType.isPrimitive()) {
                return staticMethod(BigInteger.class, "valueOf", cast(long.class, value));
            }
            return staticMethod(BigInteger.class, "valueOf", exprDotMethod(value, "longValue"));
        }
    }

    public static class SimpleNumberCoercerBigDecLong implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public final static SimpleNumberCoercerBigDecLong INSTANCE = new SimpleNumberCoercerBigDecLong();

        private SimpleNumberCoercerBigDecLong() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return new BigDecimal(numToCoerce.longValue());
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return new BigDecimal(numToCoerce.longValue());
        }

        public Class getReturnType() {
            return Long.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenBigDec(value, valueType);
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, Class type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueType == null) {
                return value;
            }
            if (valueType.isPrimitive()) {
                return codegenBigDec(value, valueType);
            }
            if (valueType == BigDecimal.class) {
                return value;
            }
            CodegenMethodNode method = codegenMethodScope.makeChild(BigDecimal.class, SimpleNumberCoercerBigDecLong.class, codegenClassScope).addParam(valueType, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigDec(ref("value"), valueType));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigDec(CodegenExpression value, Class valueType) {
            if (valueType == BigDecimal.class) {
                return value;
            }
            if (valueType == long.class || valueType == Long.class) {
                return newInstance(BigDecimal.class, value);
            }
            if (valueType.isPrimitive()) {
                return newInstance(BigDecimal.class, cast(long.class, value));
            }
            return newInstance(BigDecimal.class, exprDotMethod(value, "longValue"));
        }
    }

    public static class SimpleNumberCoercerBigDecDouble implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public final static SimpleNumberCoercerBigDecDouble INSTANCE = new SimpleNumberCoercerBigDecDouble();

        private SimpleNumberCoercerBigDecDouble() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return new BigDecimal(numToCoerce.doubleValue());
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return new BigDecimal(numToCoerce.doubleValue());
        }

        public Class getReturnType() {
            return Double.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return codegenBigDec(value, valueType);
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, Class type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueType == null) {
                return value;
            }
            if (valueType.isPrimitive()) {
                return codegenBigDec(value, valueType);
            }
            if (valueType == BigDecimal.class) {
                return value;
            }
            CodegenMethodNode method = codegenMethodScope.makeChild(BigDecimal.class, SimpleNumberCoercerBigDecDouble.class, codegenClassScope).addParam(valueType, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigDec(ref("value"), valueType));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigDec(CodegenExpression value, Class valueType) {
            if (valueType == BigDecimal.class) {
                return value;
            }
            if (valueType == double.class || valueType == Double.class) {
                return newInstance(BigDecimal.class, value);
            }
            if (valueType.isPrimitive()) {
                return newInstance(BigDecimal.class, cast(double.class, value));
            }
            return newInstance(BigDecimal.class, exprDotMethod(value, "doubleValue"));
        }
    }

    private static class SimpleNumberCoercerBigIntNull implements SimpleNumberCoercer, SimpleNumberBigIntegerCoercer {
        public final static SimpleNumberCoercerBigIntNull INSTANCE = new SimpleNumberCoercerBigIntNull();

        private SimpleNumberCoercerBigIntNull() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public BigInteger coerceBoxedBigInt(Number numToCoerce) {
            return (BigInteger) numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return value;
        }

        public CodegenExpression coerceBoxedBigIntCodegen(CodegenExpression expr, Class type) {
            return expr;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return value;
        }
    }

    private static class SimpleNumberCoercerBigDecNull implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public final static SimpleNumberCoercerBigDecNull INSTANCE = new SimpleNumberCoercerBigDecNull();

        private SimpleNumberCoercerBigDecNull() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return (BigDecimal) numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, Class valueType) {
            return value;
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, Class type) {
            return expr;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, Class valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return value;
        }
    }

    private static CodegenExpression codegenCoerceNonNull(Class primitive, Class boxed, String numberValueMethodName, CodegenExpression param, Class type) {
        if (type == primitive || type == boxed) {
            return param;
        }
        if (type.isPrimitive()) {
            return cast(primitive, param);
        }
        return exprDotMethod(param, numberValueMethodName);
    }

    private static CodegenExpression codegenCoerceMayNull(Class primitive, Class boxed, String numberValueMethodName, CodegenExpression param, Class type, CodegenMethodScope codegenMethodScope, Class generator, CodegenClassScope codegenClassScope) {
        if (type == primitive || type == boxed) {
            return param;
        }
        if (type == null) {
            return constantNull();
        }
        if (type.isPrimitive()) {
            return cast(primitive, param);
        }
        CodegenMethodNode method = codegenMethodScope.makeChild(boxed, generator, codegenClassScope).addParam(type, "value").getBlock()
                .ifRefNullReturnNull("value")
                .methodReturn(exprDotMethod(ref("value"), numberValueMethodName));
        return localMethod(method, param);
    }
}
