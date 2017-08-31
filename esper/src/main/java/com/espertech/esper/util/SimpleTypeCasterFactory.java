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
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.base.CodegenMethodNode;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

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
    public static SimpleTypeCaster getCaster(Class fromType, Class targetType) {
        if (fromType == targetType) {
            return new NullCaster();
        }

        targetType = JavaClassHelper.getBoxedType(targetType);
        if (targetType == Integer.class) {
            return new IntCaster();
        } else if (targetType == Long.class) {
            return new LongCaster();
        } else if (targetType == Double.class) {
            return new DoubleCaster();
        } else if (targetType == Float.class) {
            return new FloatCaster();
        } else if (targetType == Short.class) {
            return new ShortCaster();
        } else if (targetType == Byte.class) {
            return new ByteCaster();
        } else if ((targetType == Character.class) && (fromType == String.class)) {
            return new CharacterCaster();
        } else if (targetType == BigInteger.class) {
            return new BigIntCaster();
        } else if (targetType == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return new BigDecDoubleCaster();
            }
            return new BigDecLongCaster();
        } else {
            return new SimpleTypeCasterAnyType(targetType);
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class DoubleCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).doubleValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "doubleValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class FloatCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).floatValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerFloat.codegenFloat(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "floatValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class LongCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).longValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "longValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class IntCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).intValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "intValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ShortCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).shortValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerShort.codegenShort(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "shortValue");
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ByteCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).byteValue();
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerByte.codegenByte(input, inputType);
            }
            return exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "byteValue");
        }
    }

    /**
     * Cast implementation for char values.
     */
    public static class CharacterCaster implements SimpleTypeCaster, TypeWidener {
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
            return codegen(expression, Object.class, codegenMethodScope, codegenClassScope);
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenMethodNode method = codegenMethodScope.makeChild(Character.class, CharacterCaster.class, codegenClassScope).addParam(Object.class, "object").getBlock()
                    .declareVar(String.class, "value", exprDotMethod(ref("object"), "toString"))
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
        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return BigInteger.valueOf(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigInt.codegenBigInt(input, inputType);
            }
            return staticMethod(BigInteger.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "longValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecLongCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigDecLong.codegenBigDec(input, inputType);
            }
            return staticMethod(BigDecimal.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "longValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecDoubleCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            double value = ((Number) object).doubleValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return SimpleNumberCoercerFactory.SimpleNumberCoercerBigDecDouble.codegenBigDec(input, inputType);
            }
            return staticMethod(BigDecimal.class, "valueOf", exprDotMethod(CodegenExpressionBuilder.cast(Number.class, input), "doubleValue"));
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class NullCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return object;
        }

        public boolean isNumericCast() {
            return false;
        }

        public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            return input;
        }
    }
}
