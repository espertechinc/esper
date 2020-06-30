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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for casters, which take an object and safely cast to a given type, performing coercion or dropping
 * precision if required.
 */
public class SimpleTypeCasterFactory {
    /**
     * Returns a caster that casts to a target type.
     *
     * @param fromType   can be null, if not known
     * @param targetType to cast to
     * @return caster for casting objects to the required type
     */
    public static SimpleTypeCaster getCaster(EPType fromType, EPTypeClass targetType) {
        return getCaster(fromType == null || fromType == EPTypeNull.INSTANCE ? null : ((EPTypeClass) fromType).getType(), targetType);
    }

    /**
     * Returns a caster that casts to a target type.
     *
     * @param fromType   can be null, if not known
     * @param targetType to cast to
     * @return caster for casting objects to the required type
     */
    public static SimpleTypeCaster getCaster(Class fromType, EPTypeClass targetType) {
        if (fromType == targetType.getType()) {
            return NullCaster.INSTANCE;
        }

        targetType = JavaClassHelper.getBoxedType(targetType);
        if (targetType.getType() == Integer.class) {
            return IntCaster.INSTANCE;
        } else if (targetType.getType() == Long.class) {
            return LongCaster.INSTANCE;
        } else if (targetType.getType() == Double.class) {
            return DoubleCaster.INSTANCE;
        } else if (targetType.getType() == Float.class) {
            return FloatCaster.INSTANCE;
        } else if (targetType.getType() == Short.class) {
            return ShortCaster.INSTANCE;
        } else if (targetType.getType() == Byte.class) {
            return ByteCaster.INSTANCE;
        } else if ((targetType.getType() == Character.class) && (fromType == String.class)) {
            return CharacterCaster.INSTANCE;
        } else if (targetType.getType() == BigInteger.class) {
            return BigIntCaster.INSTANCE;
        } else if (targetType.getType() == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return BigDecDoubleCaster.INSTANCE;
            }
            return BigDecLongCaster.INSTANCE;
        } else {
            return new SimpleTypeCasterAnyType(targetType);
        }
    }

    private static boolean isPrimitiveOrImplementsNumber(EPTypeClass typeClass) {
        return typeClass.getType().isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(typeClass, EPTypePremade.NUMBER.getEPType());
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class DoubleCaster implements SimpleTypeCaster {
        public final static DoubleCaster INSTANCE = new DoubleCaster();

        private DoubleCaster() {
        }

        public Object cast(Object object) {
            return ((Number) object).doubleValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "doubleValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class FloatCaster implements SimpleTypeCaster {
        public final static FloatCaster INSTANCE = new FloatCaster();

        private FloatCaster() {
        }

        public Object cast(Object object) {
            return ((Number) object).floatValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerFloat.codegenFloat(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "floatValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class LongCaster implements SimpleTypeCaster {
        public final static LongCaster INSTANCE = new LongCaster();

        private LongCaster() {        }

        public Object cast(Object object) {
            return ((Number) object).longValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "longValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class IntCaster implements SimpleTypeCaster {
        public final static IntCaster INSTANCE = new IntCaster();

        private IntCaster() {        }

        public Object cast(Object object) {
            return ((Number) object).intValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "intValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ShortCaster implements SimpleTypeCaster {
        public final static ShortCaster INSTANCE = new ShortCaster();

        private ShortCaster() {        }

        public Object cast(Object object) {
            return ((Number) object).shortValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerShort.codegenShort(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "shortValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ByteCaster implements SimpleTypeCaster {
        public final static ByteCaster INSTANCE = new ByteCaster();

        private ByteCaster() {
        }

        public Object cast(Object object) {
            return ((Number) object).byteValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerByte.codegenByte(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "byteValue");
        }
    }

    /**
     * Cast implementation for char values.
     */
    public static class CharacterCaster implements SimpleTypeCaster, TypeWidenerSPI {
        public final static CharacterCaster INSTANCE = new CharacterCaster();

        private CharacterCaster() {
        }

        public Object cast(Object object) {
            String value = object.toString();
            if (value.length() == 0) {
                return null;
            }
            return value.charAt(0);
        }

        public Object widen(Object input) {
            return cast(input);
        }

        public boolean isNumericCast() {
            return false;
        }

        public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return codegen(expression, EPTypePremade.OBJECT.getEPType(), codegenMethodScope, codegenClassScope);
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.CHARBOXED.getEPType(), CharacterCaster.class, codegenClassScope).addParam(EPTypePremade.OBJECT.getEPType(), "object").getBlock()
                .declareVar(EPTypePremade.STRING.getEPType(), "value", exprDotMethod(ref("object"), "toString"))
                .ifCondition(equalsIdentity(exprDotMethod(ref("value"), "length"), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(exprDotMethod(ref("value"), "charAt", constant(0)));
            return localMethodBuild(method).pass(input).call();
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigIntCaster implements SimpleTypeCaster {
        public final static BigIntCaster INSTANCE = new BigIntCaster();

        private BigIntCaster() {
        }

        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return BigInteger.valueOf(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigInt.codegenBigInt(input, inputType);
            }
            return staticMethod(BigInteger.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "longValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecLongCaster implements SimpleTypeCaster {
        public final static BigDecLongCaster INSTANCE = new BigDecLongCaster();

        private BigDecLongCaster() {
        }

        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigDecLong.codegenBigDec(input, inputType);
            }
            return staticMethod(BigDecimal.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "longValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecDoubleCaster implements SimpleTypeCaster {
        public final static BigDecDoubleCaster INSTANCE = new BigDecDoubleCaster();

        private BigDecDoubleCaster() {
        }

        public Object cast(Object object) {
            double value = ((Number) object).doubleValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (isPrimitiveOrImplementsNumber(inputType)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigDecDouble.codegenBigDec(input, inputType);
            }
            return staticMethod(BigDecimal.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(EPTypePremade.NUMBER.getEPType(), input), "doubleValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class NullCaster implements SimpleTypeCaster {
        public final static NullCaster INSTANCE = new NullCaster();

        private NullCaster() {
        }

        public Object cast(Object object) {
            return object;
        }

        public boolean isNumericCast() {
            return false;
        }

        public CodegenExpression codegen(CodegenExpression input, EPTypeClass inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return input;
        }
    }
}
