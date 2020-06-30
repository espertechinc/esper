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

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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
    public static SimpleNumberBigDecimalCoercer getCoercerBigDecimal(EPTypeClass fromType) {
        if (fromType.getType() == BigDecimal.class) {
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
    public static SimpleNumberBigIntegerCoercer getCoercerBigInteger(EPTypeClass fromType) {
        if (fromType.getType() == BigInteger.class) {
            return SimpleNumberCoercerBigIntNull.INSTANCE;
        }
        return SimpleNumberCoercerBigInt.INSTANCE;
    }

    /**
     * Returns a coercer/widener/narrower to a result number type from a given type.
     *
     * @param fromType        to widen/narrow, can be null to indicate that no shortcut-coercer is used
     * @param resultBoxed type to widen/narrow to
     * @return widener/narrower
     */
    public static SimpleNumberCoercer getCoercer(EPType fromType, EPTypeClass resultBoxed) {
        if (fromType != null) {
            if (fromType.equals(resultBoxed)) {
                return SimpleNumberCoercerNull.INSTANCE;
            }
        }
        if (fromType instanceof EPTypeClass) {
            if (resultBoxed.getType() == ((EPTypeClass) fromType).getType()) {
                return SimpleNumberCoercerNull.INSTANCE;
            }
        }
        Class resultBoxedType = resultBoxed.getType();
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.NUMBER.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return value;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.DOUBLEBOXED.getEPType();
        }

        public static CodegenExpression codegenDouble(CodegenExpression param, EPType type) {
            return codegenCoerceNonNull(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), EPTypePremade.DOUBLEBOXED.getEPType(), "doubleValue", param, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenDouble(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), EPTypePremade.DOUBLEBOXED.getEPType(), "doubleValue", param, valueTypeMustNumeric, codegenMethodScope, SimpleNumberCoercerDouble.class, codegenClassScope);
        }

        public static CodegenExpression codegenDoubleMayNullBoxedIncludeBig(CodegenExpression value, EPTypeClass valueType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueType.getType() == BigInteger.class || valueType.getType() == BigDecimal.class) {
                return exprDotMethod(value, "doubleValue");
            }
            return codegenCoerceMayNull(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), EPTypePremade.DOUBLEBOXED.getEPType(), "doubleValue", value, valueType, codegenMethodScope, SimpleNumberCoercerDouble.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerLong implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerLong INSTANCE = new SimpleNumberCoercerLong();

        private SimpleNumberCoercerLong() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.longValue();
        }

        public EPTypeClass getReturnType() {
            return EPTypePremade.LONGBOXED.getEPType();
        }

        public static CodegenExpression codegenLong(CodegenExpression param, EPTypeClass type) {
            return codegenCoerceNonNull(EPTypePremade.LONGPRIMITIVE.getEPType(), EPTypePremade.LONGBOXED.getEPType(), "longValue", param, type);
        }

        public static CodegenExpression codegenLongMayNullBox(CodegenExpression param, EPType type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.LONGPRIMITIVE.getEPType(), EPTypePremade.LONGBOXED.getEPType(), "longValue", param, type, codegenMethodScope, SimpleNumberCoercerLong.class, codegenClassScope);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenLong(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenLongMayNullBox(param, valueTypeMustNumeric, codegenMethodScope, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerInt implements SimpleNumberCoercer {

        public static final SimpleNumberCoercerInt INSTANCE = new SimpleNumberCoercerInt();

        private SimpleNumberCoercerInt() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.intValue();
        }

        public EPTypeClass getReturnType() {
            return EPTypePremade.INTEGERBOXED.getEPType();
        }

        public static CodegenExpression codegenInt(CodegenExpression param, EPTypeClass type) {
            return codegenCoerceNonNull(EPTypePremade.INTEGERPRIMITIVE.getEPType(), EPTypePremade.INTEGERBOXED.getEPType(), "intValue", param, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenInt(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression param, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.INTEGERPRIMITIVE.getEPType(), EPTypePremade.INTEGERBOXED.getEPType(), "intValue", param, valueTypeMustNumeric, codegenMethodScope, SimpleNumberCoercerInt.class, codegenClassScope);
        }

        public static CodegenExpression coerceCodegenMayNull(CodegenExpression param, EPTypeClass type, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.INTEGERPRIMITIVE.getEPType(), EPTypePremade.INTEGERBOXED.getEPType(), "intValue", param, type, codegenMethodScope, SimpleNumberCoercerInt.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerFloat implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.floatValue();
        }

        public final static SimpleNumberCoercerFloat INSTANCE = new SimpleNumberCoercerFloat();

        private SimpleNumberCoercerFloat() {
        }

        public EPTypeClass getReturnType() {
            return EPTypePremade.FLOATBOXED.getEPType();
        }

        public static CodegenExpression codegenFloat(CodegenExpression ref, EPTypeClass type) {
            return codegenCoerceNonNull(EPTypePremade.FLOATPRIMITIVE.getEPType(), EPTypePremade.FLOATBOXED.getEPType(), "floatValue", ref, type);
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenFloat(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.FLOATPRIMITIVE.getEPType(), EPTypePremade.FLOATBOXED.getEPType(), "floatValue", value, valueTypeMustNumeric, codegenMethodScope, SimpleNumberCoercerFloat.class, codegenClassScope);
        }
    }

    public static class SimpleNumberCoercerShort implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerShort INSTANCE = new SimpleNumberCoercerShort();

        private SimpleNumberCoercerShort() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.shortValue();
        }

        public EPTypeClass getReturnType() {
            return EPTypePremade.SHORTBOXED.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenShort(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.SHORTPRIMITIVE.getEPType(), EPTypePremade.SHORTBOXED.getEPType(), "shortValue", value, valueTypeMustNumeric, codegenMethodScope, SimpleNumberCoercerShort.class, codegenClassScope);
        }

        public static CodegenExpression codegenShort(CodegenExpression input, EPTypeClass inputType) {
            return codegenCoerceNonNull(EPTypePremade.SHORTPRIMITIVE.getEPType(), EPTypePremade.SHORTBOXED.getEPType(), "shortValue", input, inputType);
        }
    }

    public static class SimpleNumberCoercerByte implements SimpleNumberCoercer {
        public final static SimpleNumberCoercerByte INSTANCE = new SimpleNumberCoercerByte();

        private SimpleNumberCoercerByte() {
        }

        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.byteValue();
        }

        public EPTypeClass getReturnType() {
            return EPTypePremade.BYTEBOXED.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenByte(value, valueType);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegenCoerceMayNull(EPTypePremade.BYTEPRIMITIVE.getEPType(), EPTypePremade.BYTEBOXED.getEPType(), "bteValue", value, valueTypeMustNumeric, codegenMethodScope, SimpleNumberCoercerByte.class, codegenClassScope);
        }

        public static CodegenExpression codegenByte(CodegenExpression input, EPTypeClass inputType) {
            return codegenCoerceNonNull(EPTypePremade.BYTEPRIMITIVE.getEPType(), EPTypePremade.BYTEBOXED.getEPType(), "byteValue", input, inputType);
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.LONGBOXED.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenBigInt(value, valueType);
        }

        public CodegenExpression coerceBoxedBigIntCodegen(CodegenExpression expr, EPTypeClass type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueTypeMustNumeric == null || valueTypeMustNumeric == EPTypeNull.INSTANCE) {
                return value;
            }
            EPTypeClass clazz = (EPTypeClass) valueTypeMustNumeric;
            if (clazz.getType().isPrimitive()) {
                return codegenBigInt(value, clazz);
            }
            if (clazz.getType() == BigInteger.class) {
                return value;
            }
            CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.BIGINTEGER.getEPType(), SimpleNumberCoercerBigInt.class, codegenClassScope).addParam(clazz, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigInt(ref("value"), clazz));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigInt(CodegenExpression value, EPTypeClass valueType) {
            if (valueType.getType() == BigInteger.class) {
                return value;
            }
            if (valueType.getType() == long.class || valueType.getType() == Long.class) {
                return staticMethod(BigInteger.class, "valueOf", value);
            }
            if (valueType.getType().isPrimitive()) {
                return staticMethod(BigInteger.class, "valueOf", cast(EPTypePremade.LONGPRIMITIVE.getEPType(), value));
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.LONGBOXED.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenBigDec(value, valueType);
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, EPTypeClass type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueTypeMustNumeric == null || valueTypeMustNumeric == EPTypeNull.INSTANCE) {
                return value;
            }
            EPTypeClass clazz = (EPTypeClass) valueTypeMustNumeric;
            if (clazz.getType().isPrimitive()) {
                return codegenBigDec(value, clazz);
            }
            if (clazz.getType() == BigDecimal.class) {
                return value;
            }
            CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.BIGDECIMAL.getEPType(), SimpleNumberCoercerBigDecLong.class, codegenClassScope).addParam(clazz, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigDec(ref("value"), clazz));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigDec(CodegenExpression value, EPTypeClass valueType) {
            if (valueType.getType() == BigDecimal.class) {
                return value;
            }
            if (valueType.getType() == long.class || valueType.getType() == Long.class) {
                return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), value);
            }
            if (valueType.getType().isPrimitive()) {
                return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), cast(EPTypePremade.LONGPRIMITIVE.getEPType(), value));
            }
            return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), exprDotMethod(value, "longValue"));
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.DOUBLEBOXED.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return codegenBigDec(value, valueType);
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, EPTypeClass type) {
            return coerceCodegen(expr, type);
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (valueTypeMustNumeric == null || valueTypeMustNumeric == EPTypeNull.INSTANCE) {
                return value;
            }
            EPTypeClass clazz = (EPTypeClass) valueTypeMustNumeric;
            if (clazz.getType().isPrimitive()) {
                return codegenBigDec(value, clazz);
            }
            if (clazz.getType() == BigDecimal.class) {
                return value;
            }
            CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.BIGDECIMAL.getEPType(), SimpleNumberCoercerBigDecDouble.class, codegenClassScope).addParam(clazz, "value").getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(codegenBigDec(ref("value"), clazz));
            return localMethod(method, value);
        }

        public static CodegenExpression codegenBigDec(CodegenExpression value, EPTypeClass valueType) {
            if (valueType.getType() == BigDecimal.class) {
                return value;
            }
            if (valueType.getType() == double.class || valueType.getType() == Double.class) {
                return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), value);
            }
            if (valueType.getType().isPrimitive()) {
                return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), cast(EPTypePremade.DOUBLEBOXED.getEPType(), value));
            }
            return newInstance(EPTypePremade.BIGDECIMAL.getEPType(), exprDotMethod(value, "doubleValue"));
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.NUMBER.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return value;
        }

        public CodegenExpression coerceBoxedBigIntCodegen(CodegenExpression expr, EPTypeClass type) {
            return expr;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
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

        public EPTypeClass getReturnType() {
            return EPTypePremade.NUMBER.getEPType();
        }

        public CodegenExpression coerceCodegen(CodegenExpression value, EPTypeClass valueType) {
            return value;
        }

        public CodegenExpression coerceBoxedBigDecCodegen(CodegenExpression expr, EPTypeClass type) {
            return expr;
        }

        public CodegenExpression coerceCodegenMayNullBoxed(CodegenExpression value, EPType valueTypeMustNumeric, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return value;
        }
    }

    private static CodegenExpression codegenCoerceNonNull(EPTypeClass primitive, EPTypeClass boxed, String numberValueMethodName, CodegenExpression param, EPType type) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass clazz = (EPTypeClass) type;
        if (clazz.equals(primitive) || clazz.equals(boxed)) {
            return param;
        }
        if (clazz.getType().isPrimitive()) {
            return cast(primitive, param);
        }
        return exprDotMethod(param, numberValueMethodName);
    }

    private static CodegenExpression codegenCoerceMayNull(EPTypeClass primitive, EPTypeClass boxed, String numberValueMethodName, CodegenExpression param, EPType type, CodegenMethodScope codegenMethodScope, Class generator, CodegenClassScope codegenClassScope) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass clazz = (EPTypeClass) type;
        if (clazz.equals(primitive) || clazz.equals(boxed)) {
            return param;
        }
        if (clazz.getType().isPrimitive()) {
            return cast(primitive, param);
        }
        CodegenMethod method = codegenMethodScope.makeChild(boxed, generator, codegenClassScope).addParam(clazz, "value").getBlock()
                .ifRefNullReturnNull("value")
                .methodReturn(exprDotMethod(ref("value"), numberValueMethodName));
        return localMethod(method, param);
    }
}
