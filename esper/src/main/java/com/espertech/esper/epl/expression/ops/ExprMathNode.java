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

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.math.BigDecimal;

/**
 * Represents a simple Math (+/-/divide/*) in a filter expression tree.
 */
public class ExprMathNode extends ExprNodeBase {
    private static final long serialVersionUID = 6479683588602862158L;

    private final MathArithTypeEnum mathArithTypeEnum;
    private final boolean isIntegerDivision;
    private final boolean isDivisionByZeroReturnsNull;

    private transient ExprMathNodeForge forge;

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
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Arithmatic node must have 2 parameters");
        }

        for (ExprNode child : this.getChildNodes()) {
            Class childType = child.getForge().getEvaluationType();
            if (!JavaClassHelper.isNumeric(childType)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childType.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        // Determine result type, set up compute function
        ExprNode lhs = this.getChildNodes()[0];
        ExprNode rhs = this.getChildNodes()[1];
        Class lhsType = lhs.getForge().getEvaluationType();
        Class rhsType = rhs.getForge().getEvaluationType();

        Class resultType;
        if ((lhsType == short.class || lhsType == Short.class) &&
                (rhsType == short.class || rhsType == Short.class)) {
            resultType = Integer.class;
        } else if ((lhsType == byte.class || lhsType == Byte.class) &&
                (rhsType == byte.class || rhsType == Byte.class)) {
            resultType = Integer.class;
        } else if (lhsType.equals(rhsType)) {
            resultType = JavaClassHelper.getBoxedType(rhsType);
        } else {
            resultType = JavaClassHelper.getArithmaticCoercionType(lhsType, rhsType);
        }

        if ((mathArithTypeEnum == MathArithTypeEnum.DIVIDE) && (!isIntegerDivision)) {
            if (resultType != BigDecimal.class) {
                resultType = Double.class;
            }
        }

        MathArithTypeEnum.Computer arithTypeEnumComputer = mathArithTypeEnum.getComputer(resultType, lhsType, rhsType, isIntegerDivision, isDivisionByZeroReturnsNull, validationContext.getEngineImportService().getDefaultMathContext());
        forge = new ExprMathNodeForge(this, arithTypeEnumComputer, resultType);
        return null;
    }

    public boolean isConstantResult() {
        return false;
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

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
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
