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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.math.BigDecimal;

/**
 * Represents a simple Math (+/-/divide/*) in a filter expression tree.
 */
public class ExprMathNode extends ExprNodeBase implements ExprEvaluator {
    private final MathArithTypeEnum mathArithTypeEnum;
    private final boolean isIntegerDivision;
    private final boolean isDivisionByZeroReturnsNull;

    private transient MathArithTypeEnum.Computer arithTypeEnumComputer;
    private Class resultType;
    private transient ExprEvaluator evaluatorLeft;
    private transient ExprEvaluator evaluatorRight;
    private static final long serialVersionUID = 6479683588602862158L;

    /**
     * Ctor.
     *
     * @param mathArithTypeEnum           - type of math
     * @param isIntegerDivision           - false for division returns double, true for using Java-standard integer division
     * @param isDivisionByZeroReturnsNull - false for division-by-zero returns infinity, true for null
     */
    public ExprMathNode(MathArithTypeEnum mathArithTypeEnum, boolean isIntegerDivision, boolean isDivisionByZeroReturnsNull) {
        this.mathArithTypeEnum = mathArithTypeEnum;
        this.isIntegerDivision = isIntegerDivision;
        this.isDivisionByZeroReturnsNull = isDivisionByZeroReturnsNull;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Arithmatic node must have 2 parameters");
        }

        for (ExprNode child : this.getChildNodes()) {
            Class childType = child.getExprEvaluator().getType();
            if (!JavaClassHelper.isNumeric(childType)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childType.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        // Determine result type, set up compute function
        evaluatorLeft = this.getChildNodes()[0].getExprEvaluator();
        evaluatorRight = this.getChildNodes()[1].getExprEvaluator();

        Class childTypeOne = evaluatorLeft.getType();
        Class childTypeTwo = evaluatorRight.getType();

        if ((childTypeOne == short.class || childTypeOne == Short.class) &&
                (childTypeTwo == short.class || childTypeTwo == Short.class)) {
            resultType = Integer.class;
        } else if ((childTypeOne == byte.class || childTypeOne == Byte.class) &&
                (childTypeTwo == byte.class || childTypeTwo == Byte.class)) {
            resultType = Integer.class;
        } else if (childTypeOne.equals(childTypeTwo)) {
            resultType = JavaClassHelper.getBoxedType(childTypeTwo);
        } else {
            resultType = JavaClassHelper.getArithmaticCoercionType(childTypeOne, childTypeTwo);
        }

        if ((mathArithTypeEnum == MathArithTypeEnum.DIVIDE) && (!isIntegerDivision)) {
            if (resultType != BigDecimal.class) {
                resultType = Double.class;
            }
        }

        arithTypeEnumComputer = mathArithTypeEnum.getComputer(resultType, childTypeOne, childTypeTwo, isIntegerDivision, isDivisionByZeroReturnsNull, validationContext.getEngineImportService().getDefaultMathContext());
        return null;
    }

    public Class getType() {
        return resultType;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprMath(this, mathArithTypeEnum.getExpressionText());
        }
        Object valueChildOne = evaluatorLeft.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueChildOne == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprMath(null);
            }
            return null;
        }

        Object valueChildTwo = evaluatorRight.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueChildTwo == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprMath(null);
            }
            return null;
        }

        // arithTypeEnumComputer is initialized by validation
        if (InstrumentationHelper.ENABLED) {
            Object result = arithTypeEnumComputer.compute((Number) valueChildOne, (Number) valueChildTwo);
            InstrumentationHelper.get().aExprMath(result);
            return result;
        }

        return arithTypeEnumComputer.compute((Number) valueChildOne, (Number) valueChildTwo);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        writer.append(mathArithTypeEnum.getExpressionText());
        this.getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        if (mathArithTypeEnum == MathArithTypeEnum.MULTIPLY ||
                mathArithTypeEnum == MathArithTypeEnum.DIVIDE ||
                mathArithTypeEnum == MathArithTypeEnum.MODULO) {
            return ExprPrecedenceEnum.MULTIPLY;
        } else {
            return ExprPrecedenceEnum.ADDITIVE;
        }
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprMathNode)) {
            return false;
        }

        ExprMathNode other = (ExprMathNode) node;

        if (other.mathArithTypeEnum != this.mathArithTypeEnum) {
            return false;
        }

        return true;
    }

    /**
     * Returns the type of math.
     *
     * @return math type
     */
    public MathArithTypeEnum getMathArithTypeEnum() {
        return mathArithTypeEnum;
    }
}
