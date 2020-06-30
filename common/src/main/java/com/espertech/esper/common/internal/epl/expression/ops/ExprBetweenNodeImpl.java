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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberBigDecimalCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberBigIntegerCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;

/**
 * Represents the between-clause function in an expression tree.
 */
public class ExprBetweenNodeImpl extends ExprNodeBase implements ExprBetweenNode {
    private final boolean isLowEndpointIncluded;
    private final boolean isHighEndpointIncluded;
    private final boolean isNotBetween;

    private transient ExprBetweenNodeForge forge;

    /**
     * Ctor.
     *
     * @param lowEndpointIncluded  is true for the regular 'between' or false for "val in (a:b)" (open range), or
     *                             false if the endpoint is not included
     * @param highEndpointIncluded indicates whether the high endpoint is included
     * @param notBetween           is true for 'not between' or 'not in (a:b), or false for a regular between
     */
    public ExprBetweenNodeImpl(boolean lowEndpointIncluded, boolean highEndpointIncluded, boolean notBetween) {
        isLowEndpointIncluded = lowEndpointIncluded;
        isHighEndpointIncluded = highEndpointIncluded;
        isNotBetween = notBetween;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return this.forge;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns true if the low endpoint is included, false if not
     *
     * @return indicator if endppoint is included
     */
    public boolean isLowEndpointIncluded() {
        return isLowEndpointIncluded;
    }

    /**
     * Returns true if the high endpoint is included, false if not
     *
     * @return indicator if endppoint is included
     */
    public boolean isHighEndpointIncluded() {
        return isHighEndpointIncluded;
    }

    /**
     * Returns true for inverted range, or false for regular (openn/close/half-open/half-closed) ranges.
     *
     * @return true for not betwene, false for between
     */
    public boolean isNotBetween() {
        return isNotBetween;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 3) {
            throw new ExprValidationException("The Between operator requires exactly 3 child expressions");
        }

        // Must be either numeric or string
        ExprForge[] forges = ExprNodeUtilityQuery.getForges(this.getChildNodes());
        ExprForge evalForge = forges[0];
        EPType evalType = evalForge.getEvaluationType();
        if (evalType == null || evalType == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Null value not allowed in between-clause");
        }

        EPTypeClass evalTypeClass = JavaClassHelper.getBoxedType((EPTypeClass) evalType);
        ExprForge startForge = forges[1];
        EPType startType = startForge.getEvaluationType();
        ExprForge endForge = forges[2];
        EPType endType = endForge.getEvaluationType();

        EPTypeClass compareType;
        boolean isAlwaysFalse = false;
        ExprBetweenComp computer = null;
        if (startType == null || startType == EPTypeNull.INSTANCE || endType == null || endType == EPTypeNull.INSTANCE) {
            isAlwaysFalse = true;
        } else {
            EPTypeClass startTypeClass = (EPTypeClass) startType;
            EPTypeClass endTypeClass = (EPTypeClass) endType;

            if (evalTypeClass.getType() != String.class || startTypeClass.getType() != String.class || endTypeClass.getType() != String.class) {
                ExprNodeUtilityValidate.validateReturnsNumeric(evalForge);
                ExprNodeUtilityValidate.validateReturnsNumeric(startForge);
                ExprNodeUtilityValidate.validateReturnsNumeric(endForge);
            }

            EPTypeClass intermedType = JavaClassHelper.getCompareToCoercionType(evalTypeClass, startTypeClass);
            compareType = JavaClassHelper.getCompareToCoercionType(intermedType, endTypeClass);
            computer = makeComputer(compareType, evalTypeClass, startTypeClass, endTypeClass);
        }
        forge = new ExprBetweenNodeForge(this, computer, isAlwaysFalse);
        return null;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprBetweenNodeImpl)) {
            return false;
        }

        ExprBetweenNodeImpl other = (ExprBetweenNodeImpl) node;
        return other.isNotBetween == this.isNotBetween;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        Iterator<ExprNode> it = Arrays.asList(this.getChildNodes()).iterator();
        if (isLowEndpointIncluded && isHighEndpointIncluded) {
            it.next().toEPL(writer, getPrecedence(), flags);
            if (isNotBetween) {
                writer.append(" not between ");
            } else {
                writer.append(" between ");
            }

            it.next().toEPL(writer, getPrecedence(), flags);
            writer.append(" and ");
            it.next().toEPL(writer, getPrecedence(), flags);
        } else {
            it.next().toEPL(writer, getPrecedence(), flags);
            writer.write(" in ");
            if (isLowEndpointIncluded) {
                writer.write('[');
            } else {
                writer.write('(');
            }
            it.next().toEPL(writer, getPrecedence(), flags);
            writer.write(':');
            it.next().toEPL(writer, getPrecedence(), flags);
            if (isHighEndpointIncluded) {
                writer.write(']');
            } else {
                writer.write(')');
            }
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    private ExprBetweenComp makeComputer(EPTypeClass compareType, EPTypeClass valueType, EPTypeClass lowType, EPTypeClass highType) {
        ExprBetweenComp computer;

        if (compareType.getType() == String.class) {
            computer = new ExprBetweenCompString(isLowEndpointIncluded, isHighEndpointIncluded);
        } else if (compareType.getType() == BigDecimal.class) {
            computer = new ExprBetweenCompBigDecimal(isLowEndpointIncluded, isHighEndpointIncluded, valueType, lowType, highType);
        } else if (compareType.getType() == BigInteger.class) {
            computer = new ExprBetweenCompBigInteger(isLowEndpointIncluded, isHighEndpointIncluded, valueType, lowType, highType);
        } else if (compareType.getType() == Long.class) {
            computer = new ExprBetweenCompLong(isLowEndpointIncluded, isHighEndpointIncluded);
        } else {
            computer = new ExprBetweenCompDouble(isLowEndpointIncluded, isHighEndpointIncluded);
        }
        return computer;
    }

    protected interface ExprBetweenComp {
        public boolean isBetween(Object value, Object lower, Object upper);

        CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);
    }

    protected static class ExprBetweenCompString implements ExprBetweenComp {
        private boolean isLowIncluded;
        private boolean isHighIncluded;

        public ExprBetweenCompString(boolean lowIncluded, boolean isHighIncluded) {
            this.isLowIncluded = lowIncluded;
            this.isHighIncluded = isHighIncluded;
        }

        public boolean isBetween(Object value, Object lower, Object upper) {
            if ((value == null) || (lower == null) || ((upper == null))) {
                return false;
            }
            String valueStr = (String) value;
            String lowerStr = (String) lower;
            String upperStr = (String) upper;

            if (upperStr.compareTo(lowerStr) < 0) {
                String temp = upperStr;
                upperStr = lowerStr;
                lowerStr = temp;
            }

            if (valueStr.compareTo(lowerStr) < 0) {
                return false;
            }
            if (valueStr.compareTo(upperStr) > 0) {
                return false;
            }
            if (!isLowIncluded) {
                if (valueStr.equals(lowerStr)) {
                    return false;
                }
            }
            if (!isHighIncluded) {
                if (valueStr.equals(upperStr)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isEqualsEndpoint(Object value, Object endpoint) {
            return value.equals(endpoint);
        }

        public CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprBetweenCompString.class, codegenClassScope).addParam(EPTypePremade.STRING.getEPType(), "value").addParam(EPTypePremade.STRING.getEPType(), "lower").addParam(EPTypePremade.STRING.getEPType(), "upper").getBlock()
                    .ifCondition(relational(exprDotMethod(ref("upper"), "compareTo", ref("lower")), LT, constant(0)))
                    .declareVar(EPTypePremade.STRING.getEPType(), "temp", ref("upper"))
                    .assignRef("upper", ref("lower"))
                    .assignRef("lower", ref("temp"))
                    .blockEnd()
                    .ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("lower")), LT, constant(0)))
                    .blockReturn(constantFalse())
                    .ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("upper")), GT, constant(0)))
                    .blockReturn(constantFalse());
            if (!isLowIncluded) {
                block.ifCondition(exprDotMethod(ref("value"), "equals", ref("lower"))).blockReturn(constantFalse());
            }
            if (!isHighIncluded) {
                block.ifCondition(exprDotMethod(ref("value"), "equals", ref("upper"))).blockReturn(constantFalse());
            }
            CodegenMethod method = block.methodReturn(constantTrue());
            return localMethod(method, value, lower, higher);
        }
    }

    protected static class ExprBetweenCompDouble implements ExprBetweenComp {
        private boolean isLowIncluded;
        private boolean isHighIncluded;

        public ExprBetweenCompDouble(boolean lowIncluded, boolean highIncluded) {
            isLowIncluded = lowIncluded;
            isHighIncluded = highIncluded;
        }

        public boolean isBetween(Object value, Object lower, Object upper) {
            if ((value == null) || (lower == null) || ((upper == null))) {
                return false;
            }
            double valueD = ((Number) value).doubleValue();
            double lowerD = ((Number) lower).doubleValue();
            double upperD = ((Number) upper).doubleValue();

            if (lowerD > upperD) {
                double temp = upperD;
                upperD = lowerD;
                lowerD = temp;
            }

            if (valueD > lowerD) {
                if (valueD < upperD) {
                    return true;
                }
                if (isHighIncluded) {
                    return valueD == upperD;
                }
                return false;
            }
            if (isLowIncluded && (valueD == lowerD)) {
                return true;
            }
            return false;
        }

        public CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprBetweenCompDouble.class, codegenClassScope).addParam(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), "value").addParam(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), "lower").addParam(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), "upper").getBlock()
                    .ifCondition(relational(ref("lower"), GT, ref("upper")))
                    .declareVar(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), "temp", ref("upper"))
                    .assignRef("upper", ref("lower"))
                    .assignRef("lower", ref("temp"))
                    .blockEnd();
            CodegenBlock ifValueGTLower = block.ifCondition(relational(ref("value"), GT, ref("lower")));
            {
                ifValueGTLower.ifCondition(relational(ref("value"), LT, ref("upper"))).blockReturn(constantTrue());
                if (isHighIncluded) {
                    ifValueGTLower.blockReturn(equalsIdentity(ref("value"), ref("upper")));
                } else {
                    ifValueGTLower.blockReturn(constantFalse());
                }
            }
            CodegenMethod method;
            if (isLowIncluded) {
                method = block.methodReturn(equalsIdentity(ref("value"), ref("lower")));
            } else {
                method = block.methodReturn(constantFalse());
            }
            return localMethod(method, value, lower, higher);
        }
    }

    protected static class ExprBetweenCompLong implements ExprBetweenComp {
        private boolean isLowIncluded;
        private boolean isHighIncluded;

        public ExprBetweenCompLong(boolean lowIncluded, boolean highIncluded) {
            isLowIncluded = lowIncluded;
            isHighIncluded = highIncluded;
        }

        public boolean isBetween(Object value, Object lower, Object upper) {
            if ((value == null) || (lower == null) || ((upper == null))) {
                return false;
            }
            long valueD = ((Number) value).longValue();
            long lowerD = ((Number) lower).longValue();
            long upperD = ((Number) upper).longValue();

            if (lowerD > upperD) {
                long temp = upperD;
                upperD = lowerD;
                lowerD = temp;
            }

            if (valueD > lowerD) {
                if (valueD < upperD) {
                    return true;
                }
                if (isHighIncluded) {
                    return valueD == upperD;
                }
                return false;
            }
            if (isLowIncluded && (valueD == lowerD)) {
                return true;
            }
            return false;
        }

        public CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprBetweenCompLong.class, codegenClassScope).addParam(EPTypePremade.LONGPRIMITIVE.getEPType(), "value").addParam(EPTypePremade.LONGPRIMITIVE.getEPType(), "lower").addParam(EPTypePremade.LONGPRIMITIVE.getEPType(), "upper").getBlock()
                    .ifCondition(relational(ref("lower"), GT, ref("upper")))
                    .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "temp", ref("upper"))
                    .assignRef("upper", ref("lower"))
                    .assignRef("lower", ref("temp"))
                    .blockEnd();
            CodegenBlock ifValueGTLower = block.ifCondition(relational(ref("value"), GT, ref("lower")));
            {
                ifValueGTLower.ifCondition(relational(ref("value"), LT, ref("upper"))).blockReturn(constantTrue());
                if (isHighIncluded) {
                    ifValueGTLower.blockReturn(equalsIdentity(ref("value"), ref("upper")));
                } else {
                    ifValueGTLower.blockReturn(constantFalse());
                }
            }
            CodegenMethod method;
            if (isLowIncluded) {
                method = block.methodReturn(equalsIdentity(ref("value"), ref("lower")));
            } else {
                method = block.methodReturn(constantFalse());
            }
            return localMethod(method, value, lower, higher);
        }
    }

    protected static class ExprBetweenCompBigDecimal implements ExprBetweenComp {
        private boolean isLowIncluded;
        private boolean isHighIncluded;
        private SimpleNumberBigDecimalCoercer numberCoercerLower;
        private SimpleNumberBigDecimalCoercer numberCoercerUpper;
        private SimpleNumberBigDecimalCoercer numberCoercerValue;

        public ExprBetweenCompBigDecimal(boolean lowIncluded, boolean highIncluded, EPTypeClass valueType, EPTypeClass lowerType, EPTypeClass upperType) {
            isLowIncluded = lowIncluded;
            isHighIncluded = highIncluded;

            numberCoercerLower = SimpleNumberCoercerFactory.getCoercerBigDecimal(lowerType);
            numberCoercerUpper = SimpleNumberCoercerFactory.getCoercerBigDecimal(upperType);
            numberCoercerValue = SimpleNumberCoercerFactory.getCoercerBigDecimal(valueType);
        }

        public boolean isBetween(Object valueUncast, Object lowerUncast, Object upperUncast) {
            if ((valueUncast == null) || (lowerUncast == null) || ((upperUncast == null))) {
                return false;
            }
            BigDecimal value = numberCoercerValue.coerceBoxedBigDec((Number) valueUncast);
            BigDecimal lower = numberCoercerLower.coerceBoxedBigDec((Number) lowerUncast);
            BigDecimal upper = numberCoercerUpper.coerceBoxedBigDec((Number) upperUncast);

            if (lower.compareTo(upper) > 0) {
                BigDecimal temp = upper;
                upper = lower;
                lower = temp;
            }

            int valueComparedLower = value.compareTo(lower);
            if (valueComparedLower > 0) {
                int valueComparedUpper = value.compareTo(upper);
                if (valueComparedUpper < 0) {
                    return true;
                }
                return isHighIncluded && valueComparedUpper == 0;
            }
            if (isLowIncluded && valueComparedLower == 0) {
                return true;
            }
            return false;
        }

        public CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprBetweenCompBigDecimal.class, codegenClassScope).addParam(EPTypePremade.BIGDECIMAL.getEPType(), "value").addParam(EPTypePremade.BIGDECIMAL.getEPType(), "lower").addParam(EPTypePremade.BIGDECIMAL.getEPType(), "upper").getBlock()
                    .ifRefNullReturnFalse("value")
                    .ifRefNullReturnFalse("lower")
                    .ifRefNullReturnFalse("upper")
                    .ifCondition(relational(exprDotMethod(ref("lower"), "compareTo", ref("upper")), GT, constant(0)))
                    .declareVar(EPTypePremade.BIGDECIMAL.getEPType(), "temp", ref("upper"))
                    .assignRef("upper", ref("lower"))
                    .assignRef("lower", ref("temp"))
                    .blockEnd();
            CodegenBlock ifValueGTLower = block.ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("lower")), GT, constant(0)));
            {
                ifValueGTLower.ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("upper")), LT, constant(0))).blockReturn(constantTrue());
                if (isHighIncluded) {
                    ifValueGTLower.blockReturn(exprDotMethod(ref("value"), "equals", ref("upper")));
                } else {
                    ifValueGTLower.blockReturn(constantFalse());
                }
            }
            CodegenMethod method;
            if (isLowIncluded) {
                method = block.methodReturn(exprDotMethod(ref("value"), "equals", ref("lower")));
            } else {
                method = block.methodReturn(constantFalse());
            }
            CodegenExpression valueCoerced = numberCoercerValue.coerceBoxedBigDecCodegen(value, valueType);
            CodegenExpression lowerCoerced = numberCoercerValue.coerceBoxedBigDecCodegen(lower, lowerType);
            CodegenExpression higherCoerced = numberCoercerValue.coerceBoxedBigDecCodegen(higher, higherType);
            return localMethod(method, valueCoerced, lowerCoerced, higherCoerced);
        }
    }

    protected static class ExprBetweenCompBigInteger implements ExprBetweenComp {
        private boolean isLowIncluded;
        private boolean isHighIncluded;
        private SimpleNumberBigIntegerCoercer numberCoercerLower;
        private SimpleNumberBigIntegerCoercer numberCoercerUpper;
        private SimpleNumberBigIntegerCoercer numberCoercerValue;

        public ExprBetweenCompBigInteger(boolean lowIncluded, boolean highIncluded, EPTypeClass valueType, EPTypeClass lowerType, EPTypeClass upperType) {
            isLowIncluded = lowIncluded;
            isHighIncluded = highIncluded;

            numberCoercerLower = SimpleNumberCoercerFactory.getCoercerBigInteger(lowerType);
            numberCoercerUpper = SimpleNumberCoercerFactory.getCoercerBigInteger(upperType);
            numberCoercerValue = SimpleNumberCoercerFactory.getCoercerBigInteger(valueType);
        }

        public boolean isBetween(Object value, Object lower, Object upper) {
            if ((value == null) || (lower == null) || ((upper == null))) {
                return false;
            }
            BigInteger valueD = numberCoercerValue.coerceBoxedBigInt((Number) value);
            BigInteger lowerD = numberCoercerLower.coerceBoxedBigInt((Number) lower);
            BigInteger upperD = numberCoercerUpper.coerceBoxedBigInt((Number) upper);

            if (lowerD.compareTo(upperD) > 0) {
                BigInteger temp = upperD;
                upperD = lowerD;
                lowerD = temp;
            }

            if (valueD.compareTo(lowerD) > 0) {
                if (valueD.compareTo(upperD) < 0) {
                    return true;
                }
                if (isHighIncluded) {
                    return valueD.equals(upperD);
                }
                return false;
            }
            if (isLowIncluded && (valueD.equals(lowerD))) {
                return true;
            }
            return false;
        }

        public CodegenExpression codegenNoNullCheck(CodegenExpressionRef value, EPTypeClass valueType, CodegenExpressionRef lower, EPTypeClass lowerType, CodegenExpressionRef higher, EPTypeClass higherType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
            CodegenBlock block = codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprBetweenCompBigInteger.class, codegenClassScope).addParam(EPTypePremade.BIGINTEGER.getEPType(), "value").addParam(EPTypePremade.BIGINTEGER.getEPType(), "lower").addParam(EPTypePremade.BIGINTEGER.getEPType(), "upper").getBlock()
                    .ifRefNullReturnFalse("value")
                    .ifRefNullReturnFalse("lower")
                    .ifRefNullReturnFalse("upper")
                    .ifCondition(relational(exprDotMethod(ref("lower"), "compareTo", ref("upper")), GT, constant(0)))
                    .declareVar(EPTypePremade.BIGINTEGER.getEPType(), "temp", ref("upper"))
                    .assignRef("upper", ref("lower"))
                    .assignRef("lower", ref("temp"))
                    .blockEnd();
            CodegenBlock ifValueGTLower = block.ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("lower")), GT, constant(0)));
            {
                ifValueGTLower.ifCondition(relational(exprDotMethod(ref("value"), "compareTo", ref("upper")), LT, constant(0))).blockReturn(constantTrue());
                if (isHighIncluded) {
                    ifValueGTLower.blockReturn(exprDotMethod(ref("value"), "equals", ref("upper")));
                } else {
                    ifValueGTLower.blockReturn(constantFalse());
                }
            }
            CodegenMethod method;
            if (isLowIncluded) {
                method = block.methodReturn(exprDotMethod(ref("value"), "equals", ref("lower")));
            } else {
                method = block.methodReturn(constantFalse());
            }
            CodegenExpression valueCoerced = numberCoercerValue.coerceBoxedBigIntCodegen(value, valueType);
            CodegenExpression lowerCoerced = numberCoercerValue.coerceBoxedBigIntCodegen(lower, lowerType);
            CodegenExpression higherCoerced = numberCoercerValue.coerceBoxedBigIntCodegen(higher, higherType);
            return localMethod(method, valueCoerced, lowerCoerced, higherCoerced);
        }
    }
}
